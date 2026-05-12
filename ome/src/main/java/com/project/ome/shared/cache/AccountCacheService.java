// src/main/java/com/project/ome/shared/cache/AccountCacheService.java
package com.project.ome.shared.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Short TTL — balance changes frequently after trades
    private static final Duration BALANCE_TTL = Duration.ofSeconds(30);

    private String balanceKey(UUID userId, String currency) {
        return "balance:" + userId + ":" + currency;
    }

    public void cacheBalance(UUID userId, String currency,
                             BigDecimal available) {
        redisTemplate.opsForValue().set(
                balanceKey(userId, currency),
                available.toPlainString(),
                BALANCE_TTL);
    }

    public BigDecimal getCachedBalance(UUID userId, String currency) {
        Object val = redisTemplate.opsForValue()
                .get(balanceKey(userId, currency));
        if (val != null) {
            return new BigDecimal(val.toString());
        }
        return null;  // cache miss — caller loads from DB
    }

    // Invalidate after any trade settlement
    public void invalidate(UUID userId, String currency) {
        redisTemplate.delete(balanceKey(userId, currency));
    }

    public void invalidateAll(UUID userId) {
        redisTemplate.delete(balanceKey(userId, "USD"));
        redisTemplate.delete(balanceKey(userId, "BTC"));
        redisTemplate.delete(balanceKey(userId, "ETH"));
    }
}