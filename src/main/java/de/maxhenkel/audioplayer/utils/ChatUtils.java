package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.ComponentUtils;

import java.io.IOException;
import java.util.UUID;

public class ChatUtils {

    public static MutableComponent createApplyMessage(UUID soundID, MutableComponent component) {
        return component.append(" ")
                .append(net.minecraft.network.chat.ComponentUtils.wrapInSquareBrackets(Component.literal("Copy ID"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.CopyToClipboard(soundID.toString()))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy sound ID")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on item"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.SuggestCommand("/audioplayer apply %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Put the sound on an item")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                );
    }

    public static void checkFileSize(long size) throws IOException {
        if (size > AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
        }
    }

}
