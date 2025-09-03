package de.maxhenkel.audioplayer.api;

import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;

import java.util.UUID;

public interface ChannelReference<T extends AudioChannel> {

    UUID getAudioId();

    T getChannel();

    void stopPlaying();

    boolean isStarted();

    boolean isPlaying();

    boolean isStopped();

}
