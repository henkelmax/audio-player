package de.maxhenkel.audioplayer.audioloader;

import com.google.gson.*;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.data.AudioFileMetadata;
import de.maxhenkel.audioplayer.utils.FileUtils;
import de.maxhenkel.audioplayer.utils.upgrade.MetadataUpgrader;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FileMetadataManager {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int META_VERSION = 2;

    private static final ExecutorService SAVE_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("MetadataSaver");
        thread.setDaemon(true);
        return thread;
    });

    private final AudioStorageManager audioStorageManager;
    private final Path file;
    private Map<UUID, Metadata> metadata;

    public FileMetadataManager(AudioStorageManager manager, Path file) throws Exception {
        this.audioStorageManager = manager;
        this.file = file;
        this.metadata = new ConcurrentHashMap<>();

        boolean initial = !Files.exists(file);
        MetadataUpgrader.legacyUpgrade(manager, this, initial);

        load();
    }

    private void load() throws Exception {
        if (!Files.exists(file)) {
            return;
        }
        Map<UUID, Metadata> meta = new ConcurrentHashMap<>();
        String content = Files.readString(file);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        JsonElement metaVersionElement = root.get("version");
        int metaVersion;
        if (metaVersionElement == null) {
            metaVersion = -1;
        } else {
            metaVersion = metaVersionElement.getAsInt();
        }
        if (metaVersion != META_VERSION) {
            saveBackup();
        }
        boolean changed = MetadataUpgrader.upgrade(this, root, metaVersion, META_VERSION);

        JsonObject files = root.getAsJsonObject("files");
        if (files == null) {
            files = new JsonObject();
        }
        for (String key : files.keySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                AudioPlayerMod.LOGGER.warn("Invalid UUID in metadata: {}", key);
                continue;
            }
            meta.put(uuid, Metadata.fromJson(uuid, files.getAsJsonObject(key)));
        }
        metadata = meta;

        changed |= MetadataUpgrader.upgradePostLoad(this, root, metaVersion, META_VERSION);
        changed |= MetadataUpgrader.makeFileNamesUnique(this);

        if (changed) {
            saveSync();
        }
    }

    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private void saveSync() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", META_VERSION);
            JsonObject files = new JsonObject();
            for (Map.Entry<UUID, Metadata> entry : metadata.entrySet()) {
                JsonObject metaJson = entry.getValue().toJson();
                if (metaJson.isEmpty()) {
                    continue;
                }
                files.add(entry.getKey().toString(), metaJson);
            }
            root.add("files", files);

            String json = GSON.toJson(root);

            Path backup = file.resolveSibling(FileUtils.stripFileExtension(file.getFileName().toString()) + ".bak");

            if (Files.exists(file)) {
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(file, json);
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to save metadata", e);
        }
    }

    private void saveBackup() {
        String timestamp = LocalDateTime.now().format(BACKUP_FORMATTER);
        Path backup = file.resolveSibling(FileUtils.stripFileExtension(file.getFileName().toString()) + "-" + timestamp + ".bak");
        AudioPlayerMod.LOGGER.info("Saving metadata backup to {}", backup.getFileName());
        try {
            Files.createDirectories(file.getParent());
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to save backup", e);
        }
    }

    public void saveAsync() {
        SAVE_EXECUTOR_SERVICE.execute(this::saveSync);
    }

    public Optional<Metadata> getMetadata(UUID uuid) {
        return Optional.ofNullable(metadata.get(uuid));
    }

    public Metadata getOrCreateMetadata(UUID uuid) {
        return metadata.computeIfAbsent(uuid, Metadata::new);
    }

    public Collection<AudioFileMetadata> getAllMetadata() {
        return Collections.unmodifiableCollection(metadata.values());
    }

    public Map<UUID, Metadata> getMetadata() {
        return metadata;
    }

    public Metadata modifyMetadata(UUID uuid, Consumer<Metadata> metadataConsumer) {
        Metadata metadata = this.metadata.computeIfAbsent(uuid, Metadata::new);
        metadataConsumer.accept(metadata);
        saveAsync();
        return metadata;
    }

    @Nullable
    public Metadata modifyMetadataIfExists(UUID uuid, Consumer<Metadata> metadataConsumer) {
        Metadata metadata = this.metadata.get(uuid);
        if (metadata == null) {
            return null;
        }
        metadataConsumer.accept(metadata);
        saveAsync();
        return metadata;
    }

    public void setVolumeOverride(UUID uuid, @Nullable Float volume) {
        modifyMetadata(uuid, metadata -> metadata.setVolume(volume));
    }

    public Optional<Float> getVolumeOverride(UUID uuid) {
        return getMetadata(uuid).map(Metadata::getVolume);
    }

    public void setUniqueFileName(Metadata toChange, @Nullable String name) {
        if (name == null) {
            toChange.setFileName(null);
            return;
        }
        name = FileUtils.fixName(name);
        if (name.isBlank()) {
            toChange.setFileName(null);
            return;
        }
        while (isDuplicate(toChange, name)) {
            name = FileUtils.deduplicateName(name);
        }
        toChange.setFileName(name);
    }

    private boolean isDuplicate(Metadata self, String name) {
        for (Metadata meta : metadata.values()) {
            if (meta.getAudioId().equals(self.getAudioId())) {
                continue;
            }
            if (name.equals(meta.getFileName())) {
                return true;
            }
        }
        return false;
    }

    public List<Metadata> searchByFileName(String fileName, boolean exact) {
        return metadata.values().stream().filter(metadata -> matchesName(metadata, fileName, exact)).sorted(Comparator.comparingLong(o -> o.getCreated() == null ? 0L : o.getCreated())).toList();
    }

    private static boolean matchesName(Metadata metadata, String name, boolean exact) {
        String fileName = metadata.getFileName();
        if (fileName == null) {
            return false;
        }
        if (!exact) {
            return fileName.toLowerCase().contains(name.toLowerCase());
        }
        return fileName.equals(name);
    }

    public Optional<Metadata> getAudioMetadataByHash(@Nullable String hash) {
        if (hash == null) {
            return Optional.empty();
        }
        for (Metadata meta : metadata.values()) {
            if (hash.equals(meta.getSha256())) {
                return Optional.of(meta);
            }
        }
        return Optional.empty();
    }

    public AudioStorageManager getAudioStorageManager() {
        return audioStorageManager;
    }
}
