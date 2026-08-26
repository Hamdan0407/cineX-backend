package com.bookmyshow.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CacheManager wrapper that wraps all created Caches in StatsTrackingCache.
 * Provides centralization for hits, misses, and eviction monitoring.
 */
public class StatsTrackingCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Map<String, StatsTrackingCache> cacheMap = new ConcurrentHashMap<>();
    private final String cacheType;

    public StatsTrackingCacheManager(CacheManager delegate, String cacheType) {
        this.delegate = delegate;
        this.cacheType = cacheType;
    }

    public String getCacheType() {
        return cacheType;
    }

    public CacheManager getDelegate() {
        return delegate;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, key -> {
            Cache cache = delegate.getCache(key);
            return cache != null ? new StatsTrackingCache(cache) : null;
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        Collection<String> names = delegate.getCacheNames();
        return names != null ? names : Collections.emptyList();
    }

    public Map<String, StatsTrackingCache> getTrackedCaches() {
        // Ensure all delegate cache names are loaded into cacheMap
        for (String name : getCacheNames()) {
            getCache(name);
        }
        return Collections.unmodifiableMap(cacheMap);
    }
}
