// src/main/java/com/project/ome/shared/entity/TradeLeg.java
package com.project.ome.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "trade_legs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeLeg extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Order.Side side;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;
}