package de.maxhenkel.audioplayer.interfaces;

import javax.annotation.Nullable;
import java.util.UUID;

public interface ChannelHolder {

    @Nullable
    UUID soundplayer$getChannelID();

    void soundplayer$setChannelID(@Nullable UUID channelID);

}
