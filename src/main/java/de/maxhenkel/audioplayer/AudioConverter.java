package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioConverter {

    private static final byte[][] MP3_MAGIC_BYTES = new byte[][]{
            {(byte) 0xFF, (byte) 0xFB},
            {(byte) 0xFF, (byte) 0xF3},
            {(byte) 0xFF, (byte) 0xF2},
            {(byte) 0x49, (byte) 0x44, (byte) 0x33}
    };

    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    @Nullable
    public static AudioType getAudioType(byte[] data) throws UnsupportedAudioFileException, IOException {
        if (hasMp3MagicBytes(data)) {
            return AudioType.MP3;
        }
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
        if (isWav(audioInputStream.getFormat())) {
            return AudioType.WAV;
        }
        return null;
    }

    @Nullable
    public static AudioType getAudioType(Path path) throws UnsupportedAudioFileException, IOException {
        if (AudioType.WAV.isValidFileName(path)) {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile())) {
                if (isWav(ais.getFormat())) {
                    return AudioType.WAV;
                }
            }
        } else if (AudioType.MP3.isValidFileName(path)) {
            if (isMp3File(path)) {
                return AudioType.MP3;
            }
        }
        return null;
    }

    public static boolean isWav(AudioFormat audioFormat) {
        AudioFormat.Encoding encoding = audioFormat.getEncoding();
        return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) ||
                encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED) ||
                encoding.equals(AudioFormat.Encoding.PCM_FLOAT) ||
                encoding.equals(AudioFormat.Encoding.ALAW) ||
                encoding.equals(AudioFormat.Encoding.ULAW);
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

    public static boolean isMp3File(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return hasMp3MagicBytes(is.readNBytes(3));
        }
    }

    private static boolean hasMp3MagicBytes(byte[] data) {
        for (byte[] magicBytes : MP3_MAGIC_BYTES) {
            if (data.length < magicBytes.length) {
                return false;
            }
            boolean valid = true;
            for (int i = 0; i < magicBytes.length; i++) {
                if (data[i] != magicBytes[i]) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return true;
            }
        }
        return false;
    }

}
