package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.audioplayback.PlayerThread;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ChannelReferenceImpl<T extends AudioChannel> implements ChannelReference<T> {

    private final T channel;
    private final UUID audioId;
    private final Stoppable onStop;
    private final AtomicReference<PlayerThread<T>> player;

    public ChannelReferenceImpl(T channel, UUID audioId, AtomicReference<PlayerThread<T>> player, Stoppable onStop) {
        this.channel = channel;
        this.audioId = audioId;
        this.onStop = onStop;
        this.player = player;
    }

    @Override
    public UUID getAudioId() {
        return audioId;
    }

    @Override
    public T getChannel() {
        return channel;
    }

    @Override
    public void stopPlaying() {
        onStop.stop();
    }

    @Override
    public boolean isStarted() {
        PlayerThread<T> t = player.get();
        if (t == null) {
            return false;
        }
        return t.isStarted();
    }

    @Override
    public boolean isPlaying() {
        PlayerThread<T> t = player.get();
        if (t == null) {
            return true;
        }
        return t.isPlaying();
    }

    @Override
    public boolean isStopped() {
        PlayerThread<T> t = player.get();
        if (t == null) {
            return false;
        }
        return t.isStopped();
    }

    @Override
    public void setOnChannelUpdate(@Nullable Consumer<T> onChannelUpdate) {
        PlayerThread<T> t = player.get();
        if (t == null) {
            return;
        }
        t.setChannelUpdate(onChannelUpdate);
    }

    public interface Stoppable {
        void stop();
    }

}
