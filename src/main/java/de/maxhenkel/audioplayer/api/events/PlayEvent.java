package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;

public interface PlayEvent extends ModuleAccessor {

    void overrideChannel(ChannelReference<?> channel);

    boolean isOverridden();

}
