// src/main/java/com/project/ome/shared/ratelimit/RateLimitService.java
package com.project.ome.shared.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object>  redisTemplate;
    private final DefaultRedisScript<Long>        rateLimitScript;

    // Check + consume a token atomically via Lua script
    public boolean isAllowed(String key, RateLimitConfig config) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(config.getCapacity()),
                    String.valueOf(config.getRefillRatePerSecond()),
                    String.valueOf(System.currentTimeMillis()),
                    "1"  // cost = 1 token per request
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            // On Redis failure — fail open (allow request)
            // This is a deliberate trade-off: availability > strict rate limiting
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    public boolean isOrderAllowed(String userId) {
        return isAllowed("ratelimit:order:" + userId,
                RateLimitConfig.ORDER_LIMIT);
    }

    public boolean isAuthAllowed(String ip) {
        return isAllowed("ratelimit:auth:" + ip,
                RateLimitConfig.AUTH_LIMIT);
    }
}