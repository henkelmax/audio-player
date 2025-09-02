package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;

import javax.annotation.Nullable;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioUtils {

    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    @Nullable
    public static AudioType getAudioType(Path path) throws IOException {
        if (isWav(Files.newInputStream(path))) {
            return AudioType.WAV;
        }
        if (isMp3(Files.newInputStream(path))) {
            return AudioType.MP3;
        }
        return null;
    }

    @Nullable
    public static AudioType getAudioType(byte[] data) throws IOException {
        if (isWav(new ByteArrayInputStream(data))) {
            return AudioType.WAV;
        }
        if (isMp3(new ByteArrayInputStream(data))) {
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

    public static boolean isMp3(InputStream inputStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(bis);
            return fileFormat.getType().toString().equalsIgnoreCase("mp3");
        } catch (UnsupportedAudioFileException e) {
            return false;
        }
    }

    public static short[] convert(Path file, float volume) throws IOException, UnsupportedAudioFileException {
        return convert(file, getAudioType(file), volume);
    }

    public static short[] convert(Path file, AudioType audioType, float volume) throws IOException, UnsupportedAudioFileException {
        if (audioType == AudioType.WAV) {
            return convertWav(file, volume);
        } else if (audioType == AudioType.MP3) {
            return convertMp3(file, volume);
        }
        throw new UnsupportedAudioFileException("Unsupported audio type");
    }

    public static short[] convertWav(Path file, float volume) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile())) {
            return convert(source, volume);
        }
    }

    private static short[] convert(AudioInputStream source, float volume) throws IOException {
        AudioFormat sourceFormat = source.getFormat();
        AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
        AudioInputStream stream1 = AudioSystem.getAudioInputStream(convertFormat, source);
        AudioInputStream stream2 = AudioSystem.getAudioInputStream(FORMAT, stream1);
        return VoicechatAudioPlayerPlugin.voicechatApi.getAudioConverter().bytesToShorts(adjustVolume(stream2.readAllBytes(), volume));
    }

    private static byte[] adjustVolume(byte[] audioSamples, float volume) {
        for (int i = 0; i < audioSamples.length; i += 2) {
            short buf1 = audioSamples[i + 1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xFF) << 8);
            buf2 = (short) (buf2 & 0xFF);

            short res = (short) (buf1 | buf2);
            res = (short) (res * volume);

            audioSamples[i] = (byte) res;
            audioSamples[i + 1] = (byte) (res >> 8);

        }
        return audioSamples;
    }

    public static short[] convertMp3(Path file, float volume) throws IOException, UnsupportedAudioFileException {
        try {
            Mp3Decoder mp3Decoder = VoicechatAudioPlayerPlugin.voicechatApi.createMp3Decoder(Files.newInputStream(file));
            if (mp3Decoder == null) {
                throw new IOException("Error creating mp3 decoder");
            }
            byte[] data = VoicechatAudioPlayerPlugin.voicechatApi.getAudioConverter().shortsToBytes(mp3Decoder.decode());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            AudioFormat audioFormat = mp3Decoder.getAudioFormat();
            AudioInputStream source = new AudioInputStream(byteArrayInputStream, audioFormat, data.length / audioFormat.getFrameSize());
            return convert(source, volume);
        } catch (Exception e) {
            AudioPlayer.LOGGER.warn("Error converting mp3 file with native decoder");
            return convert(AudioSystem.getAudioInputStream(file.toFile()), volume);
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
