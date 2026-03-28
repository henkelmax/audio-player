package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.api.MessageReceiver;
import de.maxhenkel.audioplayer.audioloader.cache.AudioCache;
import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.audioloader.converter.FfmpegConverter;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.utils.AudioUtils;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.utils.ComponentException;
import de.maxhenkel.audioplayer.utils.upgrade.MetadataUpgrader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
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
    private final FileMetadataManager fileMetadataManager;
    private final AudioCache audioCache;

    public AudioStorageManager(MinecraftServer server) throws Exception {
        this.server = server;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerStorageManagerExecutor");
            thread.setDaemon(true);
            return thread;
        });
        Path audioDataFolder = getAudioDataFolder();
        Files.createDirectories(audioDataFolder);
        Path metaPath = audioDataFolder.resolve("meta.json");
        boolean initial = !Files.exists(metaPath);
        fileMetadataManager = new FileMetadataManager(metaPath);
        audioCache = new AudioCache();
        MetadataUpgrader.upgrade(this, initial);
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
        try {
            instance = new AudioStorageManager(server);
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to initialize audio storage manager");
            throw new RuntimeException(e);
        }
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

    public FileMetadataManager getFileMetadataManager() {
        return fileMetadataManager;
    }

    public static AudioStorageManager instance() {
        return instance;
    }

    public static FileMetadataManager metadataManager() {
        return instance().fileMetadataManager;
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

    public void handleImport(AudioImporter importer, MessageReceiver messageReceiver, @Nullable ServerPlayer player) {
        //TODO Prevent this from hanging infinitely
        executor.execute(() -> {
            try {
                AudioImportInfo audioDownloadInfo = importer.onPreprocess(player);
                byte[] bytes = importer.onProcess(player);
                if (bytes == null || bytes.length <= 0) {
                    throw new ComponentException(Lang.translatable("audioplayer.empty_file"));
                }
                ChatUtils.checkFileSize(bytes.length);
                UUID id = audioDownloadInfo.getAudioId();
                String fileName = audioDownloadInfo.getName();
                saveSound(id, fileName, bytes, player);
                runOnMain(() -> {
                    messageReceiver.sendMessage(ChatUtils.createApplyMessage(id, Lang.translatable("audioplayer.import_successful")));
                });
                importer.onPostprocess(player);
            } catch (Exception e) {
                runOnMain(() -> {
                    if (player != null) {
                        if (e instanceof ComponentException c) {
                            messageReceiver.sendMessage(Lang.translatable("audioplayer.error", c.getComponent()).withStyle(ChatFormatting.RED));
                        } else {
                            messageReceiver.sendMessage(Lang.translatable("audioplayer.error", e.getMessage()).withStyle(ChatFormatting.RED));
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

    private void saveSound(UUID id, @Nullable String fileName, byte[] data, @Nullable ServerPlayer player) throws UnsupportedAudioFileException, IOException, FfmpegConverter.FfmpegException {
        AudioUtils.AudioType audioType = AudioUtils.getAudioType(data);
        if (AudioPlayerMod.SERVER_CONFIG.useFfmpeg.get() && shouldConvertToMp3(audioType)) {
            convertAndSaveSound(id, fileName, data, player);
            return;
        }
        checkExtensionAllowed(audioType);
        try {
            saveSound0(audioType, id, fileName, data, player);
        } catch (UnsupportedAudioFileException e) {
            if (!AudioPlayerMod.SERVER_CONFIG.useFfmpeg.get()) {
                throw e;
            }
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                AudioPlayerMod.LOGGER.error("Failed to save audio without conversion, trying FFmpeg conversion", e);
            } else {
                AudioPlayerMod.LOGGER.error("Failed to save audio without conversion, trying FFmpeg conversion");
            }
            convertAndSaveSound(id, fileName, data, player);
        }
    }

    private void convertAndSaveSound(UUID id, @Nullable String fileName, byte[] data, @Nullable ServerPlayer player) throws UnsupportedAudioFileException, IOException, FfmpegConverter.FfmpegException {
        AudioPlayerMod.LOGGER.info("Converting audio {} to mp3 using FFmpeg", id);
        FfmpegConverter.ConvertedAudio converted = FfmpegConverter.convert(fileName, data);
        saveSound0(converted.audioType(), id, converted.fileName(), converted.data(), player);
    }

    private void saveSound0(AudioUtils.AudioType audioType, UUID id, @Nullable String fileName, byte[] data, @Nullable ServerPlayer player) throws UnsupportedAudioFileException, IOException, FfmpegConverter.FfmpegException {
        float lengthSeconds = AudioUtils.getLengthSeconds(data);
        if (lengthSeconds < 0.1F) {
            // Uploading mp4 files will cause java to detect an mp3 container,
            // but actually decoding it will basically result in a tiny audio file
            throw new UnsupportedAudioFileException("Audio is too short");
        }
        if (lengthSeconds > AudioPlayerMod.SERVER_CONFIG.maxUploadDuration.get()) {
            throw new IOException("Maximum upload duration exceeded (%.1fs>%.1fs)".formatted(lengthSeconds, AudioPlayerMod.SERVER_CONFIG.maxUploadDuration.get()));
        }

        Path soundFile = getSoundFile(id, audioType.getExtension());
        if (Files.exists(soundFile)) {
            throw new FileAlreadyExistsException("This audio already exists");
        }
        Files.createDirectories(soundFile.getParent());

        try (OutputStream outputStream = Files.newOutputStream(soundFile)) {
            IOUtils.write(data, outputStream);
        }

        String finalFileName = fileName;
        fileMetadataManager.modifyMetadata(id, metadata -> {
            metadata.setFileName(finalFileName);
            metadata.setCreated(System.currentTimeMillis());
            if (player != null) {
                metadata.setOwner(Metadata.Owner.of(player));
            }
        });
    }

    private boolean shouldConvertToMp3(@Nullable AudioUtils.AudioType audioType) {
        if (audioType == null) {
            return true;
        }
        // Convert to mp3 if the user tries to upload a wav file when wav uploads are disabled
        if (!AudioPlayerMod.SERVER_CONFIG.allowWavUpload.get() && audioType.equals(AudioUtils.AudioType.WAV)) {
            return true;
        }
        return false;
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
