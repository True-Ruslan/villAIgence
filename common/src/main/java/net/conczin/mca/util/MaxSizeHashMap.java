package net.conczin.mca.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded cache map that evicts the least-recently-used entry once it exceeds {@code maxSize}. */
public class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public MaxSizeHashMap(int maxSize) {
        super(16, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}