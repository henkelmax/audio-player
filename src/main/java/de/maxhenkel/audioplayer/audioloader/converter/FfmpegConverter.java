package de.maxhenkel.audioplayer.audioloader.converter;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.utils.AudioUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

public class FfmpegConverter {

    private static String ffmpegVersion;
    private static boolean ffmpegChecked;

    @Nullable
    public static String getFfmpegVersion() {
        if (ffmpegChecked) {
            return ffmpegVersion;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(getFfmpegPath(), "-version");
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                int exitCode = process.waitFor();
                if (exitCode != 0 || firstLine == null) {
                    return null;
                }
                String[] parts = firstLine.split(" ");
                if (parts.length < 3 || !parts[0].equalsIgnoreCase("ffmpeg") || !parts[1].equalsIgnoreCase("version")) {
                    return null;
                }
                ffmpegVersion = parts[2];
                ffmpegChecked = true;
                return ffmpegVersion;
            }
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.warn("Failed to get ffmpeg version", e);
            return null;
        }
    }

    public static ConvertedAudio convert(@Nullable String fileName, byte[] sourceAudio) throws FfmpegException {
        return convert(fileName, new ByteArrayInputStream(sourceAudio));
    }

    public static ConvertedAudio convert(@Nullable String fileName, InputStream sourceAudio) throws FfmpegException {
        try {
            return new ConvertedAudio(updateFilename(fileName), convertToMonoMp3(sourceAudio), AudioUtils.AudioType.MP3);
        } catch (Exception e) {
            throw new FfmpegException("Failed to convert audio to mp3", e);
        }
    }

    @Nullable
    private static String updateFilename(@Nullable String fileName) {
        if (fileName == null) {
            return null;
        }
        int idx = fileName.indexOf(".");
        if (idx == -1) {
            return "%s.mp3".formatted(fileName);
        }
        return "%s.mp3".formatted(fileName.substring(0, idx));
    }

    private static byte[] convertToMonoMp3(InputStream sourceAudio) throws Exception {
        if (getFfmpegVersion() == null) {
            throw new IOException("FFmpeg not found");
        }

        // Use "-xerror" and "-abort_on empty_output" because converting an mp4 file
        // ffmpeg will fail to seek due to piping stdin and just output an id3 tag and then exit with exit code 0
        ProcessBuilder pb = new ProcessBuilder(
                getFfmpegPath(),
                "-xerror",
                "-abort_on", "empty_output",
                "-i", "pipe:0",
                "-ac", "1",
                "-f", "mp3",
                "pipe:1"
        );

        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();

        AtomicReference<Exception> writeError = new AtomicReference<>();

        Thread writerThread = new Thread(() -> {
            try (OutputStream ffmpegStdin = process.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = sourceAudio.read(buffer)) != -1) {
                    ffmpegStdin.write(buffer, 0, bytesRead);
                }
                ffmpegStdin.flush();
            } catch (Exception e) {
                writeError.set(e);
            }
        });
        writerThread.setDaemon(true);
        writerThread.setName("AudioPlayer-Ffmpeg-Writer");
        writerThread.start();

        ByteArrayOutputStream mp3Output = new ByteArrayOutputStream();
        try (InputStream ffmpegStdout = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ffmpegStdout.read(buffer)) != -1) {
                mp3Output.write(buffer, 0, bytesRead);
            }
        }

        writerThread.join();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code %s".formatted(exitCode));
        }
        if (writeError.get() != null) {
            throw new RuntimeException("Failed to send audio to FFmpeg");
        }

        return mp3Output.toByteArray();
    }

    private static String getFfmpegPath() {
        String path = AudioPlayerMod.SERVER_CONFIG.ffmpegPath.get();
        if (path.isBlank()) {
            return "ffmpeg";
        }
        return path;
    }

    public static record ConvertedAudio(@Nullable String fileName, byte[] data, AudioUtils.AudioType audioType) {
    }

    public static class FfmpegException extends Exception {
        public FfmpegException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
