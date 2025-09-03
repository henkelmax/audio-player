package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.AudioEvents;
import de.maxhenkel.audioplayer.audioloader.AudioData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayEventImpl implements AudioEvents.PlayEvent {

    protected final AudioData audioData;
    @Nullable
    protected UUID overrideChannel;

    public PlayEventImpl(AudioData audioData) {
        this.audioData = audioData;
    }

    @Override
    public void overrideChannel(UUID channelId) {
        overrideChannel = channelId;
    }

    @Override
    public boolean isOverridden() {
        return overrideChannel != null;
    }

    @Nullable
    public UUID getOverrideChannel() {
        return overrideChannel;
    }

    @Override
    @Nullable
    public <T extends AudioDataModule> T getModule(ModuleKey<T> moduleKey) {
        return audioData.getModule(moduleKey).orElse(null);
    }
}
