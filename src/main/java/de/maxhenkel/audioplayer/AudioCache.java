package de.maxhenkel.audioplayer;

import java.util.*;

public class AudioCache {

    private final int size;
    private final Map<UUID, short[]> audioCache;
    private final Queue<UUID> orderedKeys;

    public AudioCache(int size) {
        this.size = size;
        this.audioCache = new HashMap<>();
        this.orderedKeys = new ArrayDeque<>();
    }

    public synchronized short[] get(UUID id, AudioSupplier supplier) throws Exception {
        short[] data = audioCache.get(id);
        if (data == null) {
            short[] uncachedData = supplier.get();
            pushCache(id, uncachedData);
            return uncachedData;
        }
        return data;
    }

    private void pushCache(UUID id, short[] data) {
        if (size <= 0) {
            return;
        }
        if (audioCache.containsKey(id)) {
            return;
        }
        if (orderedKeys.size() >= size) {
            UUID poll = orderedKeys.poll();
            audioCache.remove(poll);
        }
        orderedKeys.add(id);
        audioCache.put(id, data);
    }

    public interface AudioSupplier {
        short[] get() throws Exception;
    }

}
