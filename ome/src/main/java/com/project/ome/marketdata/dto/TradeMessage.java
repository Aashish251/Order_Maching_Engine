// src/main/java/com/project/ome/marketdata/dto/TradeMessage.java
package com.project.ome.marketdata.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class TradeMessage {
    private final UUID       tradeId;
    private final String     symbol;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final String     side;       // aggressor side
    private final Instant    executedAt;
}