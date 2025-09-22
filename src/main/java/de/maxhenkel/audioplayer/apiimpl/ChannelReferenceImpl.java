package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.audioplayback.PlayerThread;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class ChannelReferenceImpl<T extends AudioChannel> implements ChannelReference<T> {

    private final T channel;
    private final UUID audioId;
    private final PlayerThread<T> player;

    public ChannelReferenceImpl(T channel, UUID audioId, PlayerThread<T> player) {
        this.channel = channel;
        this.audioId = audioId;
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
        player.stopPlaying();
    }

    @Override
    public boolean isInitialized() {
        return player.isInitialized();
    }

    @Override
    public boolean isCurrentlyPlaying() {
        return player.isCurrentlyPlaying();
    }

    @Override
    public boolean isStopped() {
        return player.isStopped();
    }

    @Override
    public void setOnChannelUpdate(@Nullable Consumer<T> onChannelUpdate) {
        player.setChannelUpdate(onChannelUpdate);
    }

}
