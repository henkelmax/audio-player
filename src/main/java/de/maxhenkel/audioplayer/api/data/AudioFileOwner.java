package de.maxhenkel.audioplayer.api.data;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface AudioFileOwner {

    @Nonnull
    UUID getUUID();

    @Nonnull
    String getName();

}
