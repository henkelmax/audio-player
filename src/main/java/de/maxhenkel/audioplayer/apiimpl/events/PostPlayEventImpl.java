package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.AudioData;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.events.PostPlayEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PostPlayEventImpl implements PostPlayEvent {

    private final ChannelReference<?> channel;
    private final AudioData audioData;
    private final UUID soundId;
    private final String category;
    private final Vec3 position;
    private final ServerLevel level;
    private final @Nullable ServerPlayer player;
    private final float distance;

    public PostPlayEventImpl(ChannelReference<?> channel, AudioData audioData, UUID soundId, String category, Vec3 position, ServerLevel level, @Nullable ServerPlayer player, float distance) {
        this.channel = channel;
        this.audioData = audioData;
        this.soundId = soundId;
        this.category = category;
        this.position = position;
        this.level = level;
        this.player = player;
        this.distance = distance;
    }

    @Override
    public ChannelReference<?> getChannel() {
        return channel;
    }

    @Override
    public ModuleAccessor getData() {
        return audioData;
    }

    @Override
    public UUID getSoundId() {
        return soundId;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public Vec3 getPosition() {
        return position;
    }

    @Override
    public ServerLevel getLevel() {
        return level;
    }

    @Override
    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public float getDistance() {
        return distance;
    }
}
