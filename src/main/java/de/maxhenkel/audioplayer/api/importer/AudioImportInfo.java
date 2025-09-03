package de.maxhenkel.audioplayer.api.importer;

import javax.annotation.Nullable;
import java.util.UUID;

public final class AudioImportInfo {
    
    private final UUID audioId;
    @Nullable
    private final String name;

    public AudioImportInfo(UUID audioId, @Nullable String name) {
        this.audioId = audioId;
        this.name = name;
    }

    public UUID getAudioId() {
        return audioId;
    }

    @Nullable
    public String getName() {
        return name;
    }

}
