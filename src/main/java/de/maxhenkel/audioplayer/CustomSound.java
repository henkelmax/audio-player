package de.maxhenkel.audioplayer;

import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class CustomSound {

    public static final String CUSTOM_SOUND = "CustomSound";
    public static final String CUSTOM_SOUND_RANGE = "CustomSoundRange";
    public static final String CUSTOM_SOUND_STATIC = "IsStaticCustomSound";

    protected UUID soundId;
    @Nullable
    protected Float range;
    protected boolean staticSound;

    public CustomSound(UUID soundId, @Nullable Float range, boolean staticSound) {
        this.soundId = soundId;
        this.range = range;
        this.staticSound = staticSound;
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
            soundId = tag.getUUID(CUSTOM_SOUND);
        } else {
            return null;
        }
        Float range = null;
        if (tag.contains(CUSTOM_SOUND_RANGE)) {
            range = tag.getFloat(CUSTOM_SOUND_RANGE);
        }
        boolean staticSound = false;
        if (tag.contains(CUSTOM_SOUND_STATIC)) {
            staticSound = tag.getBoolean(CUSTOM_SOUND_STATIC);
        }
        return new CustomSound(soundId, range, staticSound);
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

    public boolean isStaticSound() {
        return staticSound;
    }

    public void saveToNbt(CompoundTag tag) {
        if (soundId != null) {
            tag.putUUID(CUSTOM_SOUND, soundId);
        } else {
            tag.remove(CUSTOM_SOUND);
        }
        if (range != null) {
            tag.putFloat(CUSTOM_SOUND_RANGE, range);
        } else {
            tag.remove(CUSTOM_SOUND_RANGE);
        }
        if (staticSound) {
            tag.putBoolean(CUSTOM_SOUND_STATIC, true);
        } else {
            tag.remove(CUSTOM_SOUND_STATIC);
        }
    }

    public void saveToItem(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        saveToNbt(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (stack.getItem() instanceof BlockItem) {
            CustomData blockEntityData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            CompoundTag blockEntityTag = blockEntityData.copyTag();
            saveToNbt(blockEntityTag);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        }
    }

    public CustomSound asStatic(boolean staticSound) {
        return new CustomSound(soundId, range, staticSound);
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
        tag.remove(CUSTOM_SOUND_STATIC);
        if (stack.getItem() instanceof BlockItem) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData == null) {
                return true;
            }
            CompoundTag blockEntityTag = blockEntityData.copyTag();
            blockEntityTag.remove(CUSTOM_SOUND);
            blockEntityTag.remove(CUSTOM_SOUND_RANGE);
            blockEntityTag.remove(CUSTOM_SOUND_STATIC);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        }
        return true;
    }

}
