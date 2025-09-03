package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.audioloader.cache.AudioCache;
import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.utils.AudioUtils;
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
    private final FileNameManager fileNameManager;
    private final VolumeOverrideManager volumeOverrideManager;
    private final AudioCache audioCache;

    public AudioStorageManager(MinecraftServer server) {
        this.server = server;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerStorageManagerExecutor");
            thread.setDaemon(true);
            return thread;
        });
        fileNameManager = new FileNameManager(getAudioDataFolder().resolve("file-name-mappings.json"));
        volumeOverrideManager = new VolumeOverrideManager(getAudioDataFolder().resolve("volume-overrides.json"));
        audioCache = new AudioCache();
    }

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(AudioStorageManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(AudioStorageManager::onServerStopped);
        try {
            Files.createDirectories(AudioStorageManager.getUploadFolder());
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.warn("Failed to create upload folder", e);
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

    public static FileNameManager fileNameManager() {
        return instance().fileNameManager;
    }

    public static VolumeOverrideManager volumeOverrideManager() {
        return instance().volumeOverrideManager;
    }

    public static AudioCache audioCache() {
        return instance().audioCache;
    }

    public Path getSoundFile(UUID id, String extension) {
        return getAudioDataFolder().resolve(id.toString() + "." + extension);
    }

    public Path getAudioDataFolder() {
        return server.getWorldPath(AUDIO_DATA_LEVEL_RESOURCE);
    }

    public Path getExistingSoundFile(UUID id) throws FileNotFoundException {
        Path file = getSoundFile(id, AudioUtils.AudioType.MP3.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        file = getSoundFile(id, AudioUtils.AudioType.WAV.getExtension());
        if (Files.exists(file)) {
            return file;
        }
        throw new FileNotFoundException("Audio does not exist");
    }

    public boolean checkSoundExists(UUID id) {
        Path file = getSoundFile(id, AudioUtils.AudioType.MP3.getExtension());
        if (Files.exists(file)) {
            return true;
        }
        file = getSoundFile(id, AudioUtils.AudioType.WAV.getExtension());
        return Files.exists(file);
    }

    public static Path getUploadFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("audioplayer_uploads");
    }

    public void handleImport(AudioImporter importer, @Nullable ServerPlayer player) {
        //TODO Prevent this from hanging infinitely
        executor.execute(() -> {
            try {
                AudioImportInfo audioDownloadInfo = importer.onPreprocess(player);
                byte[] bytes = importer.onProcess(player);
                ChatUtils.checkFileSize(bytes.length);
                UUID id = audioDownloadInfo.getAudioId();
                String fileName = audioDownloadInfo.getName();
                saveSound(id, fileName, bytes);
                if (player != null) {
                    player.sendSystemMessage(ChatUtils.createApplyMessage(id, Component.literal("Successfully imported sound.")));
                }
                importer.onPostprocess(player);
            } catch (Exception e) {
                runOnMain(() -> {
                    if (player != null) {
                        if (e instanceof ComponentException c) {
                            player.sendSystemMessage(Component.literal("Error: ").append(c.getComponent()).withStyle(ChatFormatting.RED));
                        } else {
                            player.sendSystemMessage(Component.literal("Error: %s".formatted(e.getMessage())).withStyle(ChatFormatting.RED));
                        }
                    }
                });
                AudioPlayerMod.LOGGER.error("Failed to download audio using '{}' download handler", importer.getHandlerName(), e);
            }
        });
    }

    private void runOnMain(Runnable runnable) {
        server.execute(runnable);
    }

    private void saveSound(UUID id, @Nullable String fileName, byte[] data) throws UnsupportedAudioFileException, IOException {
        AudioUtils.AudioType audioType = AudioUtils.getAudioType(data);
        checkExtensionAllowed(audioType);

        Path soundFile = getSoundFile(id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        float lengthSeconds = AudioUtils.getLengthSeconds(data);
        if (lengthSeconds > AudioPlayerMod.SERVER_CONFIG.maxUploadDuration.get().floatValue()) {
            throw new IOException("Maximum upload duration exceeded (%.1fs>%ss)".formatted(lengthSeconds, AudioPlayerMod.SERVER_CONFIG.maxUploadDuration.get()));
        }

        try (OutputStream outputStream = Files.newOutputStream(soundFile)) {
            IOUtils.write(data, outputStream);
        }

        fileNameManager.addFileName(id, fileName);
    }

    private static void checkExtensionAllowed(@Nullable AudioUtils.AudioType audioType) throws UnsupportedAudioFileException {
        if (audioType == null) {
            throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        if (audioType.equals(AudioUtils.AudioType.MP3)) {
            if (!AudioPlayerMod.SERVER_CONFIG.allowMp3Upload.get()) {
                throw new UnsupportedAudioFileException("Importing mp3 files is not allowed on this server");
            }
        }
        if (audioType.equals(AudioUtils.AudioType.WAV)) {
            if (!AudioPlayerMod.SERVER_CONFIG.allowWavUpload.get()) {
                throw new UnsupportedAudioFileException("Importing wav files is not allowed on this server");
            }
        }
    }

}
