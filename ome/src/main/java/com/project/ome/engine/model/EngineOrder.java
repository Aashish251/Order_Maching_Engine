// src/main/java/com/project/ome/engine/model/EngineOrder.java
package com.project.ome.engine.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Pure domain object — no JPA, no Spring
@Getter
@Builder
@AllArgsConstructor
public class EngineOrder {

    private final UUID       orderId;
    private final UUID       userId;
    private final String     symbol;
    private final Side       side;
    private final Type       type;
    private final BigDecimal price;
    private BigDecimal       remainingQty;   // mutable — changes on fills
    private final Instant    timestamp;      // for time priority

    public enum Side { BUY, SELL }
    public enum Type { LIMIT, MARKET }

    public void reduceQuantity(BigDecimal fillQty) {
        this.remainingQty = this.remainingQty.subtract(fillQty);
    }

    public boolean isFilled() {
        return remainingQty.compareTo(BigDecimal.ZERO) == 0;
    }
}