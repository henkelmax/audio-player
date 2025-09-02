package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

public class AudioStorageManager {

    public static LevelResource AUDIO_DATA_LEVEL_RESOURCE = new LevelResource("audio_player_data");

    public static AudioStorageManager instance;

    private final MinecraftServer server;

    public AudioStorageManager(MinecraftServer server) {
        this.server = server;
    }

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(AudioStorageManager::onServerStarted);
        try {
            Files.createDirectories(AudioStorageManager.getUploadFolder());
        } catch (IOException e) {
            AudioPlayer.LOGGER.warn("Failed to create upload folder", e);
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        instance = new AudioStorageManager(server);
    }

    public static AudioStorageManager instance() {
        return instance;
    }

    public Path getSoundFile(UUID id, String extension) {
        return getAudioDataFolder().resolve(id.toString() + "." + extension);
    }

    public Path getAudioDataFolder() {
        return server.getWorldPath(AUDIO_DATA_LEVEL_RESOURCE);
    }

    public short[] getSound(UUID id) throws Exception {
        float volume;
        if (VolumeOverrideManager.instance().isPresent()) {
            volume = VolumeOverrideManager.instance().get().getAudioVolume(id);
        } else {
            volume = 1F;
        }
        return AudioPlayer.AUDIO_CACHE.get(id, () -> AudioConverter.convert(getExistingSoundFile(id), volume));
    }

    public Path getExistingSoundFile(UUID id) throws FileNotFoundException {
        Path file = getSoundFile(id, AudioConverter.AudioType.MP3.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        file = getSoundFile(id, AudioConverter.AudioType.WAV.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        throw new FileNotFoundException("Audio does not exist");
    }

    public boolean checkSoundExists(UUID id) {
        Path file = getSoundFile(id, AudioConverter.AudioType.MP3.getExtension());
        if (Files.exists(file)) {
            return true;
        }
        file = getSoundFile(id, AudioConverter.AudioType.WAV.getExtension());
        return Files.exists(file);
    }

    public static Path getUploadFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("audioplayer_uploads");
    }

    public void saveSound(UUID id, String url) throws UnsupportedAudioFileException, IOException {
        byte[] data = download(new URL(url), AudioPlayer.SERVER_CONFIG.maxUploadSize.get());
        saveSound(id, FileNameManager.getFileNameFromUrl(url), data);
    }

    public void saveSound(UUID id, String fileName, byte[] data) throws UnsupportedAudioFileException, IOException {
        AudioConverter.AudioType audioType = AudioConverter.getAudioType(data);
        checkExtensionAllowed(audioType);

        Path soundFile = getSoundFile(id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        try (OutputStream outputStream = Files.newOutputStream(soundFile)) {
            IOUtils.write(data, outputStream);
        }

        FileNameManager.instance().ifPresent(mgr -> mgr.addFileName(id, fileName));
    }

    public void saveSound(UUID id, Path file) throws UnsupportedAudioFileException, IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchFileException("The file %s does not exist".formatted(file.toString()));
        }

        long size = Files.size(file);
        if (size > AudioPlayer.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayer.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
        }

        AudioConverter.AudioType audioType = AudioConverter.getAudioType(file);
        checkExtensionAllowed(audioType);

        Path soundFile = getSoundFile(id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        Files.move(file, soundFile);
        FileNameManager.instance().ifPresent(mgr -> mgr.addFileName(id, FileNameManager.getFileNameFromPath(file)));
    }

    public static void checkExtensionAllowed(@Nullable AudioConverter.AudioType audioType) throws UnsupportedAudioFileException {
        if (audioType == null) {
            throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        if (audioType.equals(AudioConverter.AudioType.MP3)) {
            if (!AudioPlayer.SERVER_CONFIG.allowMp3Upload.get()) {
                throw new UnsupportedAudioFileException("Uploading mp3 files is not allowed on this server");
            }
        }
        if (audioType.equals(AudioConverter.AudioType.WAV)) {
            if (!AudioPlayer.SERVER_CONFIG.allowWavUpload.get()) {
                throw new UnsupportedAudioFileException("Uploading wav files is not allowed on this server");
            }
        }
    }

    private static byte[] download(URL url, long limit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", Filebin.USER_AGENT);
        connection.connect();

        BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());

        int nRead;
        byte[] data = new byte[32768];

        while ((nRead = bis.read(data, 0, data.length)) != -1) {
            bos.write(data, 0, nRead);
            if (bos.size() > limit) {
                bis.close();
                throw new IOException("Maximum file size of %sMB exceeded".formatted((int) (((float) limit) / 1_000_000F)));
            }
        }
        bis.close();
        return bos.toByteArray();
    }

}
