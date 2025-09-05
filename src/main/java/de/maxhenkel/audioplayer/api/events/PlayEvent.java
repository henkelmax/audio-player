package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

public interface PlayEvent extends ModuleAccessor {

    void overrideChannel(ChannelReference<?> channel);

    boolean isOverridden();

    @Nullable
    ServerPlayer getPlayer();

}
