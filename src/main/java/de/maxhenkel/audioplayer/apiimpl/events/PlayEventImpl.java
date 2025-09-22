package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.PlayEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayEventImpl implements PlayEvent {

    protected final AudioData audioData;
    protected final ServerLevel level;
    @Nullable
    protected final ServerPlayer player;
    protected float defaultDistance;
    protected String category;
    protected Vec3 position;
    @Nullable
    protected ChannelReference<?> overrideChannel;

    public PlayEventImpl(AudioData audioData, ServerLevel level, @Nullable ServerPlayer player, float defaultDistance, String category, Vec3 position) {
        this.audioData = audioData;
        this.level = level;
        this.player = player;
        this.defaultDistance = defaultDistance;
        this.category = category;
        this.position = position;
    }

    @Override
    public void overrideChannel(ChannelReference<?> channel) {
        overrideChannel = channel;
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

    @Nullable
    public ChannelReference<?> getOverrideChannel() {
        return overrideChannel;
    }

    @Override
    @Nullable
    public <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> moduleKey) {
        return audioData.getModule(moduleKey);
    }
}
