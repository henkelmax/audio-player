package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.VoicechatAudioPlayerPlugin;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SkullBlock;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public enum PlayerType {

    MUSIC_DISC(
            AudioPlayerMod.SERVER_CONFIG.musicDiscRange,
            AudioPlayerMod.SERVER_CONFIG.maxMusicDiscRange,
            AudioPlayerMod.SERVER_CONFIG.maxMusicDiscDuration,
            VoicechatAudioPlayerPlugin.MUSIC_DISC_CATEGORY,
            itemStack -> itemStack.has(DataComponents.JUKEBOX_PLAYABLE)
    ),
    NOTE_BLOCK(
            AudioPlayerMod.SERVER_CONFIG.noteBlockRange,
            AudioPlayerMod.SERVER_CONFIG.maxNoteBlockRange,
            AudioPlayerMod.SERVER_CONFIG.maxNoteBlockDuration,
            VoicechatAudioPlayerPlugin.NOTE_BLOCK_CATEGORY,
            itemStack -> itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock
    ),
    GOAT_HORN(
            AudioPlayerMod.SERVER_CONFIG.goatHornRange,
            AudioPlayerMod.SERVER_CONFIG.maxGoatHornRange,
            AudioPlayerMod.SERVER_CONFIG.maxGoatHornDuration,
            VoicechatAudioPlayerPlugin.GOAT_HORN_CATEGORY,
            itemStack -> itemStack.getItem() instanceof InstrumentItem
    );

    private final ConfigEntry<Float> defaultRange;
    private final ConfigEntry<Float> maxRange;
    private final ConfigEntry<Integer> maxDuration;
    private final String category;
    private final Predicate<ItemStack> validator;

    PlayerType(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange, ConfigEntry<Integer> maxDuration, String category, Predicate<ItemStack> validator) {
        this.defaultRange = defaultRange;
        this.maxRange = maxRange;
        this.maxDuration = maxDuration;
        this.category = category;
        this.validator = validator;
    }

    public ConfigEntry<Float> getDefaultRange() {
        return defaultRange;
    }

    public ConfigEntry<Float> getMaxRange() {
        return maxRange;
    }

    public ConfigEntry<Integer> getMaxDuration() {
        return maxDuration;
    }

    public String getCategory() {
        return category;
    }

    public Predicate<ItemStack> getValidator() {
        return validator;
    }

    public boolean isValid(ItemStack itemStack) {
        return validator.test(itemStack);
    }

    @Nullable
    public static PlayerType fromItemStack(ItemStack itemStack) {
        for (PlayerType type : values()) {
            if (type.getValidator().test(itemStack)) {
                return type;
            }
        }
        return null;
    }

}
