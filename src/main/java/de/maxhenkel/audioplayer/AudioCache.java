package de.maxhenkel.audioplayer;

import java.util.*;
import java.util.function.Supplier;

public class AudioCache {

    private final int size;
    private final Map<UUID, short[]> audioCache;
    private final Queue<UUID> orderedKeys;

    public AudioCache(int size) {
        this.size = size;
        this.audioCache = new HashMap<>();
        this.orderedKeys = new ArrayDeque<>();
    }

    public synchronized short[] get(UUID id, Supplier<short[]> getter) {
        short[] data = audioCache.get(id);
        if (data == null) {
            short[] uncachedData = getter.get();
            pushCache(id, uncachedData);
            return uncachedData;
        }
        return data;
    }

    private void pushCache(UUID id, short[] data) {
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

}
