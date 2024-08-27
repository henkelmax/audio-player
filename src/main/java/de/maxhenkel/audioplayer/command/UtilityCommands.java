package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Command("audioplayer")
public class UtilityCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("clear")
    public void clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);
        if (playerType == null) {
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return;
        }

        if (!itemInHand.hasTag()) {
            context.getSource().sendFailure(Component.literal("Item does not contain NBT data"));
            return;
        }

        if (!CustomSound.clearItem(itemInHand)) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }

        CompoundTag tag = itemInHand.getTag();
        if (tag == null) {
            return;
        }

        if (itemInHand.getItem() instanceof InstrumentItem) {
            tag.putString("instrument", "minecraft:ponder_goat_horn");
        }

        tag.remove(ItemStack.TAG_DISPLAY);
        tag.remove("HideFlags");

        context.getSource().sendSuccess(() -> Component.literal("Successfully cleared item"), false);
    }

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeWithId(CommandContext<CommandSourceStack> context, @Name("id") UUID uuid, @OptionalArgument @Name("volume") @Min("0.00") @Max("100.00") Float volume) throws CommandSyntaxException {
        volumeCommand(context,uuid,volume);
    }

    @RequiresPermission("audioplayer.volume")
    @Command("volume")
    public void volumeHeldItem(CommandContext<CommandSourceStack> context, @OptionalArgument @Name("volume") @Min("0.00") @Max("100.00") Float volume) throws CommandSyntaxException {
        CustomSound customSound = getHeldSound(context);
        if (customSound == null) {
            return;
        }
        volumeCommand(context,customSound.getSoundId(),volume);
    }

    private void volumeCommand(CommandContext<CommandSourceStack> context, UUID id, @Nullable Float volume) {
        if (!AudioManager.checkSoundExists(context.getSource().getServer(),id)) {
            context.getSource().sendFailure(Component.literal("Sound does not exist"));
            return;
        }
        Optional<VolumeOverrideManager> optionalMgr = VolumeOverrideManager.instance();
        if (optionalMgr.isEmpty()) {
            context.getSource().sendFailure(Component.literal("An internal error occurred"));
            return;
        }
        VolumeOverrideManager mgr = optionalMgr.get();
        if (volume == null) {
            float currentVolume = mgr.getAudioVolume(id);
            context.getSource().sendSuccess(() -> Component.literal("Current volume is %.2f".formatted(currentVolume * 100f)), false);
        } else {
            if (volume == 100.00f) {
                // will remove volume from json, to keep json file smaller
                mgr.setAudioVolume(id,null);
            }
            mgr.setAudioVolume(id,volume / 100f);
            AudioPlayer.AUDIO_CACHE.remove(id);
            context.getSource().sendSuccess(() -> Component.literal("Set volume, this will apply next time the sound plays"), false);
        }
    }

    @Command("id")
    public void id(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CustomSound customSound = getHeldSound(context);
        if (customSound == null) {
            return;
        }
        context.getSource().sendSuccess(() -> UploadCommands.sendUUIDMessage(customSound.getSoundId(), Component.literal("Successfully extracted sound ID.")), false);
    }

    @Command("name")
    public void name(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CustomSound customSound = getHeldSound(context);
        if (customSound == null) {
            return;
        }
        Optional<FileNameManager> optionalMgr = FileNameManager.instance();

        if (optionalMgr.isEmpty()) {
            context.getSource().sendFailure(Component.literal("An internal error occurred"));
            return;
        }

        FileNameManager mgr = optionalMgr.get();
        String fileName = mgr.getFileName(customSound.getSoundId());
        if (fileName == null) {
            context.getSource().sendFailure(Component.literal("Custom audio does not have an associated file name"));
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal("Audio file name: ").append(Component.literal(fileName).withStyle(style -> {
            return style
                    .withColor(ChatFormatting.GREEN)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, fileName));
        })), false);
    }

    private static CustomSound getHeldSound(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);

        if (playerType == null) {
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return null;
        }

        CustomSound customSound = CustomSound.of(itemInHand);
        if (customSound == null) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return null;
        }

        return customSound;
    }

}
