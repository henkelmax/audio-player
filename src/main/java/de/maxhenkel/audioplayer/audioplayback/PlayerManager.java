package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.api.events.GetDistanceEvent;
import de.maxhenkel.audioplayer.api.events.PlayEvent;
import de.maxhenkel.audioplayer.api.events.PostPlayEvent;
import de.maxhenkel.audioplayer.apiimpl.ChannelReferenceImpl;
import de.maxhenkel.audioplayer.apiimpl.events.GetDistanceEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.PlayEventImpl;
import de.maxhenkel.audioplayer.apiimpl.events.PostPlayEventImpl;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.cache.CachedAudio;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.utils.ChatUtils;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PlayerManager {

    private final Map<UUID, PlayerThread<?>> players;
    private final ExecutorService executor;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
        this.executor = Executors.newFixedThreadPool(AudioPlayerMod.SERVER_CONFIG.audioLoaderThreads.get(), r -> {
            Thread thread = new Thread(r, "AudioLoaderThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Nullable
    public ChannelReference<?> playType(ServerLevel serverLevel, @Nullable ServerPlayer player, AudioData data, PlayerType type, Event<Consumer<PlayEvent>> playEvent, Event<Consumer<PostPlayEvent>> postPlayEvent, Vec3 pos) {
        UUID soundIdToPlay = data.getSoundIdToPlay();
        if (soundIdToPlay == null) {
            return null;
        }
        Float maxDuration = type.getMaxDuration().get();
        if (maxDuration == null || maxDuration < 0F) {
            maxDuration = null;
        }
        GetDistanceEvent distanceEvent = new GetDistanceEventImpl(data, type.getDefaultRange().get(), data.getRange(type), pos);
        AudioEvents.GET_DISTANCE.invoker().accept(distanceEvent);
        PlayEventImpl event = new PlayEventImpl(data, serverLevel, null, soundIdToPlay, type.getDefaultRange().get(), distanceEvent.getDistance(), type.getCategory(), pos);
        playEvent.invoker().accept(event);
        if (event.isCancelled()) {
            return null;
        }
        ChannelReference<?> channel = event.getOverrideChannel();
        if (channel == null) {
            channel = PlayerManager.instance().playLocational(serverLevel, event.getPosition(), event.getSoundId(), player, event.getDistance(), event.getCategory(), maxDuration);
        }
        if (channel != null) {
            postPlayEvent.invoker().accept(new PostPlayEventImpl(channel, data, event.getSoundId(), event.getCategory(), event.getPosition(), serverLevel, player, event.getDistance()));
        }
        return channel;
    }

    @Nullable
    public ChannelReferenceImpl<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID sound, @Nullable ServerPlayer p, float distance, @Nullable String category, @Nullable Float maxLengthSeconds) {
        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) {
            return null;
        }

        UUID channelID = UUID.randomUUID();
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelID, api.fromServerLevel(level), api.createPosition(pos.x, pos.y, pos.z));
        if (channel == null) {
            return null;
        }
        if (category != null) {
            channel.setCategory(category);
        }
        channel.setDistance(distance);
        api.getPlayersInRange(api.fromServerLevel(level), channel.getLocation(), distance + 1F, serverPlayer -> {
            VoicechatConnection connection = api.getConnectionOf(serverPlayer);
            return !ChatUtils.isAbleToHearVoicechat(connection);
        }).stream().map(Player::getPlayer).map(ServerPlayer.class::cast).forEach(ChatUtils::sendEnableVoicechatMessage);

        return playChannel(channel, sound, p, maxLengthSeconds);
    }

    public <T extends AudioChannel> ChannelReferenceImpl<T> playChannel(T channel, UUID sound, @Nullable ServerPlayer p, @Nullable Float maxLengthSeconds) {
        PlayerThread<T> playerThread = new PlayerThread<>(channel, () -> players.remove(channel.getId()));
        players.put(channel.getId(), playerThread);

        executor.execute(() -> {
            if (!playChannel0(playerThread, sound, p, maxLengthSeconds)) {
                players.remove(channel.getId());
            }
        });
        return new ChannelReferenceImpl<>(channel, sound, playerThread);
    }

    private <T extends AudioChannel> boolean playChannel0(PlayerThread<T> playerThread, UUID sound, @Nullable ServerPlayer p, @Nullable Float maxLengthSeconds) {
        try {
            CachedAudio audio = AudioStorageManager.audioCache().getAudio(sound);

            if (maxLengthSeconds != null && audio.getDurationSeconds() > maxLengthSeconds) {
                if (p != null) {
                    p.level().getServer().execute(() -> {
                        p.displayClientMessage(Lang.translatable("audioplayer.audio_too_long").withStyle(ChatFormatting.DARK_RED), true);
                    });
                }
                AudioPlayerMod.LOGGER.error("Audio {} was too long to play", sound);
                return false;
            }

            playerThread.startPlaying(audio);
            return true;
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.level().getServer().execute(() -> {
                    p.displayClientMessage(Lang.translatable("audioplayer.play_audio_failed", e.getMessage()).withStyle(ChatFormatting.DARK_RED), true);
                });
            }
            return false;
        }
    }

    public void stop(UUID channelID) {
        PlayerThread<?> player = players.get(channelID);
        if (player == null) {
            players.remove(channelID);
            return;
        }
        player.stopPlaying();
    }

    public boolean isStopped(UUID channelID) {
        PlayerThread<?> player = players.get(channelID);
        if (player == null) {
            return true;
        }
        return player.isStopped();
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public int stopAll(UUID audioId) {
        List<? extends PlayerThread<?>> list = players.values().stream().filter(playerThread -> {
            CachedAudio audio = playerThread.getAudio();
            if (audio == null) {
                return false;
            }
            return audio.getId().equals(audioId);
        }).toList();
        list.forEach(PlayerThread::stopPlaying);
        return list.size();
    }

}
