package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Command("audioplayer")
@RequiresPermission(AudioPlayerPermissionManager.APPLY_PERMISSION_STRING)
public class ApplyCommands {

    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("file_name") String fileName, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, AudioData.withSoundAndRange(id, null), customName);
    }

    // The apply commands for UUIDs must be below the ones with file names, so that the file name does not overwrite the UUID argument

    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, AudioData.withSoundAndRange(sound, null), customName);
    }

    @Nullable
    private static UUID getId(CommandContext<CommandSourceStack> context, String fileName) {
        try {
            return UUID.fromString(fileName);
        } catch (Exception ignored) {
        }

        UUID audioId = AudioStorageManager.fileNameManager().getAudioId(fileName);

        if (audioId == null) {
            context.getSource().sendFailure(Component.literal("No audio with name '%s' found or more than one found".formatted(fileName)));
            return null;
        }
        return audioId;
    }

    private static void apply(CommandContext<CommandSourceStack> context, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (isShulkerBox(itemInHand)) {
            applyShulker(context, data, customName);
            return;
        }

        PlayerType type = PlayerType.fromItemStack(itemInHand);
        if (type == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        apply(context, itemInHand, type, data, customName);
    }

    private static void applyShulker(CommandContext<CommandSourceStack> context, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isShulkerBox(itemInHand)) {
            processShulker(context, itemInHand, data, customName);
            return;
        }
        context.getSource().sendFailure(Component.literal("You don't have a shulker box in your main hand"));
    }

    private static void processShulker(CommandContext<CommandSourceStack> context, ItemStack shulkerItem, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        ItemContainerContents contents = shulkerItem.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> shulkerContents = NonNullList.withSize(ShulkerBoxBlockEntity.CONTAINER_SIZE, ItemStack.EMPTY);
        contents.copyInto(shulkerContents);
        for (ItemStack itemStack : shulkerContents) {
            PlayerType playerType = PlayerType.fromItemStack(itemStack);
            if (playerType == null) {
                continue;
            }
            apply(context, itemStack, playerType, data, customName);
        }
        shulkerItem.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerContents));
        context.getSource().sendSuccess(() -> Component.literal("Successfully updated contents"), false);
    }

    private static void apply(CommandContext<CommandSourceStack> context, ItemStack stack, PlayerType type, AudioData data, @Nullable String customName) throws CommandSyntaxException {
        checkRange(type.getMaxRange(), data.getRange().orElse(null));
        if (!type.isValid(stack)) {
            return;
        }
        data.saveToItem(stack, customName);
        context.getSource().sendSuccess(() -> Component.literal("Successfully updated ").append(stack.getHoverName()), false);
    }

    private static void checkRange(ConfigEntry<Float> maxRange, @Nullable Float range) throws CommandSyntaxException {
        if (range == null) {
            return;
        }
        if (range > maxRange.get()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().create(range, maxRange.get());
        }
    }

    public static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock;
    }

    private static void sendInvalidHandItemMessage(CommandContext<CommandSourceStack> context, ItemStack invalidItem) {
        if (invalidItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You don't have an item in your main hand"));
            return;
        }
        context.getSource().sendFailure(Component.literal("The item in your main hand can not have custom audio"));
    }

}
