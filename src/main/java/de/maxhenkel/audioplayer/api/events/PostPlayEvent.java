package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public interface PostPlayEvent {

    ChannelReference<?> getChannel();

    ModuleAccessor getData();

    UUID getSoundId();

    String getCategory();

    Vec3 getPosition();

    ServerLevel getLevel();

    @Nullable
    ServerPlayer getPlayer();

    float getDistance();

}
