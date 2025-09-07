package de.maxhenkel.audioplayer.utils;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.Metadata;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ChatUtils {

    public static MutableComponent createApplyMessage(UUID audioID, MutableComponent component) {
        component.append(" ");
        component.append(net.minecraft.network.chat.ComponentUtils.wrapInSquareBrackets(Lang.translatable("audioplayer.copy_id"))
                .withStyle(style -> {
                    return style
                            .withClickEvent(new ClickEvent.CopyToClipboard(audioID.toString()))
                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.copy_id_tooltip")));
                })
                .withStyle(ChatFormatting.GREEN)
        );
        component.append(" ");
        component.append(ComponentUtils.wrapInSquareBrackets(Lang.translatable("audioplayer.put_on_item"))
                .withStyle(style -> {
                    return style
                            .withClickEvent(new ClickEvent.SuggestCommand("/audioplayer apply %s".formatted(audioID.toString())))
                            .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.put_on_item_tooltip")));
                })
                .withStyle(ChatFormatting.GREEN)
        );
        return component;
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static MutableComponent createInfoMessage(UUID audioID) {
        @Nullable Metadata metadata = AudioStorageManager.metadataManager().getMetadata(audioID).orElse(null);
        MutableComponent base = Component.empty();

        String fileName = metadata == null ? null : metadata.getFileName();
        if (fileName != null) {
            base.append(Component.literal(fileName).withStyle(style -> {
                return style
                        .withColor(ChatFormatting.BLUE)
                        .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_copy")))
                        .withClickEvent(new ClickEvent.CopyToClipboard(fileName));
            }));
        } else {
            base.append(Lang.translatable("audioplayer.unnamed").withStyle(ChatFormatting.BLUE));
        }
        Long created = metadata == null ? null : metadata.getCreated();
        if (created != null) {
            base.append(" ");
            base.append(Lang.translatable("audioplayer.creation_date", Component.literal(DATE_FORMAT.format(new Date(created))).withStyle(ChatFormatting.GRAY)));
        }
        Metadata.Owner owner = metadata == null ? null : metadata.getOwner();
        if (owner != null) {
            base.append(" ");
            base.append(Lang.translatable("audioplayer.by", Component.literal(owner.name()).withStyle(style -> {
                return style
                        .withHoverEvent(new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(EntityType.PLAYER, owner.uuid(), Component.literal(owner.name()))))
                        .withColor(ChatFormatting.GRAY);
            })));
        }

        return createApplyMessage(audioID, base);
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
        player.displayClientMessage(Lang.translatable("audioplayer.enable_voicechat"), true);
    }

}
