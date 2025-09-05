package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return;
        }

        if (!AudioData.clearItem(context.getSource().getServer(), itemInHand)) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal("Successfully cleared item"), false);
    }

    @Command("id")
    public void id(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AudioData data = getHeldData(context);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }
        UUID actualSoundId = data.getActualSoundId();
        if (actualSoundId == null) {
            context.getSource().sendFailure(Component.literal("Item does not have an audio ID"));
            return;
        }
        context.getSource().sendSuccess(() -> ChatUtils.createApplyMessage(actualSoundId, Component.literal("Successfully extracted sound ID.")), false);
    }

    @Command("name")
    public void name(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        AudioData data = getHeldData(context);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }
        UUID actualSoundId = data.getActualSoundId();
        if (actualSoundId == null) {
            context.getSource().sendFailure(Component.literal("Item does not have an audio ID"));
            return;
        }
        sendSoundName(context, actualSoundId);
    }

    public static void sendSoundName(CommandContext<CommandSourceStack> context, UUID id) {
        String fileName = AudioStorageManager.fileNameManager().getFileName(id);
        if (fileName == null) {
            context.getSource().sendFailure(Component.literal("Custom audio does not have an associated file name"));
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal("Audio file name: ").append(Component.literal(fileName).withStyle(style -> {
            return style
                    .withColor(ChatFormatting.GREEN)
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                    .withClickEvent(new ClickEvent.CopyToClipboard(fileName));
        })), false);
    }

    public static AudioData getHeldData(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);

        if (playerType == null) {
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return null;
        }

        AudioData data = AudioData.of(itemInHand);
        if (data == null) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return null;
        }

        return data;
    }

}
