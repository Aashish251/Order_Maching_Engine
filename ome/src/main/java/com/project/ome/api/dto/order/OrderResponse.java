// src/main/java/com/project/ome/api/dto/order/OrderResponse.java
package com.project.ome.api.dto.order;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {
    private final UUID       id;
    private final String     symbol;
    private final String     side;
    private final String     type;
    private final String     status;
    private final BigDecimal quantity;
    private final BigDecimal filledQty;
    private final BigDecimal remainingQty;
    private final BigDecimal price;
    private final BigDecimal avgFillPrice;
    private final String     clientOrderId;
    private final Instant    createdAt;
    private final Instant    updatedAt;
}