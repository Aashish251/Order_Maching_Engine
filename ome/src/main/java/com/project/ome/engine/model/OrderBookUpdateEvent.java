// src/main/java/com/project/ome/engine/model/OrderBookUpdateEvent.java
package com.project.ome.engine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Emitted after every order book change — for WebSocket clients
@Getter
@Builder
@AllArgsConstructor
public class OrderBookUpdateEvent {

    private final String           symbol;
    private final List<PriceLevel> bids;
    private final List<PriceLevel> asks;
    private final Instant          timestamp;
    private final long             sequenceNumber;   // monotonically increasing

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PriceLevel {
        private final BigDecimal price;
        private final BigDecimal totalQuantity;
        private final int        orderCount;
    }
}