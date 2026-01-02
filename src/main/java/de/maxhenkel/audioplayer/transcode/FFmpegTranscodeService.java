package de.maxhenkel.audioplayer.transcode;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FFmpegTranscodeService {

    private static FFmpegTranscodeService INSTANCE;

    @Nullable
    private final String ffmpegPath;
    private final Semaphore queueSemaphore;
    private final AtomicInteger queueSize;

    private FFmpegTranscodeService() {
        this.ffmpegPath = findFFmpeg();
        int maxConcurrent = AudioPlayerMod.SERVER_CONFIG.maxConcurrentTranscodes.get();
        this.queueSemaphore = new Semaphore(maxConcurrent, true);
        this.queueSize = new AtomicInteger(0);

        if (this.ffmpegPath == null) {
            AudioPlayerMod.LOGGER.warn("FFmpeg not found. Transcoding will be disabled.");
        } else {
            AudioPlayerMod.LOGGER.info("FFmpeg found at: {}", this.ffmpegPath);
        }
    }

    public static synchronized FFmpegTranscodeService instance() {
        if (INSTANCE == null) {
            INSTANCE = new FFmpegTranscodeService();
        }
        return INSTANCE;
    }

    @Nullable
    private String findFFmpeg() {
        // 1. Check config
        String configPath = AudioPlayerMod.SERVER_CONFIG.ffmpegPath.get();
        if (configPath != null && !configPath.isBlank()) {
            if (testFFmpeg(configPath)) {
                return configPath;
            }
            AudioPlayerMod.LOGGER.warn("Configured FFmpeg path not valid: {}", configPath);
        }

        // 2. Check system path
        if (testFFmpeg("ffmpeg")) {
            return "ffmpeg";
        }

        return null;
    }

    private boolean testFFmpeg(String path) {
        try {
            Process process = new ProcessBuilder(path, "-version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return true;
            }
            process.destroy();
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    public boolean isAvailable() {
        return ffmpegPath != null;
    }

    public boolean needsTranscoding(String urlString) {
        if (!isAvailable()) {
            return false;
        }
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            String contentType = connection.getContentType();

            // Refined Check: Handle null or generic content types by checking extension
            if (contentType == null || contentType.equalsIgnoreCase("application/octet-stream")) {
                String path = url.getPath().toLowerCase();
                if (path.endsWith(".mp3") || path.endsWith(".wav")) {
                    return false; // Trust extension if mime is ambiguous
                }
                // If extension is also unknown or other, assume transcode needed
            } else {
                // Strict mime check
                if (contentType.toLowerCase().contains("audio/mpeg") ||
                        contentType.toLowerCase().contains("audio/mp3") ||
                        contentType.toLowerCase().contains("audio/wav") ||
                        contentType.toLowerCase().contains("audio/x-wav")) {
                    return false;
                }
            }

            return true; // Transcode everything else
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.warn("Failed to check head for URL: {}", urlString, e);
            return false;
        }
    }

    public void process(String url, Path destination) throws IOException, InterruptedException {
        if (!isAvailable()) {
            throw new IOException("FFmpeg not available");
        }

        int myOptions = queueSize.incrementAndGet();
        if (myOptions > AudioPlayerMod.SERVER_CONFIG.maxConcurrentTranscodes.get()) {
            AudioPlayerMod.LOGGER.info("Transcode queue active. Position: {}", myOptions);
        }

        queueSemaphore.acquire();
        try {
            queueSize.decrementAndGet();
            runFFmpeg(url, destination);
        } finally {
            queueSemaphore.release();
        }
    }

    private void runFFmpeg(String url, Path destination) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-y", // Force overwrite
                "-i", url,
                "-vn", // No video
                "-ar", "44100", // Sample rate
                "-ac", "2", // Channels
                "-b:a", "128k", // Bitrate
                "-f", "mp3", // Format
                destination.toAbsolutePath().toString());

        // Merge stderr into stdout so we can capture it
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Capture output in a separate thread to prevent buffer blocking
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
        outputGobbler.start();

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Transcoding timed out");
        }

        if (process.exitValue() != 0) {
            // Log the captured output for debugging
            AudioPlayerMod.LOGGER.error("FFmpeg failed with exit code {}. Output:\n{}", process.exitValue(),
                    outputGobbler.getOutput());
            throw new IOException("FFmpeg exited with error code: " + process.exitValue());
        }

        if (java.nio.file.Files.size(destination) == 0) {
            AudioPlayerMod.LOGGER.error("FFmpeg produced empty file. Output:\n{}", outputGobbler.getOutput());
            throw new IOException("FFmpeg produced empty file");
        }
    }

    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        public StreamGobbler(InputStream inputStream) {
            this.inputStream = inputStream;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Limit log size to prevent memory issues if ffmpeg goes crazy
                    if (output.length() < 10000) {
                        output.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        }

        public String getOutput() {
            return output.toString();
        }
    }
}
