// src/main/java/com/project/ome/shared/cache/InstrumentCacheService.java
package com.project.ome.shared.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.ome.shared.entity.Instrument;
import com.project.ome.shared.repository.InstrumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentCacheService {

    private final InstrumentRepository instrumentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "instrument:";
    private static final Duration REDIS_TTL      = Duration.ofHours(1);

    // L1 — in-JVM Caffeine (sub-microsecond, ~100 entries max)
    private final Cache<String, Instrument> l1Cache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

    // Pre-warm both caches on startup
    @PostConstruct
    public void warmUp() {
        log.info("Warming up instrument cache...");
        instrumentRepository.findByTradingEnabledTrue()
                .forEach(instrument -> {
                    l1Cache.put(instrument.getSymbol(), instrument);
                    redisTemplate.opsForValue().set(
                            REDIS_KEY_PREFIX + instrument.getSymbol(),
                            instrument,
                            REDIS_TTL);
                });
        log.info("Instrument cache warmed: {} instruments", l1Cache.estimatedSize());
    }

    // Read: L1 → L2 → DB (write-back on miss)
    public Optional<Instrument> findBySymbol(String symbol) {
        // L1 hit — fastest path
        Instrument cached = l1Cache.getIfPresent(symbol);
        if (cached != null) return Optional.of(cached);

        // L2 hit — Redis
        Object redisVal = redisTemplate.opsForValue()
                .get(REDIS_KEY_PREFIX + symbol);
        if (redisVal instanceof Instrument instrument) {
            l1Cache.put(symbol, instrument);  // backfill L1
            return Optional.of(instrument);
        }

        // DB miss — load and cache
        return instrumentRepository.findBySymbol(symbol)
                .map(instrument -> {
                    l1Cache.put(symbol, instrument);
                    redisTemplate.opsForValue().set(
                            REDIS_KEY_PREFIX + symbol,
                            instrument, REDIS_TTL);
                    log.debug("Cache miss for instrument: {}", symbol);
                    return instrument;
                });
    }

    // Invalidate on instrument update (admin action)
    public void evict(String symbol) {
        l1Cache.invalidate(symbol);
        redisTemplate.delete(REDIS_KEY_PREFIX + symbol);
        log.info("Cache evicted for instrument: {}", symbol);
    }
}