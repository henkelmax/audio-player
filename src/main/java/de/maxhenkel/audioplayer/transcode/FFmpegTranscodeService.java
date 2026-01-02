package de.maxhenkel.audioplayer.transcode;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
            if (contentType == null) {
                return false; // Assume standard if unknown, or let download fail later
            }

            // Check for standard supported types (MP3, WAV)
            // Note: Content-Types can vary (audio/mpeg, audio/mqv, audio/x-wav, etc.)
            // We return FALSE if it IS a supported type.

            if (contentType.toLowerCase().contains("audio/mpeg") ||
                    contentType.toLowerCase().contains("audio/mp3") ||
                    contentType.toLowerCase().contains("audio/wav") ||
                    contentType.toLowerCase().contains("audio/x-wav")) {
                return false;
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
        // Since we are blocking the download thread, we can't easily send feedback to
        // the specific player
        // without passing the player object down, but the requirement was generic chat
        // feedback or just handling the queue.
        // We warn if the queue is getting large.
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

        // Redirect stderr to logger or ignore to prevent buffer filling?
        // Ideally inheritIO or consume it. ProcessBuilder needs logic to consume
        // streams.
        // For simplicity and safety against deadlocks, we redirect Error to Output and
        // ignore,
        // or discard both if we don't care about logs.
        // But FFmpeg writes stats to stderr.
        // Let's redirect stderr to null or a gobbler if we can, but Java 9+ has
        // redirectOutput(ProcessBuilder.Redirect.DISCARD).
        // Since we are on Java 21:
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();

        // Watchdog for timeout? Requirement said "Ensure process is destroyed if ...
        // takes too long"
        // We can use waitFor with timeout.
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Transcoding timed out");
        }

        if (process.exitValue() != 0) {
            throw new IOException("FFmpeg exited with error code: " + process.exitValue());
        }

        if (java.nio.file.Files.size(destination) == 0) {
            throw new IOException("FFmpeg produced empty file");
        }
    }
}
