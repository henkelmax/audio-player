package de.maxhenkel.audioplayer.utils.upgrade;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.FileMetadataManager;
import de.maxhenkel.audioplayer.audioloader.Metadata;
import de.maxhenkel.audioplayer.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class MetadataUpgrader {

    public static void upgrade(FileMetadataManager fileMetadataManager, boolean initial) {
        if (initial) {
            upgradeCreationDates(fileMetadataManager);
        }
        upgradeFileNameManager(fileMetadataManager);
        upgradeVolumeOverrideManager(fileMetadataManager);
    }

    private static void upgradeCreationDates(FileMetadataManager metadataManager) {
        AudioPlayerMod.LOGGER.info("Upgrading creation dates");
        try (Stream<Path> paths = Files.list(AudioStorageManager.instance().getAudioDataFolder())) {
            paths.forEach(path -> {
                String name = FileUtils.fileNameWithoutExtension(path.getFileName().toString());
                UUID uuid;
                try {
                    uuid = UUID.fromString(name);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
                metadataManager.modifyMetadata(uuid, metadata -> metadata.setCreated(path.toFile().lastModified()));
            });
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to upgrade creation dates", e);
        }
    }

    private static void upgradeFileNameManager(FileMetadataManager manager) {
        Path filenameMappings = AudioStorageManager.instance().getAudioDataFolder().resolve("file-name-mappings.json");
        if (!Files.exists(filenameMappings)) {
            return;
        }
        AudioPlayerMod.LOGGER.info("Upgrading file name mappings");
        FileNameManager fileNameManager = new FileNameManager(filenameMappings);
        for (Map.Entry<UUID, String> entry : fileNameManager.getFileNames().entrySet()) {
            Metadata metadata = manager.getOrCreateMetadata(entry.getKey());
            metadata.setFileName(entry.getValue());
        }
        manager.saveAsync();
        try {
            Files.delete(filenameMappings);
            AudioPlayerMod.LOGGER.info("Deleted old file name mappings config");
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to delete old file name mappings config", e);
        }
    }

    private static void upgradeVolumeOverrideManager(FileMetadataManager manager) {
        Path volumeOverrides = AudioStorageManager.instance().getAudioDataFolder().resolve("volume-overrides.json");
        if (!Files.exists(volumeOverrides)) {
            return;
        }
        AudioPlayerMod.LOGGER.info("Upgrading volume overrides");
        VolumeOverrideManager volumeOverrideManager = new VolumeOverrideManager(volumeOverrides);
        for (Map.Entry<UUID, Float> entry : volumeOverrideManager.getVolumes().entrySet()) {
            Metadata metadata = manager.getOrCreateMetadata(entry.getKey());
            Float logVolume = entry.getValue();
            if (logVolume == null) {
                continue;
            }
            float volumeFactor = Math.min(1F, Math.max(0F, VolumeOverrideManager.convertToLinearScaleFactor(logVolume)));
            if (volumeFactor < 1F) {
                metadata.setVolume(volumeFactor);
            }
        }
        manager.saveAsync();
        try {
            Files.delete(volumeOverrides);
            AudioPlayerMod.LOGGER.info("Deleted old volume overrides config");
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to delete old volume overrides", e);
        }
    }

}
