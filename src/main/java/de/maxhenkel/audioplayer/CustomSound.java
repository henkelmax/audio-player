package de.maxhenkel.audioplayer;

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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CustomSound {

    public static final String CUSTOM_SOUND = "CustomSound";
    public static final String CUSTOM_SOUND_RANDOM = "CustomSoundRandomized";
    public static final String CUSTOM_SOUND_RANGE = "CustomSoundRange";
    public static final String CUSTOM_SOUND_STATIC = "IsStaticCustomSound";
    private static final String ID = "id";

    public static final String DEFAULT_HEAD_LORE = "Has custom audio";

    protected UUID soundId;
    protected ArrayList<UUID> randomSounds;
    @Nullable
    protected Float range;
    protected boolean staticSound;

    public CustomSound(UUID soundId, @Nullable Float range, @Nullable ArrayList<UUID> randomSounds, boolean staticSound) {
        this.soundId = soundId;
        this.range = range;
        this.staticSound = staticSound;
        this.randomSounds = randomSounds;
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
        ArrayList<UUID> randomSounds = null;
        if (tag.contains(CUSTOM_SOUND_RANDOM)) {
            randomSounds = readUUIDArrayFromNbt(tag, CUSTOM_SOUND_RANDOM);
        }
        Float range = tag.getFloat(CUSTOM_SOUND_RANGE).orElse(null);
        boolean staticSound = tag.getBoolean(CUSTOM_SOUND_STATIC).orElse(false);
        return new CustomSound(soundId, range, randomSounds, staticSound);
    }

    public UUID getSoundId() {
        if (isRandomized()) {
            return randomSounds.get(ThreadLocalRandom.current().nextInt(randomSounds.size()));
        }
        return soundId;
    }

    public boolean isRandomized() {
        return randomSounds != null && !randomSounds.isEmpty();
    }

    public ArrayList<UUID> getRandomSounds() {
        return randomSounds;
    }

    public void addRandomSound(UUID id) {
        setRandomization(true);
        randomSounds.add(id);
    }

    public void setRandomization(boolean enabled) {
        if (enabled) {
            if (randomSounds == null) {
                randomSounds = new ArrayList<>();
                randomSounds.add(soundId);
            }
        } else {
            randomSounds = null;
        }
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
            tag.store(CUSTOM_SOUND, UUIDUtil.CODEC, soundId);
        } else {
            tag.remove(CUSTOM_SOUND);
        }
        if (randomSounds != null) {
            saveUUIDArrayToNbt(tag, CUSTOM_SOUND_RANDOM, randomSounds);
        } else {
            tag.remove(CUSTOM_SOUND_RANDOM);
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

    public CustomSound asStatic(boolean staticSound) {
        return new CustomSound(soundId, range, randomSounds, staticSound);
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
        tag.remove(CUSTOM_SOUND_RANDOM);
        tag.remove(CUSTOM_SOUND_RANGE);
        tag.remove(CUSTOM_SOUND_STATIC);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (stack.getItem() instanceof BlockItem) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData == null) {
                return true;
            }
            CompoundTag blockEntityTag = blockEntityData.copyTag();
            blockEntityTag.remove(CUSTOM_SOUND);
            blockEntityTag.remove(CUSTOM_SOUND_RANDOM);
            blockEntityTag.remove(CUSTOM_SOUND_RANGE);
            blockEntityTag.remove(CUSTOM_SOUND_STATIC);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        }
        return true;
    }

}
