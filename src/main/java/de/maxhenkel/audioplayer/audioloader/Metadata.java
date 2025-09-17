package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.world.entity.player.Player;
import org.json.JSONObject;

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

    public static Metadata fromJson(UUID audioId, JSONObject json) {
        Metadata metadata = new Metadata(audioId);
        metadata.fileName = json.optString("fileName", null);
        float volume = json.optFloat("volume", -1F);
        if (volume < 0F) {
            metadata.volume = null;
        } else {
            metadata.volume = Math.min(volume, 1F);
        }
        long created = json.optLong("created", -1L);
        if (created < 0L) {
            metadata.created = null;
        } else {
            metadata.created = created;
        }
        JSONObject ownerJson = json.optJSONObject("owner", null);
        if (ownerJson != null) {
            try {
                String uuidString = ownerJson.optString("uuid", null);
                String name = ownerJson.optString("name", null);
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

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (fileName != null && !fileName.isBlank()) {
            json.put("fileName", fileName);
        }
        if (volume != null) {
            json.put("volume", volume);
        }
        if (created != null) {
            json.put("created", created);
        }
        if (owner != null) {
            JSONObject ownerJson = new JSONObject();
            ownerJson.put("uuid", owner.uuid());
            ownerJson.put("name", owner.name());
            json.put("owner", ownerJson);
        }
        return json;
    }

    public static record Owner(@Nonnull UUID uuid, @Nonnull String name) {

        public static Owner of(Player player) {
            return new Owner(player.getGameProfile().id(), player.getGameProfile().name());
        }

    }

}
