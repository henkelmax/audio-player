package de.maxhenkel.audioplayer.utils.upgrade;

import com.google.gson.JsonObject;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.FileMetadataManager;
import de.maxhenkel.audioplayer.audioloader.Metadata;
import de.maxhenkel.audioplayer.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class MetadataUpgrader {

    public static void legacyUpgrade(AudioStorageManager manager, FileMetadataManager fileMetadataManager, boolean initial) {
        if (initial) {
            upgradeCreationDates(manager, fileMetadataManager);
        }
        upgradeFileNameManager(manager, fileMetadataManager);
        upgradeVolumeOverrideManager(manager, fileMetadataManager);
    }

    private static void upgradeCreationDates(AudioStorageManager manager, FileMetadataManager metadataManager) {
        AudioPlayerMod.LOGGER.info("Upgrading creation dates");
        try (Stream<Path> paths = Files.list(manager.getAudioDataFolder())) {
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

    private static void upgradeFileNameManager(AudioStorageManager manager, FileMetadataManager metadataManager) {
        Path filenameMappings = manager.getAudioDataFolder().resolve("file-name-mappings.json");
        if (!Files.exists(filenameMappings)) {
            return;
        }
        AudioPlayerMod.LOGGER.info("Upgrading file name mappings");
        FileNameManager fileNameManager = new FileNameManager(filenameMappings);
        for (Map.Entry<UUID, String> entry : fileNameManager.getFileNames().entrySet()) {
            Metadata metadata = metadataManager.getOrCreateMetadata(entry.getKey());
            metadata.setFileName(entry.getValue());
        }
        metadataManager.saveAsync();
        try {
            Files.delete(filenameMappings);
            AudioPlayerMod.LOGGER.info("Deleted old file name mappings config");
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to delete old file name mappings config", e);
        }
    }

    private static void upgradeVolumeOverrideManager(AudioStorageManager manager, FileMetadataManager metadataManager) {
        Path volumeOverrides = manager.getAudioDataFolder().resolve("volume-overrides.json");
        if (!Files.exists(volumeOverrides)) {
            return;
        }
        AudioPlayerMod.LOGGER.info("Upgrading volume overrides");
        VolumeOverrideManager volumeOverrideManager = new VolumeOverrideManager(volumeOverrides);
        for (Map.Entry<UUID, Float> entry : volumeOverrideManager.getVolumes().entrySet()) {
            Metadata metadata = metadataManager.getOrCreateMetadata(entry.getKey());
            Float logVolume = entry.getValue();
            if (logVolume == null) {
                continue;
            }
            float volumeFactor = Math.min(1F, Math.max(0F, VolumeOverrideManager.convertToLinearScaleFactor(logVolume)));
            if (volumeFactor < 1F) {
                metadata.setVolume(volumeFactor);
            }
        }
        metadataManager.saveAsync();
        try {
            Files.delete(volumeOverrides);
            AudioPlayerMod.LOGGER.info("Deleted old volume overrides config");
        } catch (IOException e) {
            AudioPlayerMod.LOGGER.error("Failed to delete old volume overrides", e);
        }
    }

    public static boolean upgrade(FileMetadataManager fileMetadataManager, JsonObject root, int oldVersion, int newVersion) {
        boolean changed = false;
        if (oldVersion < 0) {
            AudioPlayerMod.LOGGER.error("Failed to detect metadata version");
            return changed;
        }
        if (oldVersion == newVersion) {
            return changed;
        }

        // Upgrades go here

        return changed;
    }

    public static boolean upgradePostLoad(FileMetadataManager fileMetadataManager, JsonObject root, int oldVersion, int newVersion) {
        boolean changed = false;
        if (oldVersion < 0) {
            AudioPlayerMod.LOGGER.error("Failed to detect metadata version");
            return changed;
        }
        if (oldVersion == newVersion) {
            return changed;
        }

        if (oldVersion == 1) {
            postUpgradeV1ToV2(fileMetadataManager, root);
            oldVersion = 2;
            changed = true;
        }

        // Other post-upgrades go here
        return changed;
    }

    private static void postUpgradeV1ToV2(FileMetadataManager metaManager, JsonObject root) {
        AudioPlayerMod.LOGGER.info("Generating audio file hashes (This may take a while)");
        for (Metadata meta : metaManager.getMetadata().values()) {
            try {
                Path file = metaManager.getAudioStorageManager().getExistingSoundFile(meta.getAudioId());
                try (InputStream is = Files.newInputStream(file)) {
                    String hash = FileUtils.sha256(is);
                    meta.setSha256(hash);
                    AudioPlayerMod.LOGGER.info("Generated hash for audio {}: {}", meta.getAudioId(), hash);
                }
            } catch (FileNotFoundException e) {
                AudioPlayerMod.LOGGER.error("Failed to find audio file {}", meta.getAudioId());
            } catch (Exception e) {
                AudioPlayerMod.LOGGER.error("Failed to generate hash for audio file {}", meta.getAudioId());
            }
        }

        AudioPlayerMod.LOGGER.info("Stripping file extensions from name");
        for (Metadata meta : metaManager.getMetadata().values()) {
            meta.setFileName(FileUtils.stripFileExtension(meta.getFileName()));
        }
    }

    public static boolean makeFileNamesUnique(FileMetadataManager metaManager) {
        AudioPlayerMod.LOGGER.info("Deduplicating file names");
        ArrayList<Metadata> sorted = new ArrayList<>(metaManager.getMetadata().values());
        sorted.sort(Comparator.comparing(metadata -> metadata.getCreated() == null ? 0L : metadata.getCreated()));

        boolean changed = false;
        Set<String> names = new HashSet<>();
        for (Metadata metadata : sorted) {
            String name = metadata.getFileName();
            if (name == null) {
                continue;
            }
            while (names.contains(name)) {
                name = FileUtils.deduplicateName(name);
                changed = true;
            }
            metadata.setFileName(name);
            names.add(name);
        }

        return changed;
    }

}
