package de.maxhenkel.audioplayer.audioloader.importer;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.importer.AudioImportInfo;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.utils.ComponentException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;

public class ServerfileImporter implements AudioImporter {

    private final UUID soundId;
    private final String fileName;
    private Path file;

    public ServerfileImporter(String fileName) {
        this.soundId = UUID.randomUUID();
        this.fileName = fileName;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        Path uploadFolder = AudioStorageManager.getUploadFolder();
        file = uploadFolder.resolve(fileName);

        if (!uploadFolder.equals(file.getParent())) {
            throw new ComponentException(Lang.translatable("audioplayer.invalid_file_name"));
        }

        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new NoSuchFileException("The file %s does not exist".formatted(file.toString()));
        }
        long size = Files.size(file);
        ChatUtils.checkFileSize(size);
        return new AudioImportInfo(soundId, getFileNameFromPath(file));
    }

    @Nullable
    public static String getFileNameFromPath(Path path) {
        if (Files.isDirectory(path)) {
            return null;
        }
        String name = path.getFileName().toString();
        if (name.isEmpty()) {
            return null;
        }
        return name;
    }

    @Override
    public byte[] onProcess(@Nullable ServerPlayer player) throws Exception {
        return IOUtils.toByteArray(Files.newInputStream(file));
    }

    @Override
    public void onPostprocess(@Nullable ServerPlayer player) throws Exception {
        try {
            Files.delete(file);
            if (player != null) {
                player.sendSystemMessage(Lang.translatable("audioplayer.deleted_temp_file", Component.literal(fileName).withStyle(ChatFormatting.GRAY)));
            }
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to delete file {}", file, e);
        }
    }

    @Override
    public String getHandlerName() {
        return "serverfile";
    }

}
