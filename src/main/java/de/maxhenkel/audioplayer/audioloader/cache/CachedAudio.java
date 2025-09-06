package de.maxhenkel.audioplayer.audioloader.cache;

import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.audioplayer.audioloader.AudioStorageManager;
import de.maxhenkel.audioplayer.utils.AudioUtils;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

public class CachedAudio {

    public static final int FRAME_SIZE = 960;

    private final UUID id;
    private final Path file;
    private final byte[][] opusFrames;
    private final int sampleLength;

    protected CachedAudio(UUID id, Path file, byte[][] opusFrames, int sampleLength) {
        this.id = id;
        this.file = file;
        this.opusFrames = opusFrames;
        this.sampleLength = sampleLength;
    }

    public static CachedAudio load(UUID audioId) throws Exception {
        AudioStorageManager instance = AudioStorageManager.instance();
        Path existingSoundFile = instance.getExistingSoundFile(audioId);
        short[] audio = AudioUtils.convert(existingSoundFile, AudioStorageManager.metadataManager().getVolumeOverride(audioId).orElse(null));

        OpusEncoder encoder = VoicechatAudioPlayerPlugin.voicechatApi.createEncoder();
        int frameCount = (audio.length + FRAME_SIZE - 1) / FRAME_SIZE;
        byte[][] opusFrames = new byte[frameCount][];

        short[] window = new short[FRAME_SIZE];
        for (int i = 0; i < frameCount; i++) {
            int length = Math.min(FRAME_SIZE, audio.length - i * FRAME_SIZE);
            System.arraycopy(audio, i * FRAME_SIZE, window, 0, length);
            if (length < FRAME_SIZE) {
                Arrays.fill(window, length, FRAME_SIZE, (short) 0);
            }
            opusFrames[i] = encoder.encode(window);
        }
        encoder.close();

        return new CachedAudio(audioId, existingSoundFile, opusFrames, audio.length);
    }

    public UUID getId() {
        return id;
    }

    public Path getFile() {
        return file;
    }

    public byte[][] getOpusFrames() {
        return opusFrames;
    }

    public int getSampleLength() {
        return sampleLength;
    }

    public float getDurationSeconds() {
        return AudioUtils.getLengthSeconds(sampleLength);
    }
}
