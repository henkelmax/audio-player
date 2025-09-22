package de.maxhenkel.audioplayer.apiimpl.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;
import de.maxhenkel.audioplayer.api.events.GetSoundIdEvent;
import de.maxhenkel.audioplayer.audioloader.AudioData;

import javax.annotation.Nullable;
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
    public ModuleAccessor getData() {
        return itemData;
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
