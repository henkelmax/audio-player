package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.*;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Command("audioplayer")
public class ApplyCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("musicdisc")
    public void musicDisc(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, sound, itemStack -> itemStack.getItem() instanceof RecordItem, "Music Disc", customName, AudioPlayer.SERVER_CONFIG.maxMusicDiscRange, range, false);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("musicdisc_announcer")
    public void musicDiscAnnouncer(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, sound, itemStack -> itemStack.getItem() instanceof RecordItem, "Music Disc", customName, AudioPlayer.SERVER_CONFIG.maxMusicDiscRange, range, true);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("goathorn")
    public void goatHorn(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        apply(context, sound, itemStack -> itemStack.getItem() instanceof InstrumentItem, "Goat Horn", customName, AudioPlayer.SERVER_CONFIG.maxGoatHornRange, range, false);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("musicdisc_bulk")
    public void musicDiscBulk(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        applyBulk(context, sound, itemStack -> itemStack.getItem() instanceof RecordItem, itemStack -> itemStack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock, "Music Disc", customName, AudioPlayer.SERVER_CONFIG.maxMusicDiscRange, range);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("goathorn_bulk")
    public void goatHornBulk(CommandContext<CommandSourceStack> context, @Name("sound") UUID sound, @OptionalArgument @Name("range") @Min("1") Float range, @OptionalArgument @Name("custom_name") String customName) throws CommandSyntaxException {
        applyBulk(context, sound, itemStack -> itemStack.getItem() instanceof InstrumentItem, itemStack -> itemStack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock, "Goat Horn", customName, AudioPlayer.SERVER_CONFIG.maxGoatHornRange, range);
    }

    @RequiresPermission("audioplayer.apply")
    @Command("set_announcer")
    public void setAnnouncer(CommandContext<CommandSourceStack> context, @Name("enabled") Optional<Boolean> enabled) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(itemInHand.getItem() instanceof RecordItem)) {
            context.getSource().sendFailure(Component.literal("Invalid item"));
            return;
        }
        if (!itemInHand.hasTag()) {
            context.getSource().sendFailure(Component.literal("Item does not contain NBT data"));
            return;
        }
        CompoundTag tag = itemInHand.getTag();

        if (tag == null) {
            return;
        }

        if (!tag.contains("CustomSound")) {
            context.getSource().sendFailure(Component.literal("Item does not have custom audio"));
            return;
        }

        tag.putBoolean("IsStaticCustomSound", enabled.orElse(true));

        context.getSource().sendSuccess(Component.literal("Set announcer " + (enabled.orElse(true) ? "enabled" : "disabled")), false);
    }

    private static void apply(CommandContext<CommandSourceStack> context, UUID sound, Predicate<ItemStack> validator, String itemTypeName, @Nullable String customName, ConfigEntry<Float> maxRange, @Nullable Float range, boolean isStatic) throws CommandSyntaxException {
        checkRange(context, maxRange, range);

        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (validator.test(itemInHand)) {
            renameItem(context, itemInHand, sound, customName, range, isStatic);
        } else {
            context.getSource().sendFailure(Component.literal("You don't have a %s in your main hand".formatted(itemTypeName)));
        }
    }

    private static void applyBulk(CommandContext<CommandSourceStack> context, UUID sound, Predicate<ItemStack> itemValidator, Predicate<ItemStack> containerValidator, String itemTypeName, @Nullable String customName, ConfigEntry<Float> maxRange, @Nullable Float range) throws CommandSyntaxException {
        checkRange(context, maxRange, range);

        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (containerValidator.test(itemInHand)) {
            processShulker(context, itemInHand, itemValidator, itemTypeName, sound, customName, maxRange, range);
        } else {
            context.getSource().sendFailure(Component.literal("You don't have a %s in your main hand".formatted(itemTypeName)));
        }
    }

    private static void processShulker(CommandContext<CommandSourceStack> context, ItemStack itemInHand, Predicate<ItemStack> itemValidator, String itemTypeName, UUID soundID, @Nullable String name, ConfigEntry<Float> maxRange, @Nullable Float range) {
        ListTag shulkerContents = itemInHand.getOrCreateTagElement("BlockEntityTag").getList(ShulkerBoxBlockEntity.ITEMS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < shulkerContents.size(); i++) {
            CompoundTag currentItem = shulkerContents.getCompound(i);
            ItemStack itemStack = ItemStack.of(currentItem);
            if (itemValidator.test(itemStack)) {
                renameItem(context, itemStack, soundID, name, range, false);
                currentItem.put("tag", itemStack.getOrCreateTag());
            }
        }
        itemInHand.getOrCreateTagElement("BlockEntityTag").put(ShulkerBoxBlockEntity.ITEMS_TAG, shulkerContents);
        context.getSource().sendSuccess(Component.literal("Successfully updated %s contents".formatted(itemTypeName)), false);
    }

    private static void renameItem(CommandContext<CommandSourceStack> context, ItemStack stack, UUID soundID, @Nullable String name, @Nullable Float range, boolean isStatic) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.putUUID("CustomSound", soundID);

        if (range != null) {
            tag.putFloat("CustomSoundRange", range);
        }

        if (isStatic) {
            tag.putBoolean("IsStaticCustomSound", true);
        }

        if (tag.contains("instrument", Tag.TAG_STRING)) {
            tag.putString("instrument", "");
        }

        ListTag lore = new ListTag();
        if (name != null) {
            lore.add(0, StringTag.valueOf(Component.Serializer.toJson(Component.literal(name).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY))));
        }

        CompoundTag display = new CompoundTag();
        display.put(ItemStack.TAG_LORE, lore);
        tag.put(ItemStack.TAG_DISPLAY, display);

        tag.putInt("HideFlags", ItemStack.TooltipPart.ADDITIONAL.getMask());

        context.getSource().sendSuccess(Component.literal("Successfully updated ").append(stack.getHoverName()), false);
    }

    private static void checkRange(CommandContext<CommandSourceStack> context, ConfigEntry<Float> maxRange, @Nullable Float range) throws CommandSyntaxException {
        if (range == null) {
            return;
        }
        if (range > maxRange.get()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().create(range, maxRange.get());
        }
    }

}
