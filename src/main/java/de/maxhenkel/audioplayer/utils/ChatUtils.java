package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.UUID;

public class ChatUtils {

    public static MutableComponent createApplyMessage(UUID audioID, MutableComponent component) {
        return component.append(" ")
                .append(net.minecraft.network.chat.ComponentUtils.wrapInSquareBrackets(Component.literal("Copy ID"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.CopyToClipboard(audioID.toString()))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy audio ID")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Put on item"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.SuggestCommand("/audioplayer apply %s".formatted(audioID.toString())))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Put the audio on an item")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                );
    }

    public static void checkFileSize(long size) throws IOException {
        if (size > AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get()) {
            throw new IOException("Maximum file size exceeded (%sMB>%sMB)".formatted(Math.round((float) size / 1_000_000F), Math.round(AudioPlayerMod.SERVER_CONFIG.maxUploadSize.get().floatValue() / 1_000_000F)));
        }
    }

    public static void notifyToEnableVoicechatIfNoVoicechat(ServerPlayer player) {
        if (isAbleToHearVoicechat(player)) {
            return;
        }
        sendEnableVoicechatMessage(player);
    }

    public static boolean isAbleToHearVoicechat(ServerPlayer player) {
        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) {
            return false;
        }
        VoicechatConnection connection = api.getConnectionOf(player.getUUID());
        return isAbleToHearVoicechat(connection);
    }

    public static boolean isAbleToHearVoicechat(VoicechatConnection connection) {
        return connection != null && !connection.isDisabled() && connection.isConnected() && connection.isInstalled();
    }

    public static void sendEnableVoicechatMessage(ServerPlayer player) {
        player.displayClientMessage(Component.literal("You need to enable voice chat to hear custom audio"), true);
    }

}
