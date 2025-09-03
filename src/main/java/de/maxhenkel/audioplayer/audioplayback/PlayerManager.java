package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.AudioPlayerMod;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.audioloader.cache.CachedAudio;
import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerManager {

    private final Map<UUID, PlayerReference<?>> players;
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
    public UUID playLocational(ServerLevel level, Vec3 pos, PlayerType type, AudioData sound, @Nullable ServerPlayer player) {
        UUID soundIdToPlay = sound.getSoundIdToPlay();
        if (soundIdToPlay == null) {
            return null;
        }
        PlayerReference<LocationalAudioChannel> ref = playLocational(
                level,
                pos,
                soundIdToPlay,
                player,
                sound.getRange(type),
                type.getCategory(),
                type.getMaxDuration().get()
        );
        if (ref == null) {
            return null;
        }
        return ref.channel();
    }

    @Nullable
    public PlayerReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID sound, @Nullable ServerPlayer p, float distance, @Nullable String category, int maxLengthSeconds) {
        return playLocational(level, pos, sound, p, distance, category, maxLengthSeconds, false);
    }

    @Nullable
    public PlayerReference<LocationalAudioChannel> playLocational(ServerLevel level, Vec3 pos, UUID sound, @Nullable ServerPlayer p, float distance, @Nullable String category, int maxLengthSeconds, boolean byCommand) {
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
            if (connection != null) {
                return connection.isDisabled();
            }
            return true;
        }).stream().map(Player::getPlayer).map(ServerPlayer.class::cast).forEach(player -> {
            player.displayClientMessage(Component.literal("You need to enable voice chat to hear custom audio"), true);
        });

        return playChannel(channel, sound, p, maxLengthSeconds, byCommand);
    }

    private <T extends AudioChannel> PlayerReference<T> playChannel(T channel, UUID sound, @Nullable ServerPlayer p, int maxLengthSeconds, boolean byCommand) {
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<PlayerThread<T>> player = new AtomicReference<>();

        PlayerReference<T> playerReference = new PlayerReference<>(channel.getId(), () -> {
            synchronized (stopped) {
                stopped.set(true);
                PlayerThread<T> audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.stopPlaying();
                }
            }
        }, player, sound, byCommand);
        players.put(playerReference.channel(), playerReference);

        executor.execute(() -> {
            PlayerThread<T> playerThread = playChannel(channel, sound, p, maxLengthSeconds);
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
    private <T extends AudioChannel> PlayerThread<T> playChannel(T channel, UUID sound, @Nullable ServerPlayer p, int maxLengthSeconds) {
        try {
            CachedAudio audio = AudioStorageManager.audioCache().getAudio(sound);

            if (audio.getDurationSeconds() > maxLengthSeconds) {
                if (p != null) {
                    p.displayClientMessage(Component.literal("Audio is too long to play").withStyle(ChatFormatting.DARK_RED), true);
                } else {
                    AudioPlayerMod.LOGGER.error("Audio {} was too long to play", sound);
                }
                return null;
            }

            PlayerThread<T> playerThread = new PlayerThread<>(channel, audio);
            playerThread.startPlaying();
            return playerThread;
        } catch (Exception e) {
            AudioPlayerMod.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.displayClientMessage(Component.literal("Failed to play audio: %s".formatted(e.getMessage())).withStyle(ChatFormatting.DARK_RED), true);
            }
            return null;
        }
    }

    public void stop(UUID channelID) {
        PlayerReference<?> player = players.get(channelID);
        if (player != null) {
            player.onStop.stop();
        }
        players.remove(channelID);
    }

    public boolean isPlaying(UUID channelID) {
        PlayerReference<?> player = players.get(channelID);
        if (player == null) {
            return false;
        }
        PlayerThread<?> p = player.player.get();
        if (p == null) {
            return true;
        }
        return p.isPlaying();
    }

    private static PlayerManager instance;

    public static PlayerManager instance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private interface Stoppable {
        void stop();
    }

    public record PlayerReference<T extends AudioChannel>(UUID channel,
                                                          Stoppable onStop,
                                                          AtomicReference<PlayerThread<T>> player,
                                                          UUID sound, boolean byCommand) {
    }

    @Nullable
    public UUID findChannelID(UUID sound, boolean onlyByCommand) {
        for (Map.Entry<UUID, PlayerReference<?>> entry : players.entrySet()) {
            if (entry.getValue().sound.equals(sound) && (entry.getValue().byCommand || !onlyByCommand)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
