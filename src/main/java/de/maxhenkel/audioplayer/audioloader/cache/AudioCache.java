package de.maxhenkel.audioplayer.audioloader.cache;

import de.maxhenkel.audioplayer.AudioPlayerMod;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class AudioCache {

    private final Map<UUID, CachedAudio> audioCache;

    public AudioCache() {
        audioCache = Collections.synchronizedMap(new LruCache<>(AudioPlayerMod.SERVER_CONFIG.cacheSize.get()));
    }

    public CachedAudio getAudio(UUID audioId) throws Exception {
        CachedAudio cachedAudio = audioCache.get(audioId);
        if (cachedAudio != null) {
            return cachedAudio;
        }
        cachedAudio = CachedAudio.load(audioId);
        audioCache.put(audioId, cachedAudio);
        return cachedAudio;
    }

    public void invalidate(UUID audioId) {
        audioCache.remove(audioId);
    }

    public void clear() {
        audioCache.clear();
    }

}
