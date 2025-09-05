package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.ChannelReference;
import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.PlayEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayEventImpl implements PlayEvent {

    protected final AudioData audioData;
    protected final ServerLevel level;
    @Nullable
    protected final ServerPlayer player;
    @Nullable
    protected ChannelReference<?> overrideChannel;

    public PlayEventImpl(AudioData audioData, ServerLevel level, @Nullable ServerPlayer player) {
        this.audioData = audioData;
        this.level = level;
        this.player = player;
    }

    @Override
    public void overrideChannel(ChannelReference<?> channel) {
        overrideChannel = channel;
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
