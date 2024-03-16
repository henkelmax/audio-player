package de.maxhenkel.audioplayer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

public class FilenameMappings {

    public static void append(MinecraftServer server, String filename, UUID sound) {
        URI location = server.getWorldPath(AudioManager.AUDIO_DATA).resolve("filename_mappings.dat").toUri();
        File mappingsFile = new File(location);
        String extension = filename.substring(filename.length() - 4);

        try {
            CompoundTag root = loadOrDefault(mappingsFile);
            ListTag mappings = getMappingListOrDefault(root);

            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", sound.toString() + extension);
            entry.putString("filename", filename);

            mappings.add(entry);

            if (!root.contains("mappings")) {
                root.put("mappings", mappings);
            }

            NbtIo.writeCompressed(root, mappingsFile.toPath());
        } catch (IOException e) {
            System.err.println("Permission Denied. Failed to read or write the file.");
        }

        verifyFilesExist(server);
    }

    public static void verifyFilesExist(MinecraftServer server) {
        URI location = server.getWorldPath(AudioManager.AUDIO_DATA).resolve("filename_mappings.dat").toUri();
        File mappingsFile = new File(location);

        try {
            CompoundTag root = loadOrDefault(mappingsFile);
            ListTag mappings = getMappingListOrDefault(root);

            for (int i = 0; i < mappings.size(); i++) {
                String uuid = mappings.getCompound(i).getString("uuid");
                URI filepath = server.getWorldPath(AudioManager.AUDIO_DATA).resolve(uuid).toUri();

                if (!new File(filepath).exists()) {
                    mappings.remove(i);
                }
            }

            NbtIo.writeCompressed(root, mappingsFile.toPath());
        } catch (IOException e) {
            System.err.println("Permission Denied. Failed to read or write the file.");
        }
    }

    public static void autoSaveHappened() {
        AudioPlayer.LOGGER.warn("Auto Save Triggered!");
    }

    private static CompoundTag loadOrDefault(File filepath) throws IOException {
        if (filepath.exists()) {
            return NbtIo.readCompressed(filepath.toPath(), NbtAccounter.unlimitedHeap());
        }
        return new CompoundTag();
    }

    private static ListTag getMappingListOrDefault(CompoundTag tag) {
        if (tag.contains("mappings")) {
            return (ListTag) tag.get("mappings");
        }
        return new ListTag();
    }
}
