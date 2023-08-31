package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// TODO Move this to the voice chat API
public class StaticAudioPlayer implements de.maxhenkel.voicechat.api.audiochannel.AudioPlayer, Runnable {

    private final Thread playbackThread;
    private final AudioSupplier audio;
    private final VoicechatServerApi api;
    private final String category;
    private final Vec3 pos;
    private final ServerLevel level;
    private final float distance;
    private final OpusEncoder encoder;

    private static final long FRAME_SIZE_NS = 20_000_000;
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;

    private final ConcurrentHashMap<UUID, StaticAudioChannel> audioChannels;
    private boolean started;
    @Nullable
    private Runnable onStopped;

    public StaticAudioPlayer(short[] audio, VoicechatServerApi api, String category, Vec3 pos, UUID playerID, ServerLevel level, float distance) {
        this.playbackThread = new Thread(this);
        this.audio = new AudioSupplier(audio);
        this.api = api;
        this.category = category;
        this.pos = pos;
        this.audioChannels = new ConcurrentHashMap<>();
        this.encoder = api.createEncoder();
        this.playbackThread.setDaemon(true);
        this.playbackThread.setName("StaticAudioPlayer-%s".formatted(playerID));
        this.level = level;
        this.distance = distance;
    }

    public static StaticAudioPlayer create(VoicechatServerApi api, ServerLevel level, UUID sound, ServerPlayer p, int maxLengthSeconds, String category, Vec3 pos, UUID playerID, float distance) {
        try {
            short[] audio = AudioManager.getSound(level.getServer(), sound);

            if (AudioManager.getLengthSeconds(audio) > maxLengthSeconds) {
                if (p != null) {
                    p.displayClientMessage(Component.literal("Audio is too long to play").withStyle(ChatFormatting.DARK_RED), true);
                } else {
                    AudioPlayer.LOGGER.error("Audio {} was too long to play", sound);
                }
                return null;
            }

            StaticAudioPlayer instance = new StaticAudioPlayer(audio, api, category, pos, playerID, level, distance);
            instance.startPlaying();
            return instance;
        } catch (Exception e) {
            AudioPlayer.LOGGER.error("Failed to play audio", e);
            if (p != null) {
                p.displayClientMessage(Component.literal("Failed to play audio: %s".formatted(e.getMessage())).withStyle(ChatFormatting.DARK_RED), true);
            }
            return null;
        }
    }

    @Override
    public void startPlaying() {
        if (started) {
            return;
        }
        this.playbackThread.start();
        started = true;
    }

    @Override
    public void stopPlaying() {
        this.playbackThread.interrupt();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isPlaying() {
        return playbackThread.isAlive();
    }

    @Override
    public boolean isStopped() {
        return started && !playbackThread.isAlive();
    }

    @Override
    public void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    @Override
    public void run() {
        int framePosition = 0;

        ScheduledFuture<?> nearbyPlayersTask = AudioPlayer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            List<ServerPlayer> players = api.getPlayersInRange(api.fromServerLevel(this.level), api.createPosition(pos.x, pos.y, pos.z), distance + 1F, serverPlayer -> {
                VoicechatConnection connection = api.getConnectionOf(serverPlayer);
                if (connection != null) {
                    // TODO Either document in the api that this helper is square distance, or provide a spherical version (or both?)
                    Vec3 playerPos = ((ServerPlayer) serverPlayer.getPlayer()).getPosition(0.0F);
                    return !connection.isDisabled() && pos.distanceTo(playerPos) <= distance;
                }
                return false;
            }).stream().map(Player::getPlayer).map(ServerPlayer.class::cast).toList();

            for (ServerPlayer player : players) {
                this.audioChannels.computeIfAbsent(player.getUUID(), uuid -> {
                    StaticAudioChannel audioChannel = api.createStaticAudioChannel(UUID.randomUUID(), api.fromServerLevel(this.level), api.getConnectionOf(api.fromServerPlayer(player)));
                    audioChannel.setCategory(this.category);
                    return audioChannel;
                });
            }

            List<UUID> uuids = players.stream().map(ServerPlayer::getUUID).toList();

            for (UUID uuid : this.audioChannels.keySet()) {
                if (!uuids.contains(uuid)) {
                    StaticAudioChannel toRemove = this.audioChannels.remove(uuid);
                    toRemove.flush();
                }
            }
        }, 0L, 100L, TimeUnit.MILLISECONDS);

        long startTime = System.nanoTime();

        short[] frame;

        while ((frame = this.audio.get()) != null) {
            if (frame.length != FRAME_SIZE) {
                AudioPlayer.LOGGER.error("Got invalid audio frame size {}!={}", frame.length, FRAME_SIZE);
                break;
            }
            byte[] encoded = encoder.encode(frame);
            for (StaticAudioChannel audioChannel : this.audioChannels.values()) {
                audioChannel.send(encoded);
            }
            framePosition++;
            long waitTimestamp = startTime + framePosition * FRAME_SIZE_NS;

            long waitNanos = waitTimestamp - System.nanoTime();

            try {
                if (waitNanos > 0L) {
                    Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000));
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        encoder.close();
        nearbyPlayersTask.cancel(true);

        for (StaticAudioChannel audioChannel : this.audioChannels.values()) {
            audioChannel.flush();
        }

        if (onStopped != null) {
            onStopped.run();
        }
    }

    public class AudioSupplier implements Supplier<short[]> {

        private final short[] audioData;
        private final short[] frame;
        private int framePosition;

        public AudioSupplier(short[] audioData) {
            this.audioData = audioData;
            this.frame = new short[FRAME_SIZE];
        }

        @Override
        public short[] get() {
            if (framePosition >= audioData.length) {
                return null;
            }

            Arrays.fill(frame, (short) 0);
            System.arraycopy(audioData, framePosition, frame, 0, Math.min(frame.length, audioData.length - framePosition));
            framePosition += frame.length;
            return frame;
        }
    }
}
