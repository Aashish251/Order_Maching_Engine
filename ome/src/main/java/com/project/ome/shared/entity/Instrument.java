// src/main/java/com/project/ome/shared/entity/Instrument.java
package com.project.ome.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 10)
    private String quoteCurrency;

    @Column(name = "tick_size", nullable = false, precision = 20, scale = 8)
    private BigDecimal tickSize;

    @Column(name = "lot_size", nullable = false, precision = 20, scale = 8)
    private BigDecimal lotSize;

    @Column(name = "min_order_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal minOrderValue;

    @Column(name = "trading_enabled", nullable = false)
    @Builder.Default
    private boolean tradingEnabled = true;
}