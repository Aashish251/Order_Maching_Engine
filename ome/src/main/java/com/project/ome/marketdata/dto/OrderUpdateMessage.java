// src/main/java/com/project/ome/marketdata/dto/OrderUpdateMessage.java
package com.project.ome.marketdata.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Sent privately to the order owner when their order is filled
@Getter
@Builder
@AllArgsConstructor
public class OrderUpdateMessage {
    private final UUID       orderId;
    private final String     symbol;
    private final String     status;
    private final BigDecimal filledQty;
    private final BigDecimal remainingQty;
    private final BigDecimal lastFillPrice;
    private final Instant    timestamp;
}