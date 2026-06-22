package de.maxhenkel.audioplayer.api.importer;

import de.maxhenkel.audioplayer.api.data.AudioFileMetadata;

import java.util.UUID;

public interface ImportedAudio {

    UUID getAudioId();

    AudioFileMetadata getMetadata();

    boolean isDuplicate();
}
