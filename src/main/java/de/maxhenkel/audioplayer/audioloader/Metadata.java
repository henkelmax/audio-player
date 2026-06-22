package de.maxhenkel.audioplayer.audioloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.data.AudioFileMetadata;
import de.maxhenkel.audioplayer.api.data.AudioFileOwner;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class Metadata implements AudioFileMetadata {

    private final UUID audioId;
    @Nullable
    private String fileName;
    @Nullable
    private Float volume;
    @Nullable
    private Long created;
    @Nullable
    private AudioFileOwner owner;
    @Nullable
    private String sha256;

    public Metadata(UUID audioId) {
        this.audioId = audioId;
    }

    @Override
    public UUID getAudioId() {
        return audioId;
    }

    @Nullable
    @Override
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@Nullable String fileName) {
        this.fileName = fileName;
    }

    @Nullable
    @Override
    public Float getVolume() {
        return volume;
    }

    public void setVolume(@Nullable Float volume) {
        this.volume = volume;
    }

    @Nullable
    @Override
    public Long getCreated() {
        return created;
    }

    public void setCreated(@Nullable Long created) {
        this.created = created;
    }

    @Nullable
    @Override
    public String getSha256() {
        return sha256;
    }

    public void setSha256(@Nullable String sha256) {
        this.sha256 = sha256;
    }

    @Nullable
    @Override
    public AudioFileOwner getOwner() {
        return owner;
    }

    public void setOwner(@Nullable AudioFileOwner owner) {
        this.owner = owner;
    }

    public static Metadata fromJson(UUID audioId, JsonObject json) {
        Metadata metadata = new Metadata(audioId);
        JsonElement filenameElement = json.get("fileName");
        metadata.fileName = filenameElement == null ? null : filenameElement.getAsString();
        JsonElement volumeElement = json.get("volume");
        metadata.volume = volumeElement == null ? null : Math.max(Math.min(volumeElement.getAsFloat(), 1F), 0F);
        JsonElement createdElement = json.get("created");
        metadata.created = createdElement == null ? null : createdElement.getAsLong();
        JsonElement sha256Element = json.get("sha256");
        metadata.sha256 = sha256Element == null ? null : sha256Element.getAsString();
        JsonObject ownerJson = json.getAsJsonObject("owner");
        if (ownerJson != null) {
            try {
                JsonElement uuidElement = ownerJson.get("uuid");
                String uuidString = uuidElement == null ? null : uuidElement.getAsString();
                JsonElement nameElement = ownerJson.get("name");
                String name = nameElement == null ? null : nameElement.getAsString();
                if (uuidString != null && name != null) {
                    UUID uuid = UUID.fromString(uuidString);
                    metadata.owner = new Owner(uuid, name);
                }
            } catch (IllegalArgumentException e) {
                AudioPlayerMod.LOGGER.warn("Invalid owner UUID in metadata", e);
            }
        }
        return metadata;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (fileName != null && !fileName.isBlank()) {
            json.addProperty("fileName", fileName);
        }
        if (volume != null) {
            json.addProperty("volume", volume);
        }
        if (created != null) {
            json.addProperty("created", created);
        }
        if (sha256 != null) {
            json.addProperty("sha256", sha256);
        }
        if (owner != null) {
            JsonObject ownerJson = new JsonObject();
            ownerJson.addProperty("uuid", owner.getUUID().toString());
            ownerJson.addProperty("name", owner.getName());
            json.add("owner", ownerJson);
        }
        return json;
    }

    public static class Owner implements AudioFileOwner {

        private final UUID uuid;
        private final String name;

        public Owner(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public static Owner of(Player player) {
            return new Owner(player.getGameProfile().id(), player.getGameProfile().name());
        }

        @Override
        @Nonnull
        public UUID getUUID() {
            return uuid;
        }

        @Override
        @Nonnull
        public String getName() {
            return name;
        }
    }

}
