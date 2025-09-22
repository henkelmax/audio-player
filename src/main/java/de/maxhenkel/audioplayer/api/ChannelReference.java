package de.maxhenkel.audioplayer.api;

import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public interface ChannelReference<T extends AudioChannel> {

    UUID getAudioId();

    T getChannel();

    void stopPlaying();

    /**
     * @return if the audio is loaded and playing - this is also true if the channel is done playing
     */
    boolean isInitialized();

    /**
     * @return if the channel is currently playing audio
     */
    boolean isCurrentlyPlaying();

    /**
     * @return if the channel is done playing
     */
    boolean isStopped();

    void setOnChannelUpdate(@Nullable Consumer<T> onChannelUpdate);

}
