package de.maxhenkel.audioplayer.apiimpl;

import de.maxhenkel.audioplayer.api.data.AudioFileMetadata;

import java.util.UUID;

public class ImportedAudioImpl implements de.maxhenkel.audioplayer.api.importer.ImportedAudio {

    private final UUID audioId;
    private final AudioFileMetadata metadata;
    private final boolean duplicate;

    public ImportedAudioImpl(UUID audioId, AudioFileMetadata metadata, boolean duplicate) {
        this.audioId = audioId;
        this.metadata = metadata;
        this.duplicate = duplicate;
    }

    @Override
    public UUID getAudioId() {
        return audioId;
    }

    @Override
    public AudioFileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean isDuplicate() {
        return duplicate;
    }
}
