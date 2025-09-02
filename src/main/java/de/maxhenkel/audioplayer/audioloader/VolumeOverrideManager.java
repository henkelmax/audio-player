package de.maxhenkel.audioplayer.audioloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.maxhenkel.audioplayer.AudioPlayer;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VolumeOverrideManager {

    private final Path file;
    private final Gson gson;
    private Map<UUID, Float> volumes;

    public VolumeOverrideManager(Path file) {
        this.file = file;
        this.gson = new GsonBuilder().create();
        this.volumes = new HashMap<>();
        load();
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type fileNameMapType = new TypeToken<Map<UUID, Float>>() {
            }.getType();
            volumes = gson.fromJson(reader, fileNameMapType);
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to load volume overrides", e);
        }
        if (volumes == null) {
            volumes = new HashMap<>();
        }
        save();
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(volumes, writer);
            }
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to save file name mappings", e);
        }
    }

    /**
     * Gets the volume override associated with the provided sound ID, or 1 if there is no override set.
     *
     * @param audioId the audio ID
     */
    public float getAudioVolume(UUID audioId) {
        return volumes.getOrDefault(audioId, 1F);
    }

    /**
     * Sets the volume override associated with the provided ID, removes override if the volume is <code>null</code>.
     *
     * @param audioId the audio ID
     * @param volume  the file name or <code>null</code>
     */
    public void setAudioVolume(UUID audioId, @Nullable Float volume) {
        if (volume == null) {
            volumes.remove(audioId);
            //TODO Save off-thread
            save();
            return;
        }
        volumes.put(audioId, volume);
        //TODO Save off-thread
        save();
    }

    private static final float LOG_BASE = 2F;

    public static float convertToLinearScaleFactor(float logarithmicScaleFactor) {
        if (logarithmicScaleFactor <= 0F) {
            return 0F;
        }

        return 1F + (float) (Math.log10(logarithmicScaleFactor) / LOG_BASE);
    }

    public static float convertToLogarithmicScaleFactor(float linearScaleFactor) {
        linearScaleFactor = Math.max(0F, Math.min(linearScaleFactor, 1F));

        return (float) Math.pow(10D, (linearScaleFactor - 1F) * LOG_BASE);
    }

}
