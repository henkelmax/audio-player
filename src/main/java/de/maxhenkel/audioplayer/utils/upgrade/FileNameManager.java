package de.maxhenkel.audioplayer.utils.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.maxhenkel.audioplayer.AudioPlayerMod;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Deprecated
public class FileNameManager {

    private final Path file;
    private final Gson gson;
    private Map<UUID, String> fileNames;

    public FileNameManager(Path file) {
        this.file = file;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
        this.fileNames = new HashMap<>();
        load();
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Type fileNameMapType = new TypeToken<Map<UUID, String>>() {
            }.getType();
            fileNames = gson.fromJson(reader, fileNameMapType);
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to load file name mappings", e);
        }
        if (fileNames == null) {
            fileNames = new HashMap<>();
        }
    }

    public Map<UUID, String> getFileNames() {
        return fileNames;
    }

}
