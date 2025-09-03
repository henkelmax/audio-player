package de.maxhenkel.audioplayer.api;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public interface AudioPlayerApi {

    static AudioPlayerApi instance() {
        return AudioPlayerApiImpl.INSTANCE;
    }

    <T extends AudioDataModule> ModuleKey<T> registerModuleType(ResourceLocation id, Supplier<T> constructor);

    void importAudio(AudioImporter importer, @Nullable ServerPlayer player);

    MutableComponent createApplyMessage(UUID audioID, MutableComponent component);

    void invalidateCachedAudio(UUID audioID);

}
