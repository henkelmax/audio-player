package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.audioloader.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.audioloader.importer.AudioImporter;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.utils.ComponentException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStorageManager {

    public static LevelResource AUDIO_DATA_LEVEL_RESOURCE = new LevelResource("audio_player_data");

    public static AudioStorageManager instance;

    private final MinecraftServer server;
    private final ExecutorService executor;
    private final VolumeOverrideManager volumeOverrideManager;

    public AudioStorageManager(MinecraftServer server) {
        this.server = server;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerStorageManagerExecutor");
            thread.setDaemon(true);
            return thread;
        });
        volumeOverrideManager = new VolumeOverrideManager(getAudioDataFolder().resolve("volume-overrides.json"));
    }

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(AudioStorageManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(AudioStorageManager::onServerStopped);
        try {
            Files.createDirectories(AudioStorageManager.getUploadFolder());
        } catch (IOException e) {
            AudioPlayer.LOGGER.warn("Failed to create upload folder", e);
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        instance = new AudioStorageManager(server);
    }

    private static void onServerStopped(MinecraftServer server) {
        if (instance != null) {
            instance.close();
        }
        instance = null;
    }

    private void close() {
        executor.shutdown();
    }

    public static AudioStorageManager instance() {
        return instance;
    }

    public static VolumeOverrideManager volumeOverrideManager() {
        return instance().volumeOverrideManager;
    }

    public Path getSoundFile(UUID id, String extension) {
        return getAudioDataFolder().resolve(id.toString() + "." + extension);
    }

    public Path getAudioDataFolder() {
        return server.getWorldPath(AUDIO_DATA_LEVEL_RESOURCE);
    }

    public short[] getSound(UUID id) throws Exception {
        return AudioPlayer.AUDIO_CACHE.get(id, () -> AudioConverter.convert(getExistingSoundFile(id), volumeOverrideManager.getAudioVolume(id)));
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

    public void handleDownload(AudioImporter downloadHandler, @Nullable ServerPlayer player) {
        //TODO Prevent this from hanging infinitely
        executor.execute(() -> {
            try {
                AudioImportInfo audioDownloadInfo = downloadHandler.onPreprocess(player);
                byte[] bytes = downloadHandler.onProcess(player);
                ChatUtils.checkFileSize(bytes.length);
                UUID id = audioDownloadInfo.soundId();
                String fileName = audioDownloadInfo.name();
                saveSound(id, fileName, bytes);
                if (player != null) {
                    player.sendSystemMessage(ChatUtils.createApplyMessage(id, Component.literal("Successfully imported sound.")));
                }
                downloadHandler.onPostprocess(player);
            } catch (Exception e) {
                runOnMain(() -> {
                    if (player != null) {
                        if (e instanceof ComponentException c) {
                            player.sendSystemMessage(Component.literal("Error: ").append(c.getComponent()).withStyle(ChatFormatting.RED));
                        } else {
                            player.sendSystemMessage(Component.literal("Failed to import sound: %s".formatted(e.getMessage())).withStyle(ChatFormatting.RED));
                        }
                    }
                });
                AudioPlayer.LOGGER.error("Failed to download audio using '{}' download handler", downloadHandler.getHandlerName(), e);
            }
        });
    }

    private void runOnMain(Runnable runnable) {
        server.execute(runnable);
    }

    private void saveSound(UUID id, @Nullable String fileName, byte[] data) throws UnsupportedAudioFileException, IOException {
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

    private static void checkExtensionAllowed(@Nullable AudioConverter.AudioType audioType) throws UnsupportedAudioFileException {
        if (audioType == null) {
            throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        if (audioType.equals(AudioConverter.AudioType.MP3)) {
            if (!AudioPlayer.SERVER_CONFIG.allowMp3Upload.get()) {
                throw new UnsupportedAudioFileException("Importing mp3 files is not allowed on this server");
            }
        }
        if (audioType.equals(AudioConverter.AudioType.WAV)) {
            if (!AudioPlayer.SERVER_CONFIG.allowWavUpload.get()) {
                throw new UnsupportedAudioFileException("Importing wav files is not allowed on this server");
            }
        }
    }

}
