package de.maxhenkel.audioplayer.audioloader;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.api.AudioPlayerModule;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.api.events.ItemEvents;
import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;
import de.maxhenkel.audioplayer.apiimpl.JsonData;
import de.maxhenkel.audioplayer.apiimpl.events.ApplyEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.ClearEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.GetSoundIdEventImpl;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AudioData {

    public static final String AUDIOPLAYER_CUSTOM_DATA = "audioplayer";


    public static final String DEFAULT_HEAD_LORE = "Has custom audio";

    protected Map<ModuleKey<? extends AudioDataModule>, AudioDataModule> modules;
    protected Map<ResourceLocation, JSONObject> unknownModules;

    protected AudioData() {
        this.modules = new ConcurrentHashMap<>();
        this.unknownModules = new ConcurrentHashMap<>();
    }

    @Nullable
    private static AudioData fromJson(JSONObject rawData) {
        AudioData data = new AudioData();
        for (String key : rawData.keySet()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(key);
            if (resourceLocation == null) {
                AudioPlayerMod.LOGGER.warn("Invalid module key: {}", key);
                continue;
            }
            JSONObject jsonObject = rawData.optJSONObject(key);
            if (jsonObject == null) {
                AudioPlayerMod.LOGGER.warn("Invalid content for module: {}", key);
                continue;
            }
            ModuleKey<? extends AudioDataModule> moduleKey = AudioPlayerApiImpl.INSTANCE.getModuleType(resourceLocation);
            if (moduleKey == null) {
                data.unknownModules.put(resourceLocation, jsonObject);
                //TODO Debug log unknown module
            } else {
                AudioDataModule module = moduleKey.create();
                try {
                    module.load(new JsonData(jsonObject));
                    data.modules.put(moduleKey, module);
                } catch (Exception e) {
                    data.unknownModules.put(resourceLocation, jsonObject);
                    AudioPlayerMod.LOGGER.error("Failed to load module {}", resourceLocation, e);
                }
            }
        }
        if (!data.modules.containsKey(AudioPlayerModule.KEY)) {
            AudioPlayerMod.LOGGER.error("Missing audio player module");
            //TODO Do we want to allow items without an actual ID?
            return null;
        }
        return data;
    }

    @Nullable
    public static AudioData of(ItemStack item) {
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        return of(customData.copyTag());
    }

    @Nullable
    public static AudioData of(CompoundTag tag) {
        return of(tag.getStringOr(AUDIOPLAYER_CUSTOM_DATA, null));
    }

    @Nullable
    public static AudioData of(ValueInput valueInput) {
        return of(valueInput.getStringOr(AUDIOPLAYER_CUSTOM_DATA, null));
    }

    @Nullable
    public static AudioData of(@Nullable String data) {
        if (data == null) {
            return null;
        }
        try {
            return AudioData.fromJson(new JSONObject(data));
        } catch (JSONException e) {
            AudioPlayerMod.LOGGER.error("Failed to parse item data", e);
            return null;
        }
    }

    public static AudioData withSoundAndRange(UUID soundId, @Nullable Float range) {
        AudioData audioData = new AudioData();
        audioData.modules.put(AudioPlayerModule.KEY, new AudioPlayerModule(soundId, range));
        return audioData;
    }

    public <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> id) {
        AudioDataModule module = modules.get(id);
        return Optional.ofNullable((T) module);
    }

    @Nullable
    public UUID getSoundIdToPlay() {
        UUID soundId = getModule(AudioPlayerModule.KEY).map(AudioPlayerModule::getSoundId).orElse(null);
        GetSoundIdEventImpl event = new GetSoundIdEventImpl(this, soundId);
        AudioEvents.GET_SOUND_ID.invoker().accept(event);
        return event.getSoundId();
    }

    @Nullable
    public UUID getActualSoundId() {
        return getModule(AudioPlayerModule.KEY).map(AudioPlayerModule::getSoundId).orElse(null);
    }

    public Optional<Float> getRange() {
        return getModule(AudioPlayerModule.KEY).flatMap(m -> Optional.ofNullable(m.getRange()));
    }

    public float getRange(PlayerType playerType) {
        return getRangeOrDefault(playerType.getDefaultRange(), playerType.getMaxRange());
    }

    public float getRangeOrDefault(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange) {
        float range = getRange().orElseGet(defaultRange::get);
        if (range > maxRange.get()) {
            return maxRange.get();
        } else {
            return range;
        }
    }

    private JSONObject toJson() {
        JSONObject rawData = new JSONObject();
        for (Map.Entry<ResourceLocation, JSONObject> entry : unknownModules.entrySet()) {
            rawData.put(entry.toString(), entry.getValue());
        }
        for (Map.Entry<ModuleKey<? extends AudioDataModule>, AudioDataModule> entry : modules.entrySet()) {
            AudioDataModule module = entry.getValue();
            JsonData jsonData = new JsonData(new JSONObject());
            try {
                module.save(jsonData);
                rawData.put(entry.getKey().getId().toString(), jsonData.getRawData());
            } catch (Exception e) {
                AudioPlayerMod.LOGGER.error("Failed to save module {}", entry.getKey().getId(), e);
            }
        }
        return rawData;
    }

    public void saveToNbt(CompoundTag tag) {
        tag.putString(AUDIOPLAYER_CUSTOM_DATA, toJson().toString());
    }

    public void saveToValueOutput(ValueOutput valueOutput) {
        valueOutput.putString(AUDIOPLAYER_CUSTOM_DATA, toJson().toString());
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
            ResourceLocation skullId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(BlockEntityType.SKULL);
            if (skullId != null) {
                blockEntityTag.putString("id", skullId.toString());
            }
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

        ItemEvents.APPLY.invoker().accept(new ApplyEventImpl(this, stack));
    }

    public static boolean clearItem(ItemStack stack) {
        AudioData audioData = AudioData.of(stack);
        if (audioData == null) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(AUDIOPLAYER_CUSTOM_DATA)) {
            return false;
        }
        tag.remove(AUDIOPLAYER_CUSTOM_DATA);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (stack.getItem() instanceof BlockItem) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null) {
                CompoundTag blockEntityTag = blockEntityData.copyTag();
                blockEntityTag.remove(AUDIOPLAYER_CUSTOM_DATA);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
            }
        }

        ItemEvents.CLEAR.invoker().accept(new ClearEventImpl(audioData, stack));
        return true;
    }

}
