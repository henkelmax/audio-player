package de.maxhenkel.audioplayer.api.data;

import javax.annotation.Nullable;
import java.util.UUID;

public interface AudioFileMetadata {

    UUID getAudioId();

    @Nullable
    String getFileName();

    @Nullable
    Float getVolume();

    @Nullable
    Long getCreated();

    @Nullable
    String getSha256();

    @Nullable
    AudioFileOwner getOwner();

}
