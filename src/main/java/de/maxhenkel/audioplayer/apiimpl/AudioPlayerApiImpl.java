package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.AudioPlayerApi;
import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.AudioData;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.importer.AudioImporter;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioplayback.PlayerManager;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
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
import java.util.Map;
import java.util.Optional;
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
    public MutableComponent createInfoMessage(UUID audioID) {
        return ChatUtils.createInfoMessage(audioID);
    }

    @Override
    public void invalidateCachedAudio(UUID audioID) {
        AudioStorageManager.audioCache().invalidate(audioID);
    }

    @Override
    public void clearAudioCache() {
        AudioStorageManager.audioCache().clear();
    }

    @Override
    public void notifyPlayerToEnableVoicechat(ServerPlayer player) {
        ChatUtils.notifyToEnableVoicechatIfNoVoicechat(player);
    }

    @Override
    public boolean canPlayerHearVoicechatAudio(ServerPlayer player) {
        return ChatUtils.isAbleToHearVoicechat(player);
    }

    @Override
    @Nullable
    public VoicechatServerApi getVoicechatServerApi() {
        return VoicechatAudioPlayerPlugin.voicechatServerApi;
    }

    @Override
    public ChannelReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID audioId, @Nullable ServerPlayer p, float distance, @Nullable String category) {
        return PlayerManager.instance().playLocational(level, pos, audioId, p, distance, category, null);
    }

    @Override
    public <T extends AudioChannel> ChannelReference<T> playChannel(T channel, UUID audioId, @Nullable ServerPlayer p) {
        return PlayerManager.instance().playChannel(channel, audioId, p, null);
    }

    @Override
    public Optional<AudioData> getAudioData(ItemStack stack) {
        return Optional.ofNullable(de.maxhenkel.audioplayer.audioloader.AudioData.of(stack));
    }

}
