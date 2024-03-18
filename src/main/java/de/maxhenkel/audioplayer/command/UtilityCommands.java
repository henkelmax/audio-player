package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.FileNameManager;
import de.maxhenkel.audioplayer.PlayerType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

import java.util.Optional;

import java.util.Optional;

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

        if (!CustomSound.clearItem(itemInHand)) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }

        if (itemInHand.has(DataComponents.INSTRUMENT)) {
            Optional<Holder.Reference<Instrument>> holder = BuiltInRegistries.INSTRUMENT.getHolder(Instruments.PONDER_GOAT_HORN);
            holder.ifPresent(instrumentReference -> itemInHand.set(DataComponents.INSTRUMENT, instrumentReference));
        }

        if (itemInHand.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            itemInHand.remove(DataComponents.HIDE_ADDITIONAL_TOOLTIP);
        }

        if (itemInHand.has(DataComponents.LORE)) {
            itemInHand.remove(DataComponents.LORE);
        }

        context.getSource().sendSuccess(() -> Component.literal("Successfully cleared item"), false);
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
