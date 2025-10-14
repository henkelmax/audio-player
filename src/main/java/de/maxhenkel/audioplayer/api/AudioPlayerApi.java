package de.maxhenkel.audioplayer.api;

import de.maxhenkel.audioplayer.api.data.AudioData;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.apiimpl.AudioPlayerApiImpl;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface AudioPlayerApi {

    static AudioPlayerApi instance() {
        return AudioPlayerApiImpl.INSTANCE;
    }

    <T extends AudioDataModule> ModuleKey<T> registerModuleType(ResourceLocation id, Supplier<T> constructor);

    void importAudio(AudioImporter importer, @Nullable ServerPlayer player);

    MutableComponent createApplyMessage(UUID audioID, MutableComponent component);

    MutableComponent createInfoMessage(UUID audioID);

    void invalidateCachedAudio(UUID audioID);

    void clearAudioCache();

    /**
     * Sends a message to the player to enable voicechat in case they have it disabled
     *
     * @param player The player to send the message to
     */
    void notifyPlayerToEnableVoicechat(ServerPlayer player);

    boolean canPlayerHearVoicechatAudio(ServerPlayer player);

    @Nullable
    VoicechatServerApi getVoicechatServerApi();

    @Nullable
    ChannelReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID audioId, @Nullable ServerPlayer p, float distance, @Nullable String category);

    <T extends AudioChannel> ChannelReference<T> playChannel(T channel, UUID audioId, @Nullable ServerPlayer p);

    Optional<AudioData> getAudioData(ItemStack stack);

}
