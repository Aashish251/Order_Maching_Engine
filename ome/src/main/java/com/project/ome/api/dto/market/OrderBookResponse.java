// src/main/java/com/project/ome/api/dto/market/OrderBookResponse.java
package com.project.ome.api.dto.market;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class OrderBookResponse {
    private final String          symbol;
    private final List<PriceLevel> bids;     // descending by price
    private final List<PriceLevel> asks;     // ascending by price
    private final Instant         timestamp;

    @Getter
    @Builder
    public static class PriceLevel {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final int        orderCount;
    }
}