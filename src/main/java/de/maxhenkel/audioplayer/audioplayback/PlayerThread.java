package de.maxhenkel.audioplayer.audioplayback;

import de.maxhenkel.audioplayer.audioloader.cache.CachedAudio;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class PlayerThread<T extends AudioChannel> extends Thread {

    private static final long FRAME_DURATION_NS = 20_000_000;

    private final T audioChannel;
    private final CachedAudio audio;
    private boolean started;
    @Nullable
    private Runnable onStopped;
    @Nullable
    private Consumer<T> channelUpdate;

    public PlayerThread(T audioChannel, CachedAudio audio) {
        this.audioChannel = audioChannel;
        this.audio = audio;
        setDaemon(true);
        setName(String.format("AudioPlayback-%s", audioChannel.getId()));
    }

    public void startPlaying() {
        if (started) {
            return;
        }
        start();
        started = true;
    }

    private void updateChannel(T audioChannel) {
        if (channelUpdate != null) {
            channelUpdate.accept(audioChannel);
        }
    }

    public void stopPlaying() {
        interrupt();
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isPlaying() {
        return isAlive();
    }

    public boolean isStopped() {
        return started && !isAlive();
    }

    public void setOnStopped(@Nullable Runnable onStopped) {
        this.onStopped = onStopped;
    }

    public void setChannelUpdate(@Nullable Consumer<T> channelUpdate) {
        this.channelUpdate = channelUpdate;
    }

    @Override
    public void run() {
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