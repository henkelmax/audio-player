package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.exceptions.ChannelAlreadyOverriddenException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface PlayEvent extends ModuleAccessor {

    /**
     * You should check {@link #isOverridden()} before overriding the channel, or else this will throw an exception.
     *
     * @param channel The channel to override
     * @throws ChannelAlreadyOverriddenException If the channel has already been overridden
     */
    void overrideChannel(ChannelReference<?> channel) throws ChannelAlreadyOverriddenException;

    void setCategory(String category);

    String getCategory();

    void setPosition(Vec3 position);

    Vec3 getPosition();

    boolean isOverridden();

    ServerLevel getLevel();

    @Nullable
    ServerPlayer getPlayer();

    float getDefaultDistance();

}
