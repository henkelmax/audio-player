package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.utils.FileUtils;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FileMetadataManager {

    private static final int META_VERSION = 1;

    private static final ExecutorService SAVE_EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("MetadataSaver");
        thread.setDaemon(true);
        return thread;
    });

    private final Path file;
    private Map<UUID, Metadata> metadata;

    public FileMetadataManager(Path file) throws Exception {
        this.file = file;
        this.metadata = new ConcurrentHashMap<>();
        load();
    }

    private void load() throws Exception {
        if (!Files.exists(file)) {
            return;
        }
        Map<UUID, Metadata> meta = new ConcurrentHashMap<>();
        String content = Files.readString(file);
        JSONObject root = new JSONObject(content);

        int metaVersion = root.optInt("version", -1);
        //TODO Check meta version

        JSONObject files = root.optJSONObject("files", new JSONObject());
        for (String key : files.keySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                AudioPlayerMod.LOGGER.warn("Invalid UUID in metadata: {}", key);
                continue;
            }
            meta.put(uuid, Metadata.fromJson(uuid, files.getJSONObject(key)));
        }
        metadata = meta;
        saveSync();
    }

    private void saveSync() {
        try {
            Files.createDirectories(file.getParent());
            JSONObject root = new JSONObject();
            root.put("version", META_VERSION);
            JSONObject files = new JSONObject();
            for (Map.Entry<UUID, Metadata> entry : metadata.entrySet()) {
                JSONObject metaJson = entry.getValue().toJson();
                if (metaJson.isEmpty()) {
                    continue;
                }
                files.put(entry.getKey().toString(), metaJson);
            }
            root.put("files", files);
            Files.writeString(file, root.toString(2));
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to save metadata", e);
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

    public void modifyMetadata(UUID uuid, Consumer<Metadata> metadataConsumer) {
        Metadata metadata = this.metadata.computeIfAbsent(uuid, Metadata::new);
        metadataConsumer.accept(metadata);
        saveAsync();
    }

    public void setVolumeOverride(UUID uuid, @Nullable Float volume) {
        modifyMetadata(uuid, metadata -> metadata.setVolume(volume));
    }

    public Optional<Float> getVolumeOverride(UUID uuid) {
        return getMetadata(uuid).map(Metadata::getVolume);
    }

    @Nullable
    public String getFileName(UUID uuid) {
        return getMetadata(uuid).map(Metadata::getFileName).orElse(null);
    }

    public List<Metadata> getByFileName(String fileName, boolean exact) {
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
        String withoutExt = FileUtils.fileNameWithoutExtension(fileName);
        if (withoutExt.equals(name)) {
            return true;
        }
        return fileName.equals(name);
    }


}
