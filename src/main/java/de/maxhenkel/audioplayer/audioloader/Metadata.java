package de.maxhenkel.audioplayer.audioloader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class Metadata {

    private final UUID audioId;
    @Nullable
    private String fileName;
    @Nullable
    private Float volume;
    @Nullable
    private Long created;
    @Nullable
    private Owner owner;

    public Metadata(UUID audioId) {
        this.audioId = audioId;
    }

    public UUID getAudioId() {
        return audioId;
    }

    @Nullable
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@Nullable String fileName) {
        this.fileName = fileName;
    }

    @Nullable
    public Float getVolume() {
        return volume;
    }

    public void setVolume(@Nullable Float volume) {
        this.volume = volume;
    }

    @Nullable
    public Long getCreated() {
        return created;
    }

    public void setCreated(@Nullable Long created) {
        this.created = created;
    }

    @Nullable
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(@Nullable Owner owner) {
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
        if (owner != null) {
            JsonObject ownerJson = new JsonObject();
            ownerJson.addProperty("uuid", owner.uuid().toString());
            ownerJson.addProperty("name", owner.name());
            json.add("owner", ownerJson);
        }
        return json;
    }

    public static record Owner(@Nonnull UUID uuid, @Nonnull String name) {

        public static Owner of(Player player) {
            return new Owner(player.getGameProfile().id(), player.getGameProfile().name());
        }

    }

}
