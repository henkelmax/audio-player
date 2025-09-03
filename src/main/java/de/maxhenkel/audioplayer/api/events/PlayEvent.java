package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;

import java.util.UUID;

public interface PlayEvent extends ModuleAccessor {

    void overrideChannel(UUID channelId);

    boolean isOverridden();

}
