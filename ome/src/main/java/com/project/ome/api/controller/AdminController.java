// src/main/java/com/project/ome/api/controller/AdminController.java
package com.project.ome.api.controller;

import com.project.ome.shared.cache.InstrumentCacheService;
import com.project.ome.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")    // ← entire controller is admin-only
public class AdminController {

    private final InstrumentCacheService instrumentCacheService;

    @PostMapping("/cache/instruments/evict/{symbol}")
    public ApiResponse<String> evictInstrument(@PathVariable String symbol) {
        instrumentCacheService.evict(symbol);
        return ApiResponse.ok("Cache evicted for: " + symbol);
    }

    @PostMapping("/cache/instruments/warmup")
    public ApiResponse<String> warmUpCache() {
        instrumentCacheService.warmUp();
        return ApiResponse.ok("Cache warmed up successfully");
    }
}