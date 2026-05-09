package com.project.ome.engine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class TradeEvent {

    private final UUID             tradeId;
    private final String           symbol;

    // Aggressor = the incoming order that caused the match
    private final UUID             aggressorOrderId;
    private final UUID             aggressorUserId;
    private final EngineOrder.Side aggressorSide;   // ← ONE field, ONE type

    // Resting = the order that was sitting in the book
    private final UUID             restingOrderId;
    private final UUID             restingUserId;

    private final BigDecimal       price;
    private final BigDecimal       quantity;
    private final Instant          executedAt;
}