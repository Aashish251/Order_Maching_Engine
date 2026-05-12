// src/main/java/com/project/ome/shared/ratelimit/RateLimitConfig.java
package com.project.ome.shared.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitConfig {

    private final int capacity;           // max tokens in bucket
    private final int refillRatePerSecond; // tokens added per second

    // 10 orders/sec per user, burst up to 20
    public static final RateLimitConfig ORDER_LIMIT =
            new RateLimitConfig(20, 10);

    // 5 auth attempts/min per IP (burst up to 10)
    public static final RateLimitConfig AUTH_LIMIT =
            new RateLimitConfig(10, 0);  // refillRate=0 means no auto-refill (sliding window)
}