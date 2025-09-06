package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.apiimpl.ChannelReferenceImpl;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerManager {

    private final Map<UUID, ChannelReferenceImpl<?>> players;
    private final ExecutorService executor;

    public PlayerManager() {
        this.players = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "AudioPlayerThread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Nullable
    public ChannelReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, PlayerType type, AudioData sound, @Nullable ServerPlayer player) {
        UUID soundIdToPlay = sound.getSoundIdToPlay();
        if (soundIdToPlay == null) {
            return null;
        }
        return playLocational(
                level,
                pos,
                soundIdToPlay,
                player,
                sound.getRange(type),
                type.getCategory(),
                type.getMaxDuration().get()
        );
    }

    @Nullable
    public ChannelReferenceImpl<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID sound, @Nullable ServerPlayer p, float distance, @Nullable String category, @Nullable Integer maxLengthSeconds) {
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

    public <T extends AudioChannel> ChannelReferenceImpl<T> playChannel(T channel, UUID sound, @Nullable ServerPlayer p, @Nullable Integer maxLengthSeconds) {
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<PlayerThread<T>> player = new AtomicReference<>();

        ChannelReferenceImpl<T> playerReference = new ChannelReferenceImpl<>(channel, sound, player, () -> {
            synchronized (stopped) {
                stopped.set(true);
                PlayerThread<T> audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
                players.remove(channel.getId());
            }
        });
        players.put(channel.getId(), playerReference);

        executor.execute(() -> {
            PlayerThread<T> playerThread = playChannel0(channel, sound, p, maxLengthSeconds);
            if (playerThread == null) {
                players.remove(channel.getId());
                return;
            }
            playerThread.setOnStopped(() -> {
                players.remove(channel.getId());
            });
            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(playerThread);
                } else {
                    playerThread.stopPlaying();
                }
            }
        });
        return playerReference;
    }

    @Nullable
    private <T extends AudioChannel> PlayerThread<T> playChannel0(T channel, UUID sound, @Nullable ServerPlayer p, @Nullable Integer maxLengthSeconds) {
        try {
            CachedAudio audio = AudioStorageManager.audioCache().getAudio(sound);

            if (maxLengthSeconds != null && audio.getDurationSeconds() > maxLengthSeconds) {
                if (p != null) {
                    //TODO Rework this
                    p.displayClientMessage(Lang.translatable("audioplayer.audio_too_long").withStyle(ChatFormatting.DARK_RED), true);
                }
                AudioPlayerMod.LOGGER.error("Audio {} was too long to play", sound);
                return null;
            }

            PlayerThread<T> playerThread = new PlayerThread<>(channel, audio);
            playerThread.startPlaying();
            return playerThread;
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.displayClientMessage(Lang.translatable("audioplayer.play_audio_failed", e.getMessage()).withStyle(ChatFormatting.DARK_RED), true);
            }
            return null;
        }
    }

    public void stop(UUID channelID) {
        ChannelReferenceImpl<?> player = players.get(channelID);
        if (player == null) {
            players.remove(channelID);
            return;
        }
        player.stopPlaying();
    }

    public boolean isPlaying(UUID channelID) {
        ChannelReferenceImpl<?> player = players.get(channelID);
        if (player == null) {
            return false;
        }
        return player.isPlaying();
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public int stopAll(UUID audioId) {
        List<? extends ChannelReferenceImpl<?>> list = players.entrySet().stream().filter(entry -> entry.getValue().getAudioId().equals(audioId)).map(Map.Entry::getValue).toList();
        list.forEach(ChannelReferenceImpl::stopPlaying);
        return list.size();
    }

}
