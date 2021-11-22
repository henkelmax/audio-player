package de.maxhenkel.audioplayer.interfaces;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IJukebox {

    @Nullable
    UUID getChannelID();

    void setChannelID(@Nullable UUID channelID);

}
