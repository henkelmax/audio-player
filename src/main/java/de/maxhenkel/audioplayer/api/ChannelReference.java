package de.maxhenkel.audioplayer.api;

import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public interface ChannelReference<T extends AudioChannel> {

    UUID getAudioId();

    T getChannel();

    void stopPlaying();

    boolean isStarted();

    boolean isPlaying();

    boolean isStopped();

    void setOnChannelUpdate(@Nullable Consumer<T> onChannelUpdate);

}
