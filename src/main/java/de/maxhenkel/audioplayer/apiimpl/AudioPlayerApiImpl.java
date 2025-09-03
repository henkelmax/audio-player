package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.AudioPlayerApi;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class AudioPlayerApiImpl implements AudioPlayerApi {

    public static final AudioPlayerApiImpl INSTANCE = new AudioPlayerApiImpl();

    protected Map<ResourceLocation, ModuleKeyImpl<? extends AudioDataModule>> moduleTypes;

    public AudioPlayerApiImpl() {
        moduleTypes = new ConcurrentHashMap<>();
    }

    public ModuleKey<? extends AudioDataModule> getModuleType(ResourceLocation id) {
        return moduleTypes.get(id);
    }

    @Override
    public <T extends AudioDataModule> ModuleKey<T> registerModuleType(ResourceLocation id, Supplier<T> constructor) {
        ModuleKeyImpl<T> key = new ModuleKeyImpl<>(id, constructor);
        moduleTypes.put(id, key);
        return key;
    }

}
