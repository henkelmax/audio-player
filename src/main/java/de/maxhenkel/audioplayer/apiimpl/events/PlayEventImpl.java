package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.events.PlayEvent;
import de.maxhenkel.audioplayer.api.exceptions.ChannelAlreadyOverriddenException;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayEventImpl implements PlayEvent {

    protected final AudioData audioData;
    protected final ServerLevel level;
    @Nullable
    protected final ServerPlayer player;
    protected UUID soundId;
    protected final float defaultDistance;
    protected float distance;
    protected String category;
    protected Vec3 position;
    @Nullable
    protected ChannelReference<?> overrideChannel;
    protected boolean cancelled;

    public PlayEventImpl(AudioData audioData, ServerLevel level, @Nullable ServerPlayer player, UUID soundId, float defaultDistance, float distance, String category, Vec3 position) {
        this.audioData = audioData;
        this.level = level;
        this.player = player;
        this.soundId = soundId;
        this.defaultDistance = defaultDistance;
        this.distance = distance;
        this.category = category;
        this.position = position;
    }

    @Override
    public ModuleAccessor getData() {
        return audioData;
    }

    @Override
    public void overrideChannel(ChannelReference<?> channel) throws ChannelAlreadyOverriddenException {
        if (overrideChannel != null) {
            throw new ChannelAlreadyOverriddenException("Channel already overridden with audio ID %s".formatted(overrideChannel.getAudioId()));
        }
        overrideChannel = channel;
    }

    @Override
    public void setSoundId(UUID soundId) {
        this.soundId = soundId;
    }

    @Override
    public UUID getSoundId() {
        return soundId;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setPosition(Vec3 position) {
        this.position = position;
    }

    @Override
    public Vec3 getPosition() {
        return position;
    }

    @Override
    public boolean isOverridden() {
        return overrideChannel != null;
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
    public float getDefaultDistance() {
        return defaultDistance;
    }

    @Override
    public void setDistance(float distance) {
        this.distance = distance;
    }

    @Override
    public float getDistance() {
        return distance;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Nullable
    public ChannelReference<?> getOverrideChannel() {
        return overrideChannel;
    }
}
