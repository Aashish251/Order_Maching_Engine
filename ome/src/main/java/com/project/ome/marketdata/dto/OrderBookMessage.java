// src/main/java/com/project/ome/marketdata/dto/OrderBookMessage.java
package com.project.ome.marketdata.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrderBookMessage {

    private final String           symbol;
    private final List<PriceLevel> bids;
    private final List<PriceLevel> asks;
    private final Instant          timestamp;
    private final long             sequence;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PriceLevel {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final int        orderCount;
    }
}