package de.maxhenkel.audioplayer;

import com.mojang.serialization.Codec;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.*;

public class CustomSound {

    public static final String CUSTOM_SOUND = "CustomSound";
    public static final String CUSTOM_SOUND_RANGE = "CustomSoundRange";
    private static final String ID = "id";

    public static final String DEFAULT_HEAD_LORE = "Has custom audio";

    protected UUID soundId;
    @Nullable
    protected Float range;

    public CustomSound(UUID soundId, @Nullable Float range) {
        this.soundId = soundId;
        this.range = range;
    }

    @Nullable
    public static CustomSound of(ItemStack item) {
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        return of(customData.copyTag());
    }

    @Nullable
    public static CustomSound of(CompoundTag tag) {
        UUID soundId;
        if (tag.contains(CUSTOM_SOUND)) {
            soundId = tag.read(CUSTOM_SOUND, UUIDUtil.CODEC).orElse(null);
        } else {
            return null;
        }
        Float range = tag.getFloat(CUSTOM_SOUND_RANGE).orElse(null);
        return new CustomSound(soundId, range);
    }

    @Nullable
    public static CustomSound of(ValueInput valueInput) {
        UUID soundId = valueInput.read(CUSTOM_SOUND, UUIDUtil.CODEC).orElse(null);
        if (soundId == null) {
            return null;
        }
        Float range = valueInput.read(CUSTOM_SOUND_RANGE, Codec.FLOAT).orElse(null);
        return new CustomSound(soundId, range);
    }

    public UUID getSoundId() {
        return soundId;
    }

    public Optional<Float> getRange() {
        return Optional.ofNullable(range);
    }

    public float getRange(PlayerType playerType) {
        return getRangeOrDefault(playerType.getDefaultRange(), playerType.getMaxRange());
    }

    public float getRangeOrDefault(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange) {
        if (range == null) {
            return defaultRange.get();
        } else if (range > maxRange.get()) {
            return maxRange.get();
        } else {
            return range;
        }
    }

    public void saveToNbt(CompoundTag tag) {
        if (soundId != null) {
            tag.store(CUSTOM_SOUND, UUIDUtil.CODEC, soundId);
        } else {
            tag.remove(CUSTOM_SOUND);
        }
        if (range != null) {
            tag.putFloat(CUSTOM_SOUND_RANGE, range);
        } else {
            tag.remove(CUSTOM_SOUND_RANGE);
        }
    }

    public void saveToValueOutput(ValueOutput valueOutput) {
        if (soundId != null) {
            valueOutput.store(CUSTOM_SOUND, UUIDUtil.CODEC, soundId);
        } else {
            valueOutput.discard(CUSTOM_SOUND);
        }
        if (range != null) {
            valueOutput.putFloat(CUSTOM_SOUND_RANGE, range);
        } else {
            valueOutput.discard(CUSTOM_SOUND_RANGE);
        }
    }

    public static void saveUUIDArrayToNbt(CompoundTag tag, String id, List<UUID> uuids) {
        ListTag uuidList = new ListTag();
        for (UUID uuid : uuids) {
            uuidList.add(UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, uuid).getOrThrow());
        }
        tag.put(id, uuidList);
    }

    public static ArrayList<UUID> readUUIDArrayFromNbt(CompoundTag tag, String id) {
        ListTag list = tag.getList(id).orElse(new ListTag());
        ArrayList<UUID> uuidList = new ArrayList<>(list.size());
        for (Tag value : list) {
            uuidList.add(UUIDUtil.CODEC.decode(NbtOps.INSTANCE, value).getOrThrow().getFirst());
        }
        return uuidList;
    }

    public void saveToItemIgnoreLore(ItemStack stack) {
        saveToItem(stack, null, false);
    }

    public void saveToItem(ItemStack stack) {
        saveToItem(stack, null);
    }

    public void saveToItem(ItemStack stack, @Nullable String loreString) {
        saveToItem(stack, loreString, true);
    }

    public void saveToItem(ItemStack stack, @Nullable String loreString, boolean applyLore) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        saveToNbt(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        ItemLore l = null;

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
            CustomData blockEntityData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            CompoundTag blockEntityTag = blockEntityData.copyTag();
            saveToNbt(blockEntityTag);
            blockEntityTag.putString(ID, BlockEntityType.SKULL.builtInRegistryHolder().key().location().toString());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
            if (loreString == null) {
                l = new ItemLore(Collections.singletonList(Component.literal(DEFAULT_HEAD_LORE).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY)));
            }
        }
        if (loreString != null) {
            l = new ItemLore(Collections.singletonList(Component.literal(loreString).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY)));
        }

        if (applyLore) {
            if (l != null) {
                stack.set(DataComponents.LORE, l);
            } else {
                stack.remove(DataComponents.LORE);
            }
        }

        TooltipDisplay tooltipDisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        LinkedHashSet<DataComponentType<?>> hiddenComponents = new LinkedHashSet<>(tooltipDisplay.hiddenComponents());
        hiddenComponents.add(DataComponents.JUKEBOX_PLAYABLE);
        hiddenComponents.add(DataComponents.INSTRUMENT);
        stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(tooltipDisplay.hideTooltip(), hiddenComponents));
    }

    public static boolean clearItem(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(CUSTOM_SOUND)) {
            return false;
        }
        tag.remove(CUSTOM_SOUND);
        tag.remove(CUSTOM_SOUND_RANGE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (stack.getItem() instanceof BlockItem) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData == null) {
                return true;
            }
            CompoundTag blockEntityTag = blockEntityData.copyTag();
            blockEntityTag.remove(CUSTOM_SOUND);
            blockEntityTag.remove(CUSTOM_SOUND_RANGE);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        }
        return true;
    }

}
