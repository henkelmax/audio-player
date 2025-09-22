package de.maxhenkel.audioplayer.api.data;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public interface ModuleAccessor {

    <T extends AudioDataModule> Optional<T> getModule(ModuleKey<T> moduleKey);

    /**
     * @return the stored sound id - this isn't necessarily the sound id that will be played
     */
    UUID getSoundId();

    /**
     * @return the stored range - this isn't necessarily the range that will be used
     */
    @Nullable
    Float getRange();

}
