package de.maxhenkel.audioplayer.api;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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

    void clearAudioCache();

    //TODO Modify arguments
    ChannelReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID audioId, @Nullable ServerPlayer p, float distance, @Nullable String category, int maxLengthSeconds);

    //<T extends AudioChannel> ChannelReference<T> playChannel(T channel, UUID audioId);

}
