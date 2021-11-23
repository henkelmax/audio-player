package de.maxhenkel.audioplayer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.sound.sampled.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class AudioManager {

    public static LevelResource AUDIO_DATA = new LevelResource("audio_player_data");
    public static AudioFormat FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    public static short[] getSound(MinecraftServer server, UUID id) throws IOException, UnsupportedAudioFileException {
        Path soundFile = getSoundFile(server, id);

        if (!Files.exists(soundFile)) {
            throw new FileNotFoundException("Sound does not exist");
        }

        return readSound(soundFile);
    }

    public static short[] readSound(Path file) throws IOException, UnsupportedAudioFileException {
        AudioInputStream in = AudioSystem.getAudioInputStream(file.toFile());
        AudioInputStream convertedIn = AudioSystem.getAudioInputStream(FORMAT, in);
        return bytesToShorts(convertedIn.readAllBytes());
    }

    public static Path getSoundFile(MinecraftServer server, UUID id) {
        return server.getWorldPath(AUDIO_DATA).resolve(id.toString() + ".wav");
    }

    public static void saveSound(MinecraftServer server, UUID id, String url) throws UnsupportedAudioFileException, IOException {
        AudioInputStream in = AudioSystem.getAudioInputStream(new URL(url));
        Path soundFile = getSoundFile(server, id);
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }

        Files.createDirectories(soundFile.getParent());
        AudioSystem.write(in, AudioFileFormat.Type.WAVE, Files.newOutputStream(soundFile, StandardOpenOption.CREATE_NEW));
    }

    //TODO move to API
    public static short[] bytesToShorts(byte[] bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("Input bytes need to be divisible by 2");
        }
        short[] data = new short[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2) {
            data[i / 2] = bytesToShort(bytes[i], bytes[i + 1]);
        }
        return data;
    }

    public static short bytesToShort(byte b1, byte b2) {
        return (short) (((b2 & 0xFF) << 8) | (b1 & 0xFF));
    }

}
