package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.audioloader.cache.CachedAudio;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class PlayerThread<T extends AudioChannel> extends Thread {

    private static final long FRAME_DURATION_NS = 20_000_000;

    private final T audioChannel;
    @Nullable
    private CachedAudio audio;
    @Nullable
    private final Runnable onStopped;
    @Nullable
    private Consumer<T> channelUpdate;

    public PlayerThread(T audioChannel, @Nullable Runnable onStopped) {
        this.audioChannel = audioChannel;
        this.onStopped = onStopped;
        setDaemon(true);
        setName("AudioPlayback-%s".formatted(audioChannel.getId()));
    }

    public void startPlaying(CachedAudio cachedAudio) {
        if (audio != null) {
            return;
        }
        this.audio = cachedAudio;
        start();
    }

    @Nullable
    public CachedAudio getAudio() {
        return audio;
    }

    private void updateChannel(T audioChannel) {
        if (channelUpdate != null) {
            channelUpdate.accept(audioChannel);
        }
    }

    public void stopPlaying() {
        interrupt();
    }

    public boolean isInitialized() {
        return audio != null;
    }

    /**
     * @return true if the thread is actively playing music - This is also false if the thread is not started yet
     */
    public boolean isCurrentlyPlaying() {
        return isAlive();
    }

    public boolean isStopped() {
        return isInitialized() && !isAlive();
    }

    public void setChannelUpdate(@Nullable Consumer<T> channelUpdate) {
        this.channelUpdate = channelUpdate;
    }

    @Override
    public void run() {
        if (isInterrupted()) {
            if (onStopped != null) {
                onStopped.run();
            }
            return;
        }
        int framePosition = 0;

        long startTime = System.nanoTime();

        byte[] frame;

        while ((frame = audio.getOpusFrames()[framePosition]) != null) {
            audioChannel.send(frame);
            framePosition++;
            updateChannel(audioChannel);
            long waitTimestamp = startTime + framePosition * FRAME_DURATION_NS;

            long waitNanos = waitTimestamp - System.nanoTime();

            try {
                if (waitNanos > 0L) {
                    Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000));
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        audioChannel.flush();

        if (onStopped != null) {
            onStopped.run();
        }
    }

}