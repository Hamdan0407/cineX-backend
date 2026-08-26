package com.bookmyshow.controller;

import com.bookmyshow.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Cache API", description = "Operations related to Redis / Embedded Cache monitoring and eviction")
@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;
    private final com.bookmyshow.service.AdminAuthService adminAuthService;

    public CacheController(CacheService cacheService, com.bookmyshow.service.AdminAuthService adminAuthService) {
        this.cacheService = cacheService;
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "Get real-time cache statistics and architectural explanations")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        return ResponseEntity.ok(cacheService.getCacheStatistics());
    }

    @Operation(summary = "Evict all cache entries across the platform")
    @DeleteMapping("/evict-all")
    public ResponseEntity<String> evictAllCaches(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        cacheService.evictAllCaches();
        return ResponseEntity.ok("All caches evicted successfully");
    }

    @Operation(summary = "Evict entries for a specific cache")
    @DeleteMapping("/evict/{cacheName}")
    public ResponseEntity<String> evictCache(
            @PathVariable String cacheName,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        adminAuthService.validateAdmin(roleHeader, emailHeader);
        cacheService.evictCache(cacheName);
        return ResponseEntity.ok("Cache '" + cacheName + "' evicted successfully");
    }
}
