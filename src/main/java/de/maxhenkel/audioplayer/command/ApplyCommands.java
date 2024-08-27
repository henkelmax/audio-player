package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.FileNameManager;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Command("audioplayer")
public class ApplyCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("file_name") String fileName, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, new CustomSound(id, range, false), customName);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    public void apply(CommandContext<CommandSourceStack> context, @Name("file_name") String fileName, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        UUID id = getId(context, fileName);
        if (id == null) {
            return;
        }
        apply(context, new CustomSound(id, null, false), customName);
    }

    // The apply commands for UUIDs must be below the ones with file names, so that the file name does not overwrite the UUID argument

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    @Command("musicdisc")
    @Command("goathorn")
    public void apply(CommandContext<CommandSourceStack> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, new CustomSound(sound, range, false), customName);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("apply")
    @Command("musicdisc")
    @Command("goathorn")
    public void apply(CommandContext<CommandSourceStack> context, @Name("sound_id") UUID sound, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, new CustomSound(sound, null, false), customName);
    }

    @Nullable
    private static UUID getId(CommandContext<CommandSourceStack> context, String fileName) {
        try {
            return UUID.fromString(fileName);
        } catch (Exception ignored) {
        }

        Optional<FileNameManager> optionalFileNameManager = FileNameManager.instance();
        if (optionalFileNameManager.isEmpty()) {
            context.getSource().sendFailure(Component.literal("An internal error occurred"));
            return null;
        }

        FileNameManager fileNameManager = optionalFileNameManager.get();
        UUID audioId = fileNameManager.getAudioId(fileName);

        if (audioId == null) {
            context.getSource().sendFailure(Component.literal("No audio with name '%s' found or more than one found".formatted(fileName)));
            return null;
        }
        return audioId;
    }

    private static void apply(CommandContext<CommandSourceStack> context, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (isShulkerBox(itemInHand)) {
            applyShulker(context, sound, customName);
            return;
        }

        PlayerType type = PlayerType.fromItemStack(itemInHand);
        if (type == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        apply(context, itemInHand, type, sound, customName);
    }

    @RequiresPermission("audioplayer.set_static")
    @Command("setstatic")
    public void setStatic(CommandContext<CommandSourceStack> context, @Name("enabled") Optional<Boolean> enabled) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        PlayerType playerType = PlayerType.fromItemStack(itemInHand);

        if (playerType == null) {
            sendInvalidHandItemMessage(context, itemInHand);
            return;
        }
        CustomSound customSound = CustomSound.of(itemInHand);
        if (customSound == null) {
            context.getSource().sendFailure(Component.literal("This item does not have custom audio"));
            return;
        }

        CustomSound newSound = customSound.asStatic(enabled.orElse(true));
        newSound.saveToItemIgnoreLore(itemInHand);

        context.getSource().sendSuccess(() -> Component.literal((enabled.orElse(true) ? "Enabled" : "Disabled") + " static audio"), false);
    }

    private static void applyShulker(CommandContext<CommandSourceStack> context, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (isShulkerBox(itemInHand)) {
            processShulker(context, itemInHand, sound, customName);
            return;
        }
        context.getSource().sendFailure(Component.literal("You don't have a shulker box in your main hand"));
    }

    private static void processShulker(CommandContext<CommandSourceStack> context, ItemStack shulkerItem, CustomSound sound, @Nullable String customName) throws CommandSyntaxException {
        ListTag shulkerContents = shulkerItem.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG).getList(ShulkerBoxBlockEntity.ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < shulkerContents.size(); i++) {
            CompoundTag currentItem = shulkerContents.getCompound(i);
            ItemStack itemStack = ItemStack.of(currentItem);
            PlayerType playerType = PlayerType.fromItemStack(itemStack);
            if (playerType == null) {
                continue;
            }
            apply(context, itemStack, playerType, sound, customName);
            currentItem.put("tag", itemStack.getOrCreateTag());
        }
        context.getSource().sendSuccess(() -> Component.literal("Successfully updated contents"), false);
    }

    private static void apply(CommandContext<CommandSourceStack> context, ItemStack stack, PlayerType type, CustomSound customSound, @Nullable String customName) throws CommandSyntaxException {
        checkRange(type.getMaxRange(), customSound.getRange().orElse(null));
        if (!type.isValid(stack)) {
            return;
        }
        customSound.saveToItem(stack, customName);
        CompoundTag tag = stack.getOrCreateTag();

        if (stack.getItem() instanceof InstrumentItem) {
            tag.putString("instrument", "");
        } else {
            tag.remove("instrument");
        }

        if (stack.getItem() instanceof BlockItem) {
            CompoundTag blockEntityTag = stack.getOrCreateTagElement(BlockItem.BLOCK_ENTITY_TAG);
            customSound.saveToNbt(blockEntityTag);
        }

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
