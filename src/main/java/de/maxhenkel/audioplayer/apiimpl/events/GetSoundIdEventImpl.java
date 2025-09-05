package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.AudioDataModule;
import de.maxhenkel.audioplayer.api.data.ModuleKey;
import de.maxhenkel.audioplayer.api.events.GetSoundIdEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class GetSoundIdEventImpl implements GetSoundIdEvent {

    private final AudioData itemData;
    @Nullable
    private UUID soundId;

    public GetSoundIdEventImpl(AudioData itemData, @Nullable UUID soundId) {
        this.itemData = itemData;
        this.soundId = soundId;
    }

    @Override
    @Nullable
    public <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> moduleKey) {
        return itemData.getModule(moduleKey);
    }

    @Override
    @Nullable
    public UUID getSoundId() {
        return soundId;
    }

    @Override
    public void setSoundId(@Nullable UUID soundId) {
        this.soundId = soundId;
    }

}
