// src/main/java/com/project/ome/shared/entity/Order.java
package com.project.ome.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "client_order_id", length = 64, unique = true)
    private String clientOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "filled_qty", nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal filledQty = BigDecimal.ZERO;

    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;                       // null for MARKET orders

    @Column(name = "avg_fill_price", precision = 20, scale = 8)
    private BigDecimal avgFillPrice;

    // Bug #5 fix: remaining_qty is a GENERATED ALWAYS column in the DB.
    // We map it read-only so Hibernate validation passes and JPA never
    // attempts to INSERT/UPDATE this column.
    @Column(name = "remaining_qty", insertable = false, updatable = false,
            precision = 20, scale = 8)
    private BigDecimal remainingQty;

    // ── Enums ──────────────────────────────────────────────────
    public enum Side   { BUY, SELL }
    public enum Type   { LIMIT, MARKET, STOP }
    public enum Status {
        PENDING, OPEN, PARTIALLY_FILLED,
        FILLED, CANCELLED, REJECTED
    }

    // ── Derived helpers ────────────────────────────────────────
    /**
     * Returns the remaining quantity. Prefers the DB-computed value if available,
     * otherwise computes it from quantity - filledQty (for transient/new entities).
     */
    public BigDecimal getRemainingQty() {
        if (remainingQty != null) {
            return remainingQty;
        }
        return quantity.subtract(filledQty);
    }

    public boolean isActive() {
        return status == Status.OPEN || status == Status.PARTIALLY_FILLED
                || status == Status.PENDING;
    }

    public boolean isCancellable() {
        return status == Status.OPEN || status == Status.PARTIALLY_FILLED
                || status == Status.PENDING;
    }

    // Called by matching engine after a fill
    public void applyFill(BigDecimal fillQty, BigDecimal fillPrice) {
        // Recalculate weighted average fill price
        if (this.avgFillPrice == null) {
            this.avgFillPrice = fillPrice;
        } else {
            BigDecimal totalCost = this.avgFillPrice.multiply(this.filledQty)
                    .add(fillPrice.multiply(fillQty));
            BigDecimal newFilledQty = this.filledQty.add(fillQty);
            this.avgFillPrice = totalCost.divide(newFilledQty, 8,
                    java.math.RoundingMode.HALF_UP);
        }
        this.filledQty = this.filledQty.add(fillQty);
        this.status = getRemainingQty().compareTo(BigDecimal.ZERO) == 0
                ? Status.FILLED : Status.PARTIALLY_FILLED;
    }
}