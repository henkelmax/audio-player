package de.maxhenkel.audioplayer.utils.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.maxhenkel.audioplayer.AudioPlayerMod;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Deprecated
public class VolumeOverrideManager {

    private final Path file;
    private final Gson gson;
    private Map<UUID, Float> volumes;

    public VolumeOverrideManager(Path file) {
        this.file = file;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
        this.volumes = new HashMap<>();
        load();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type fileNameMapType = new TypeToken<Map<UUID, Float>>() {
            }.getType();
            volumes = gson.fromJson(reader, fileNameMapType);
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to load volume overrides", e);
        }
        if (volumes == null) {
            volumes = new HashMap<>();
        }
    }

    public Map<UUID, Float> getVolumes() {
        return volumes;
    }

    private static final float LOG_BASE = 2F;

    public static float convertToLinearScaleFactor(float logarithmicScaleFactor) {
        if (logarithmicScaleFactor <= 0F) {
            return 0F;
        }

        return 1F + (float) (Math.log10(logarithmicScaleFactor) / LOG_BASE);
    }

}
