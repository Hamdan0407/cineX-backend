package com.bookmyshow.cache;

import org.springframework.cache.Cache;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decorator for Spring Cache that records cache hits, misses, and evictions.
 * Supports both embedded ConcurrentMapCache and RedisCache seamlessly.
 */
public class StatsTrackingCache implements Cache {

    private final Cache delegate;
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public StatsTrackingCache(Cache delegate) {
        this.delegate = delegate;
    }

    public Cache getDelegate() {
        return delegate;
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public long getEvictions() {
        return evictions.get();
    }

    public void resetStats() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper wrapper = delegate.get(key);
        if (wrapper != null) {
            hits.incrementAndGet();
        } else {
            misses.incrementAndGet();
        }
        return wrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        if (value != null) {
            hits.incrementAndGet();
        } else {
            misses.incrementAndGet();
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        // If we get via valueLoader, check if it was already present
        ValueWrapper existing = delegate.get(key);
        if (existing != null) {
            hits.incrementAndGet();
            return (T) existing.get();
        } else {
            misses.incrementAndGet();
            return delegate.get(key, valueLoader);
        }
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        evictions.incrementAndGet();
        delegate.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean evicted = delegate.evictIfPresent(key);
        if (evicted) {
            evictions.incrementAndGet();
        }
        return evicted;
    }

    @Override
    public void clear() {
        evictions.incrementAndGet();
        delegate.clear();
    }
}
