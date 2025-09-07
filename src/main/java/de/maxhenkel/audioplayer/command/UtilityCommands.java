package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.Metadata;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

import java.util.*;

@Command("audioplayer")
public class UtilityCommands {

    @RequiresPermission(AudioPlayerPermissionManager.APPLY_PERMISSION_STRING)
    @Command("clear")
    public void clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);
        if (playerType == null) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.invalid_item"));
            return;
        }

        if (!AudioData.clearItem(context.getSource().getServer(), itemInHand)) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.item_no_audio"));
            return;
        }

        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.item_clear_successful"), false);
    }

    @Command("id")
    public void id(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID id = getHeldItemId(context);
        if (id == null) {
            return;
        }
        context.getSource().sendSuccess(() -> ChatUtils.createApplyMessage(id, Lang.translatable("audioplayer.extract_sound_id_successful")), false);
    }

    @Command("info")
    public void info(CommandContext<CommandSourceStack> context, @Name("id") UUID id) {
        if (!AudioStorageManager.instance().checkSoundExists(id)) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_audio_file_id_found", id.toString()));
            return;
        }
        context.getSource().sendSuccess(() -> ChatUtils.createInfoMessage(id), false);
    }

    @Command("info")
    public void info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID id = getHeldItemId(context);
        if (id == null) {
            return;
        }
        context.getSource().sendSuccess(() -> ChatUtils.createInfoMessage(id), false);
    }

    @Command("search")
    public void search(CommandContext<CommandSourceStack> context, @Name("file_name") String name) throws CommandSyntaxException {
        List<Metadata> metadata = AudioStorageManager.metadataManager().getByFileName(name, false);

        if (metadata.isEmpty()) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.no_audio_files_name_found", name));
            return;
        }

        context.getSource().sendSuccess(() -> Lang.translatable("audioplayer.search_results", metadata.size(), name), false);

        for (Metadata meta : metadata) {
            context.getSource().sendSuccess(() -> ChatUtils.createInfoMessage(meta.getAudioId()), false);
        }
    }

    public static UUID getHeldItemId(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        AudioData data = AudioData.of(itemInHand);
        if (data == null) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.item_no_audio"));
            return null;
        }
        UUID actualSoundId = data.getActualSoundId();
        if (actualSoundId == null) {
            context.getSource().sendFailure(Lang.translatable("audioplayer.item_no_audio_id"));
            return null;
        }

        return actualSoundId;
    }

}
