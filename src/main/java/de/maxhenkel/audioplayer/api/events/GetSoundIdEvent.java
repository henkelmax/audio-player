package de.maxhenkel.audioplayer.api.events;

import de.maxhenkel.audioplayer.api.data.ModuleAccessor;

import javax.annotation.Nullable;
import java.util.UUID;

public interface GetSoundIdEvent {

    ModuleAccessor getData();

    @Nullable
    UUID getSoundId();

    void setSoundId(@Nullable UUID soundId);

}
