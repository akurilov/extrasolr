package com.github.akurilov.extrasolr.parse;

import org.apache.commons.collections4.map.LRUMap;

import java.util.Map;
import java.util.function.Predicate;

public class FixedCacheUniquenessFilter<T>
implements Predicate<T> {

    private static final Object dummy = new Object();
    private final Map<T, Object> cache;

    public FixedCacheUniquenessFilter(final int capacity) {
        cache = new LRUMap<>(capacity);
    }

    @Override
    public boolean test(final T v) {
        synchronized(cache) {
            return null == cache.putIfAbsent(v, dummy);
        }
    }
}
