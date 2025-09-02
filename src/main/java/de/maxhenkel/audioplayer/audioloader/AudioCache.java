package de.maxhenkel.audioplayer.audioloader;

import java.util.*;

public class AudioCache {

    private final int size;
    private final Map<UUID, short[]> audioCache;
    private final Deque<UUID> accessQueue;

    public AudioCache(int size) {
        this.size = size;
        this.audioCache = new HashMap<>();
        this.accessQueue = new ArrayDeque<>();
    }

    public short[] get(UUID id, AudioSupplier supplier) throws Exception {
        synchronized (audioCache) {
            short[] data = audioCache.get(id);
            if (data == null) {
                short[] uncachedData = supplier.get();
                pushCache(id, uncachedData);
                return uncachedData;
            }
            accessQueue.remove(id);
            accessQueue.addFirst(id);
            return data;
        }
    }

    public void remove(UUID id) {
        synchronized (audioCache) {
            audioCache.remove(id);
            accessQueue.remove(id);
        }
    }

    private void pushCache(UUID id, short[] data) {
        if (size <= 0) {
            return;
        }
        if (audioCache.containsKey(id)) {
            return;
        }
        if (accessQueue.size() >= size) {
            UUID leastRecentlyUsed = accessQueue.removeLast();
            audioCache.remove(leastRecentlyUsed);
        }
        accessQueue.addFirst(id);
        audioCache.put(id, data);
    }

    public interface AudioSupplier {
        short[] get() throws Exception;
    }

}
