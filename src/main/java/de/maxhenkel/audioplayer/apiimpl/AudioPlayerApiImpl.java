package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.AudioPlayerApi;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
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

    @Override
    public void importAudio(AudioImporter importer, @Nullable ServerPlayer player) {
        AudioStorageManager.instance().handleImport(importer, player);
    }

    @Override
    public MutableComponent createApplyMessage(UUID audioID, MutableComponent component) {
        return ChatUtils.createApplyMessage(audioID, component);
    }

    @Override
    public void invalidateCachedAudio(UUID audioID) {
        AudioStorageManager.audioCache().invalidate(audioID);
    }

}
