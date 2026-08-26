package com.bookmyshow.service;

import com.bookmyshow.cache.StatsTrackingCache;
import com.bookmyshow.cache.StatsTrackingCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        String cacheType = "UNKNOWN";
        if (cacheManager instanceof StatsTrackingCacheManager stcm) {
            cacheType = stcm.getCacheType();
        }
        stats.put("cacheType", cacheType);

        long totalHits = 0;
        long totalMisses = 0;
        long totalEvictions = 0;

        List<Map<String, Object>> cacheDetails = new ArrayList<>();

        if (cacheManager instanceof StatsTrackingCacheManager stcm) {
            Map<String, StatsTrackingCache> trackedCaches = stcm.getTrackedCaches();
            for (Map.Entry<String, StatsTrackingCache> entry : trackedCaches.entrySet()) {
                String name = entry.getKey();
                StatsTrackingCache cache = entry.getValue();

                long h = cache.getHits();
                long m = cache.getMisses();
                long e = cache.getEvictions();

                totalHits += h;
                totalMisses += m;
                totalEvictions += e;

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("name", name);
                detail.put("hits", h);
                detail.put("misses", m);
                detail.put("evictions", e);
                long totalReqs = h + m;
                double ratio = totalReqs > 0 ? ((double) h / totalReqs) * 100.0 : 0.0;
                detail.put("hitRatio", String.format("%.2f%%", ratio));

                cacheDetails.add(detail);
            }
        } else {
            for (String name : cacheManager.getCacheNames()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("name", name);
                detail.put("status", "Active (Stats unmonitored)");
                cacheDetails.add(detail);
            }
        }

        stats.put("totalHits", totalHits);
        stats.put("totalMisses", totalMisses);
        stats.put("totalEvictions", totalEvictions);
        long totalRequests = totalHits + totalMisses;
        double overallRatio = totalRequests > 0 ? ((double) totalHits / totalRequests) * 100.0 : 0.0;
        stats.put("overallHitRatio", String.format("%.2f%%", overallRatio));
        stats.put("caches", cacheDetails);

        // Architectural explanation
        Map<String, String> explanation = new LinkedHashMap<>();
        explanation.put("Cache Hit", "Occurs when requested data is found in the cache memory (Redis or Embedded). The data is returned instantly in sub-milliseconds without executing slow database queries or external API network requests.");
        explanation.put("Cache Miss", "Occurs when requested data is NOT present in the cache. The application executes the underlying method (e.g., querying MySQL or TMDB), stores the resulting value in the cache for future requests, and returns it to the caller.");
        explanation.put("Cache Eviction", "The process of removing stale or invalidated entries from cache memory. In CineX, whenever an Admin updates or deletes a movie, show, or theatre, @CacheEvict triggers to wipe the outdated data so subsequent requests fetch fresh data.");
        explanation.put("TTL (Time-To-Live)", "The maximum duration a cache entry remains valid before it automatically expires and is evicted by Redis. For example, TMDB movie feeds have a 60-minute TTL, while show times expire every 15 minutes.");
        stats.put("explanation", explanation);

        return stats;
    }

    public void evictAllCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    public void evictCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
