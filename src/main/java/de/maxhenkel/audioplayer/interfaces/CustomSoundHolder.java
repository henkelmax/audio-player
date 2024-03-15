package de.maxhenkel.audioplayer.interfaces;

import java.util.Optional;
import java.util.UUID;

public interface CustomSoundHolder {

    UUID soundplayer$getSoundID();

    void soundplayer$setSoundID(UUID soundID);

    Optional<Float> soundplayer$getRange();

    void soundplayer$setRange(Optional<Float> range);

    boolean soundplayer$isStatic();

    void soundplayer$setStatic(boolean staticSound);

}
