package de.maxhenkel.audioplayer.utils.upgrade;

import com.mojang.serialization.Codec;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;

import javax.annotation.Nullable;
import java.util.UUID;

public class ItemUpgrader {

    public static final String CUSTOM_SOUND = "CustomSound";
    public static final String CUSTOM_SOUND_RANGE = "CustomSoundRange";
    public static final String CUSTOM_SOUND_RANDOM = "CustomSoundRandomized";
    public static final String CUSTOM_SOUND_STATIC = "IsStaticCustomSound";

    public static boolean upgradeItem(ItemStack item) {
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag tag = customData.copyTag();
        AudioData data = of(tag);
        if (data == null) {
            return false;
        }
        tag.remove(CUSTOM_SOUND);
        tag.remove(CUSTOM_SOUND_RANGE);
        tag.remove(CUSTOM_SOUND_STATIC);
        tag.remove(CUSTOM_SOUND_RANDOM);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        data.saveToItem(item);
        return true;
    }

    public static AudioData upgradeBlockEntity(ValueInput valueInput) {
        return of(valueInput);
    }

    @Nullable
    private static AudioData of(CompoundTag tag) {
        UUID soundId = tag.read(CUSTOM_SOUND, UUIDUtil.CODEC).orElse(null);
        if (soundId == null) {
            return null;
        }

        //TODO Upgrade random sounds and static sound
        //List<UUID> randomSounds = tag.read(CUSTOM_SOUND_RANDOM, UUID_LIST_CODEC).orElse(null);
        //boolean staticSound = tag.getBoolean(CUSTOM_SOUND_STATIC).orElse(false);

        Float range = tag.getFloat(CUSTOM_SOUND_RANGE).orElse(null);
        return AudioData.withSoundAndRange(soundId, range);
    }

    @Nullable
    private static AudioData of(ValueInput valueInput) {
        UUID soundId = valueInput.read(CUSTOM_SOUND, UUIDUtil.CODEC).orElse(null);
        if (soundId == null) {
            return null;
        }

        //TODO Upgrade random sounds and static sound
        //List<UUID> randomSounds = valueInput.read(CUSTOM_SOUND_RANDOM, UUID_LIST_CODEC).orElse(null);
        //boolean staticSound = valueInput.read(CUSTOM_SOUND_STATIC, Codec.BOOL).orElse(false);

        Float range = valueInput.read(CUSTOM_SOUND_RANGE, Codec.FLOAT).orElse(null);
        return AudioData.withSoundAndRange(soundId, range);
    }

}
