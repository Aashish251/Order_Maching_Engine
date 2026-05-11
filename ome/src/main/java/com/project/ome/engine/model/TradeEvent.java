package com.project.ome.engine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEvent {

    private UUID             tradeId;
    private String           symbol;

    // Aggressor = the incoming order that caused the match
    private UUID             aggressorOrderId;
    private UUID             aggressorUserId;
    private EngineOrder.Side aggressorSide;

    // Resting = the order that was sitting in the book
    private UUID             restingOrderId;
    private UUID             restingUserId;

    private BigDecimal       price;
    private BigDecimal       quantity;
    private Instant          executedAt;
}