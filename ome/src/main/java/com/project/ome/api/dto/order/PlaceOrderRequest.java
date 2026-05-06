// src/main/java/com/project/ome/api/dto/order/PlaceOrderRequest.java
package com.project.ome.api.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlaceOrderRequest {

    @NotBlank(message = "Symbol is required")
    @Pattern(regexp = "^[A-Z]{2,10}-[A-Z]{2,10}$",
             message = "Symbol must be in format BASE-QUOTE e.g. BTC-USD")
    private String symbol;

    @NotNull(message = "Side is required")
    private OrderSide side;           // BUY | SELL

    @NotNull(message = "Order type is required")
    private OrderType type;           // LIMIT | MARKET

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00001", message = "Quantity must be greater than 0")
    @Digits(integer = 12, fraction = 8)
    private BigDecimal quantity;

    // Required for LIMIT orders, ignored for MARKET
    @DecimalMin(value = "0.00000001", message = "Price must be positive")
    @Digits(integer = 12, fraction = 8)
    private BigDecimal price;

    // Client-supplied idempotency key (optional but recommended)
    @Size(max = 64)
    private String clientOrderId;
}