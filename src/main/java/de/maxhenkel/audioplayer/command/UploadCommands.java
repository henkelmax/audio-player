package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.Filebin;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command("audioplayer")
public class UploadCommands {

    public static final Pattern SOUND_FILE_PATTERN = Pattern.compile("^[a-z0-9_ -]+.((wav)|(mp3))$", Pattern.CASE_INSENSITIVE);

    @RequiresPermission("audioplayer.upload")
    @Command
    public void audioPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                Component.literal("Upload audio via Filebin ")
                        .append(Component.literal("here").withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer upload"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                        }).withStyle(ChatFormatting.GREEN))
                        .append(".")
                , false);
        context.getSource().sendSuccess(
                Component.literal("Upload audio with access to the servers file system ")
                        .append(Component.literal("here").withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer serverfile"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                        }).withStyle(ChatFormatting.GREEN))
                        .append(".")
                , false);
        context.getSource().sendSuccess(
                Component.literal("Upload audio from a URL ")
                        .append(Component.literal("here").withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer url"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to show more")));
                        }).withStyle(ChatFormatting.GREEN))
                        .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("upload")
    @Command("filebin")
    public void filebin(CommandContext<CommandSourceStack> context) {
        UUID uuid = UUID.randomUUID();
        String uploadURL = Filebin.getBin(uuid);

        MutableComponent msg = Component.literal("Click ")
                .append(Component.literal("this link")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uploadURL))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to open")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" and upload your sound as ")
                .append(Component.literal("mp3").withStyle(ChatFormatting.GRAY))
                .append(" or ")
                .append(Component.literal("wav").withStyle(ChatFormatting.GRAY))
                .append(".\n")
                .append("Once you have uploaded the file, click ")
                .append(Component.literal("here")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer filebin " + uuid))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to confirm upload")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(".");

        context.getSource().sendSuccess(msg, false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("filebin")
    public void filebinUpload(CommandContext<CommandSourceStack> context, @Name("id") UUID sound) {
        new Thread(() -> {
            try {
                context.getSource().sendSuccess(Component.literal("Downloading sound, please wait..."), false);
                Filebin.downloadSound(context.getSource().getServer(), sound);
                context.getSource().sendSuccess(sendUUIDMessage(sound, Component.literal("Successfully downloaded sound.")), false);
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.getMessage());
                context.getSource().sendFailure(Component.literal("Failed to download sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void url(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                Component.literal("If you have a direct link to a ")
                        .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                        .append(" or ")
                        .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                        .append(" file, enter the following command: ")
                        .append(Component.literal("/audioplayer url <link-to-your-file>").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer url "))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to fill in the command")));
                        }))
                        .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void urlUpload(CommandContext<CommandSourceStack> context, @Name("url") String url) {
        UUID sound = UUID.randomUUID();
        new Thread(() -> {
            try {
                context.getSource().sendSuccess(Component.literal("Downloading sound, please wait..."), false);
                AudioManager.saveSound(context.getSource().getServer(), sound, url);
                context.getSource().sendSuccess(sendUUIDMessage(sound, Component.literal("Successfully downloaded sound.")), false);
            } catch (UnknownHostException e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Failed to download sound: Unknown host"));
            } catch (UnsupportedAudioFileException e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Failed to download sound: Invalid file format"));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to download a sound: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Failed to download sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFile(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                Component.literal("Upload a ")
                        .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                        .append(" or ")
                        .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                        .append(" file to ")
                        .append(Component.literal(AudioManager.getUploadFolder().toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY))
                        .append(" on the server and run the command ")
                        .append(Component.literal("/audioplayer serverfile \"yourfile.mp3\"").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer serverfile "))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to fill in the command")));
                        }))
                        .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFileUpload(CommandContext<CommandSourceStack> context, @Name("filename") String fileName) {
        Matcher matcher = SOUND_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            context.getSource().sendFailure(Component.literal("Invalid file name! Valid characters are ")
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
                    .append(".")
            );
            return;
        }
        UUID uuid = UUID.randomUUID();
        new Thread(() -> {
            Path file = AudioManager.getUploadFolder().resolve(fileName);
            try {
                AudioManager.saveSound(context.getSource().getServer(), uuid, file);
                context.getSource().sendSuccess(sendUUIDMessage(uuid, Component.literal("Successfully copied sound.")), false);
                context.getSource().sendSuccess(Component.literal("Deleted temporary file ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."), false);
            } catch (NoSuchFileException e) {
                context.getSource().sendFailure(Component.literal("Could not find file ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} failed to copy a sound: {}", context.getSource().getTextName(), e.getMessage());
                context.getSource().sendFailure(Component.literal("Failed to copy sound: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    public static MutableComponent sendUUIDMessage(UUID soundID, MutableComponent component) {
        return component.append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Copy ID"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, soundID.toString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy sound ID")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on item"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer apply %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Put the sound on an item")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                );
    }

}
