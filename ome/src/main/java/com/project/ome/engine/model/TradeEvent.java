// src/main/java/com/project/ome/engine/model/TradeEvent.java
package com.project.ome.engine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Emitted by the engine when a match occurs
@Getter
@Builder
@AllArgsConstructor
public class TradeEvent {

    private final UUID       tradeId;
    private final String     symbol;

    // Aggressor = the incoming order that caused the match
    private final UUID       aggressorOrderId;
    private final UUID       aggressorUserId;
    private final EngineOrder.Side aggressorSide;

    // Resting = the order that was sitting in the book
    private final UUID       restingOrderId;
    private final UUID       restingUserId;

    private final BigDecimal price;       // fill price = resting order price
    private final BigDecimal quantity;    // fill quantity
    private final Instant    executedAt;
}