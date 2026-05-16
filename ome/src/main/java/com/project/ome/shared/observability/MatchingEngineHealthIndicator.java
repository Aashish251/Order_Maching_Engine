// src/main/java/com/project/ome/shared/observability/MatchingEngineHealthIndicator.java
package com.project.ome.shared.observability;

import com.project.ome.engine.core.MatchingEngineRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingEngineHealthIndicator implements HealthIndicator {

    private final MatchingEngineRegistry engineRegistry;

    @Override
    public Health health() {
        try {
            var engines = engineRegistry.getAllEngines();
            if (engines.isEmpty()) {
                return Health.down()
                        .withDetail("error", "No matching engines registered")
                        .build();
            }

            Health.Builder builder = Health.up();
            engines.forEach((symbol, engine) -> {
                builder.withDetail(symbol + ".bids",
                        engine.getOrderBook().getBids().size());
                builder.withDetail(symbol + ".asks",
                        engine.getOrderBook().getAsks().size());
            });

            return builder
                    .withDetail("enginesRegistered", engines.size())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}