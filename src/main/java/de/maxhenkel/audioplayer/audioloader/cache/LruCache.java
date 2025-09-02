package de.maxhenkel.audioplayer.audioloader.cache;

import java.util.*;

public class LruCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxEntries;

    public LruCache(int maxEntries) {
        super(maxEntries + 1, 1F, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

}
