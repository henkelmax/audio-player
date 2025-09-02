package de.maxhenkel.audioplayer.audioloader.importer;

import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerfileImporter implements AudioImporter {

    public static final Pattern SOUND_FILE_PATTERN = Pattern.compile("^[a-z0-9_ -]+.((wav)|(mp3))$", Pattern.CASE_INSENSITIVE);

    private final UUID soundId;
    private final String fileName;
    private Path file;

    public ServerfileImporter(String fileName) {
        this.soundId = UUID.randomUUID();
        this.fileName = fileName;
    }

    @Override
    public AudioImportInfo onPreprocess(@Nullable ServerPlayer player) throws Exception {
        Matcher matcher = SOUND_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            throw new ComponentException(Component.literal("Invalid file name! Valid characters are ")
                    .append(Component.literal("A-Z").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("0-9").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("_").withStyle(ChatFormatting.GRAY))
                    .append(" and ")
                    .append(Component.literal("-").withStyle(ChatFormatting.GRAY))
                    .append(". The name must also end in ")
                    .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                    .append(" or ")
                    .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                    .append("."));
        }
        file = AudioStorageManager.getUploadFolder().resolve(fileName);
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
                player.sendSystemMessage(Component.literal("Deleted temporary file ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."));
            }
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to delete file {}", file, e);
        }
    }

    @Override
    public String getHandlerName() {
        return "url";
    }

}
