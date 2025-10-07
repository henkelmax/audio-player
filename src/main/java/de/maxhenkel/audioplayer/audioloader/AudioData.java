package de.maxhenkel.audioplayer.audioloader;

import com.google.gson.*;
import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioplayback.PlayerType;
import de.maxhenkel.audioplayer.api.AudioPlayerModule;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.api.events.ItemEvents;
import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;
import de.maxhenkel.audioplayer.apiimpl.events.ApplyEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.ClearEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.GetSoundIdEventImpl;
import de.maxhenkel.audioplayer.utils.ComponentUtils;
import de.maxhenkel.audioplayer.utils.upgrade.ItemUpgrader;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AudioData implements de.maxhenkel.audioplayer.api.data.AudioData {

    public static final String AUDIOPLAYER_CUSTOM_DATA = "audioplayer";

    public static final String DEFAULT_HEAD_LORE = "Has custom audio";

    public static final Gson GSON = new GsonBuilder().create();

    protected Map<ModuleKey<? extends AudioDataModule>, AudioDataModule> modules;
    protected Map<ResourceLocation, JsonObject> unknownModules;

    protected AudioData() {
        this.modules = new ConcurrentHashMap<>();
        this.unknownModules = new ConcurrentHashMap<>();
    }

    @Nullable
    private static AudioData fromJson(JsonObject rawData) {
        AudioData data = new AudioData();
        for (Map.Entry<String, JsonElement> entry : rawData.entrySet()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(entry.getKey());
            if (resourceLocation == null) {
                AudioPlayerMod.LOGGER.warn("Invalid module key: {}", entry.getKey());
                continue;
            }
            JsonElement jsonElement = entry.getValue();
            if (!jsonElement.isJsonObject()) {
                AudioPlayerMod.LOGGER.warn("Invalid content for module: {}", entry.getKey());
                continue;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            ModuleKey<? extends AudioDataModule> moduleKey = AudioPlayerApiImpl.INSTANCE.getModuleType(resourceLocation);
            if (moduleKey == null) {
                data.unknownModules.put(resourceLocation, jsonObject);
                AudioPlayerMod.LOGGER.debug("Unknown module: {}", resourceLocation);
            } else {
                AudioDataModule module = moduleKey.create();
                try {
                    module.load(jsonObject);
                    data.modules.put(moduleKey, module);
                } catch (Exception e) {
                    data.unknownModules.put(resourceLocation, jsonObject);
                    AudioPlayerMod.LOGGER.error("Failed to load module {}", resourceLocation, e);
                }
            }
        }
        if (!data.modules.containsKey(AudioPlayerModule.KEY)) {
            AudioPlayerMod.LOGGER.error("Missing audio player module");
            return null;
        }
        return data;
    }

    @Nullable
    public static AudioData of(ItemStack item) {
        if (ItemUpgrader.upgradeItem(item)) {
            AudioPlayerMod.LOGGER.info("Upgraded audio player data of item {}", item.getHoverName().getString());
        }
        CustomData customData = item.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        return of(customData.copyTag().getStringOr(AUDIOPLAYER_CUSTOM_DATA, null));
    }

    @Nullable
    public static AudioData of(ValueInput valueInput) {
        AudioData upgradeData = ItemUpgrader.upgradeBlockEntity(valueInput);
        if (upgradeData != null) {
            AudioPlayerMod.LOGGER.info("Upgraded audio player data {}", upgradeData.getActualSoundId());
            return upgradeData;
        }

        return of(valueInput.getStringOr(AUDIOPLAYER_CUSTOM_DATA, null));
    }

    @Nullable
    public static AudioData of(@Nullable String data) {
        if (data == null) {
            return null;
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(data);
            if (!jsonElement.isJsonObject()) {
                AudioPlayerMod.LOGGER.error("Failed to parse item data - element is not a json object");
                return null;
            }
            return AudioData.fromJson(jsonElement.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            AudioPlayerMod.LOGGER.error("Failed to parse item data", e);
            return null;
        }
    }

    public static AudioData withSoundAndRange(UUID soundId, @Nullable Float range) {
        AudioData audioData = new AudioData();
        audioData.modules.put(AudioPlayerModule.KEY, new AudioPlayerModule(soundId, range));
        return audioData;
    }

    @Override
    public <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> id) {
        AudioDataModule module = modules.get(id);
        return Optional.ofNullable((T) module);
    }

    @Override
    public <T extends AudioDataModule> void setModule(ModuleKey<T> moduleKey, T module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        modules.put(moduleKey, module);
    }

    @Nullable
    @Override
    public <T extends AudioDataModule> T removeModule(ModuleKey<T> moduleKey) {
        if (moduleKey == null) {
            return null;
        }
        if (moduleKey.equals(AudioPlayerModule.KEY)) {
            throw new IllegalArgumentException("Can't remove base audioplayer module");
        }
        return (T) modules.remove(moduleKey);
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

    @Override
    public UUID getSoundId() {
        return getModule(AudioPlayerModule.KEY).map(AudioPlayerModule::getSoundId).orElseThrow();
    }

    @Nullable
    @Override
    public Float getRange() {
        return getModule(AudioPlayerModule.KEY).map(AudioPlayerModule::getRange).orElse(null);
    }

    public Optional<Float> getOptionalRange() {
        return getModule(AudioPlayerModule.KEY).flatMap(m -> Optional.ofNullable(m.getRange()));
    }

    public float getRange(PlayerType playerType) {
        return getRangeOrDefault(playerType.getDefaultRange(), playerType.getMaxRange());
    }

    public float getRangeOrDefault(ConfigEntry<Float> defaultRange, ConfigEntry<Float> maxRange) {
        float range = getOptionalRange().orElseGet(defaultRange::get);
        if (range > maxRange.get()) {
            return maxRange.get();
        } else {
            return range;
        }
    }

    private JsonObject toJson() {
        JsonObject rawData = new JsonObject();
        for (Map.Entry<ResourceLocation, JsonObject> entry : unknownModules.entrySet()) {
            rawData.add(entry.toString(), entry.getValue());
        }
        for (Map.Entry<ModuleKey<? extends AudioDataModule>, AudioDataModule> entry : modules.entrySet()) {
            AudioDataModule module = entry.getValue();
            JsonObject jsonData = new JsonObject();
            try {
                module.save(jsonData);
                rawData.add(entry.getKey().getId().toString(), jsonData);
            } catch (Exception e) {
                AudioPlayerMod.LOGGER.error("Failed to save module {}", entry.getKey().getId(), e);
            }
        }
        return rawData;
    }

    public void saveToNbt(CompoundTag tag) {
        tag.putString(AUDIOPLAYER_CUSTOM_DATA, GSON.toJson(toJson()));
    }

    public void saveToValueOutput(ValueOutput valueOutput) {
        valueOutput.putString(AUDIOPLAYER_CUSTOM_DATA, GSON.toJson(toJson()));
    }

    public void saveToItemIgnoreLore(ItemStack stack) {
        saveToItem(stack, null, false);
    }

    @Override
    public void saveToItem(ItemStack stack) {
        saveToItem(stack, (Component) null);
    }

    public void saveToItem(ItemStack stack, @Nullable String loreString) {
        saveToItem(stack, loreString == null ? null : Component.literal(loreString), true);
    }

    @Override
    public void saveToItem(ItemStack stack, @Nullable Component lore) {
        saveToItem(stack, lore, true);
    }

    public void saveToItem(ItemStack stack, @Nullable Component lore, boolean applyLore) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        saveToNbt(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (stack.has(DataComponents.INSTRUMENT)) {
            stack.set(DataComponents.INSTRUMENT, ComponentUtils.EMPTY_INSTRUMENT);
        }
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            stack.set(DataComponents.JUKEBOX_PLAYABLE, ComponentUtils.CUSTOM_JUKEBOX_PLAYABLE);
        }

        ItemLore l = null;

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SkullBlock) {
            TypedEntityData<? extends BlockEntityType<?>> blockEntityData = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.SKULL, new CompoundTag()));
            CompoundTag blockEntityTag = blockEntityData.copyTagWithoutId();
            saveToNbt(blockEntityTag);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.SKULL, blockEntityTag));
            if (lore == null) {
                l = new ItemLore(Collections.singletonList(Component.literal(DEFAULT_HEAD_LORE).withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY)));
            }
        }
        if (lore != null) {
            l = new ItemLore(Collections.singletonList(lore.copy().withStyle(style -> style.withItalic(false)).withStyle(ChatFormatting.GRAY)));
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

    public static boolean clearItem(MinecraftServer server, ItemStack stack) {
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
            TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData != null) {
                CompoundTag blockEntityTag = blockEntityData.copyTagWithoutId();
                blockEntityTag.remove(AUDIOPLAYER_CUSTOM_DATA);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntityData.type(), blockEntityTag));
            }
        }

        revertMinecraftData(server, stack);

        ItemEvents.CLEAR.invoker().accept(new ClearEventImpl(audioData, stack));
        return true;
    }

    private static void revertMinecraftData(MinecraftServer server, ItemStack stack) {
        if (stack.has(DataComponents.INSTRUMENT)) {
            Optional<Holder.Reference<Instrument>> holder = server.registryAccess().lookupOrThrow(Registries.INSTRUMENT).get(Instruments.PONDER_GOAT_HORN);
            holder.ifPresent(instrumentReference -> stack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrumentReference)));
        }
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            JukeboxPlayable jukeboxPlayable = stack.getItem().components().get(DataComponents.JUKEBOX_PLAYABLE);
            if (jukeboxPlayable != null) {
                stack.set(DataComponents.JUKEBOX_PLAYABLE, jukeboxPlayable);
            } else {
                stack.remove(DataComponents.JUKEBOX_PLAYABLE);
            }
        }

        TooltipDisplay tooltipDisplay = stack.get(DataComponents.TOOLTIP_DISPLAY);
        if (tooltipDisplay != null) {
            LinkedHashSet<DataComponentType<?>> hiddenComponents = new LinkedHashSet<>(tooltipDisplay.hiddenComponents());
            hiddenComponents.remove(DataComponents.JUKEBOX_PLAYABLE);
            hiddenComponents.remove(DataComponents.INSTRUMENT);
            stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(tooltipDisplay.hideTooltip(), hiddenComponents));
        }

        if (stack.has(DataComponents.LORE)) {
            stack.remove(DataComponents.LORE);
        }
    }
}
