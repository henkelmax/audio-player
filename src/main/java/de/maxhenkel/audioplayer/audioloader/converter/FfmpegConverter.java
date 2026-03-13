package de.maxhenkel.audioplayer.audioloader.converter;

import de.maxhenkel.audioplayer.AudioPlayerMod;

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

    public static byte[] convertToMonoMp3(InputStream sourceAudio) throws Exception {
        if (getFfmpegVersion() == null) {
            throw new IOException("FFMPEG not found");
        }

        ProcessBuilder pb = new ProcessBuilder(
                getFfmpegPath(),
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

}
