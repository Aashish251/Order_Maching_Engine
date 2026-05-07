// src/main/java/com/project/ome/shared/entity/Trade.java
package com.project.ome.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trades")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {  

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private Instant executedAt = Instant.now();  // ← separate business field

    @OneToMany(mappedBy = "trade",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<TradeLeg> legs = new ArrayList<>();
}