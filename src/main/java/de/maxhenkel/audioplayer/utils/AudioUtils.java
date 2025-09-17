package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.mp3.Mp3Decoder;
import net.fabricmc.loader.api.FabricLoader;

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

    public static float getLengthSeconds(short[] audio) {
        return getLengthSeconds(audio.length);
    }

    public static float getLengthSeconds(int sampleLength) {
        return (float) sampleLength / AudioUtils.FORMAT.getSampleRate();
    }

    public static float getLengthSeconds(byte[] file) throws UnsupportedAudioFileException, IOException {
        return getLengthSeconds(convert(file));
    }

    public static void adjustVolume(short[] samples, float volume) {
        float gain = (float) Math.pow(10, (volume - 1F));
        for (int i = 0; i < samples.length; i++) {
            int out = Math.round(samples[i] * gain);
            samples[i] = (short) Math.max(Math.min(out, Short.MAX_VALUE), Short.MIN_VALUE);
        }
    }

    public static short[] convert(Path file) throws IOException, UnsupportedAudioFileException {
        return convert(() -> new BufferedInputStream(Files.newInputStream(file)), getAudioType(file));
    }

    public static short[] convert(byte[] file) throws IOException, UnsupportedAudioFileException {
        return convert(() -> new ByteArrayInputStream(file), getAudioType(file));
    }

    public static short[] convert(Path file, @Nullable Float volume) throws IOException, UnsupportedAudioFileException {
        short[] converted = convert(file);
        if (volume != null) {
            adjustVolume(converted, volume);
        }
        return converted;
    }

    private static short[] convert(InputStreamSupplier inputStreamSupplier, AudioType audioType) throws IOException, UnsupportedAudioFileException {
        if (audioType == AudioType.WAV) {
            return convertWav(inputStreamSupplier);
        } else if (audioType == AudioType.MP3) {
            return convertMp3(inputStreamSupplier);
        }
        throw new UnsupportedAudioFileException("Unsupported audio type");
    }

    private static short[] convertWav(InputStreamSupplier inputStreamSupplier) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(inputStreamSupplier.get())) {
            return convert(source);
        }
    }

    private static short[] convertMp3(InputStreamSupplier inputStreamSupplier) throws IOException, UnsupportedAudioFileException {
        try {
            Mp3Decoder mp3Decoder = VoicechatAudioPlayerPlugin.voicechatApi.createMp3Decoder(inputStreamSupplier.get());
            if (mp3Decoder == null) {
                throw new IOException("Error creating mp3 decoder");
            }
            byte[] data = VoicechatAudioPlayerPlugin.voicechatApi.getAudioConverter().shortsToBytes(mp3Decoder.decode());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            AudioFormat audioFormat = mp3Decoder.getAudioFormat();
            AudioInputStream source = new AudioInputStream(byteArrayInputStream, audioFormat, data.length / audioFormat.getFrameSize());
            return convert(source);
        } catch (Exception e) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                AudioPlayerMod.LOGGER.error("Could not convert mp3 using native decoder, using fallback", e);
            } else {
                AudioPlayerMod.LOGGER.debug("Could not convert mp3 using native decoder, using fallback");
            }
            return convert(AudioSystem.getAudioInputStream(inputStreamSupplier.get()));
        }
    }

    private static short[] convert(AudioInputStream source) throws IOException {
        AudioFormat sourceFormat = source.getFormat();
        AudioFormat convertFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
        AudioInputStream stream1 = AudioSystem.getAudioInputStream(convertFormat, source);
        AudioInputStream stream2 = AudioSystem.getAudioInputStream(FORMAT, stream1);
        return VoicechatAudioPlayerPlugin.voicechatApi.getAudioConverter().bytesToShorts(stream2.readAllBytes());
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

    private static interface InputStreamSupplier {
        InputStream get() throws IOException;
    }

}
