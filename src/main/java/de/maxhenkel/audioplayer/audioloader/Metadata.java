package de.maxhenkel.audioplayer.audioloader;

import org.json.JSONObject;

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

    public static Metadata fromJson(UUID audioId, JSONObject json) {
        Metadata metadata = new Metadata(audioId);
        metadata.fileName = json.optString("fileName", null);
        float volume = json.optFloat("volume", -1F);
        if (volume < 0F) {
            metadata.volume = null;
        } else {
            metadata.volume = Math.min(volume, 1F);
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
        return json;
    }

}
