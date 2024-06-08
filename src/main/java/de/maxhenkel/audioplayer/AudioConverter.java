package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioConverter {

    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    @Nullable
    public static AudioType getAudioType(Path path) throws IOException {
        if (isWav(Files.newInputStream(path))) {
            return AudioType.WAV;
        }
        if (isMp3File(Files.newInputStream(path))) {
            return AudioType.MP3;
        }
        return null;
    }

    @Nullable
    public static AudioType getAudioType(byte[] data) throws IOException {
        if (isWav(new ByteArrayInputStream(data))) {
            return AudioType.WAV;
        }
        if (isMp3File(new ByteArrayInputStream(data))) {
            return AudioType.MP3;
        }
        return null;
    }

    public static boolean isWav(InputStream inputStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(bis);
            return fileFormat.getType().toString().equalsIgnoreCase("wave");
        } catch (UnsupportedAudioFileException e) {
            return false;
        }
    }

    public static boolean isMp3File(InputStream inputStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(bis);
            return fileFormat.getType().toString().equalsIgnoreCase("mp3");
        } catch (UnsupportedAudioFileException e) {
            return false;
        }
    }

    public static short[] convert(Path file) throws IOException, UnsupportedAudioFileException {
        return convert(file, getAudioType(file));
    }

    public static short[] convert(Path file, AudioType audioType) throws IOException, UnsupportedAudioFileException {
        if (audioType == AudioType.WAV) {
            return convertWav(file);
        } else if (audioType == AudioType.MP3) {
            return convertMp3(file);
        }
        throw new UnsupportedAudioFileException("Unsupported audio type");
    }

    public static short[] convertWav(Path file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
            return convert(source);
        }
    }

    private static short[] convert(AudioInputStream source) throws IOException {
        AudioFormat sourceFormat = source.getFormat();
        AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
        AudioInputStream stream1 = AudioSystem.getAudioInputStream(convertFormat, source);
        AudioInputStream stream2 = AudioSystem.getAudioInputStream(FORMAT, stream1);
        return Plugin.voicechatApi.getAudioConverter().bytesToShorts(stream2.readAllBytes());
    }

    public static short[] convertMp3(Path file) throws IOException, UnsupportedAudioFileException {
        try {
            Mp3Decoder mp3Decoder = Plugin.voicechatApi.createMp3Decoder(Files.newInputStream(file));
            if (mp3Decoder == null) {
                throw new IOException("Error creating mp3 decoder");
            }
            byte[] data = Plugin.voicechatApi.getAudioConverter().shortsToBytes(mp3Decoder.decode());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            AudioFormat audioFormat = mp3Decoder.getAudioFormat();
            AudioInputStream source = new AudioInputStream(byteArrayInputStream, audioFormat, data.length / audioFormat.getFrameSize());
            return convert(source);
        } catch (Exception e) {
            AudioPlayer.LOGGER.warn("Error converting mp3 file with native decoder");
            return convert(AudioSystem.getAudioInputStream(file.toFile()));
        }
    }

    public enum AudioType {
        MP3("mp3"),
        WAV("wav");

        private final String extension;

        AudioType(String fileName) {
            this.extension = fileName;
        }

        public boolean isValidFileName(Path path) {
            return path.toString().toLowerCase().endsWith(".%s".formatted(extension));
        }

        public String getExtension() {
            return extension;
        }
    }

}
