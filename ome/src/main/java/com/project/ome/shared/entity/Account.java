// src/main/java/com/project/ome/shared/entity/Account.java
package com.project.ome.shared.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "available_balance",
            nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance",
            nullable = false, precision = 20, scale = 8)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    // Optimistic locking — CRITICAL for concurrent balance updates
    @Version
    @Column(nullable = false)
    private Long version;

    // ── Business methods (keep logic in entity, not service) ──

    public void reserveFunds(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.reservedBalance  = this.reservedBalance.add(amount);
    }

    public void releaseReservedFunds(BigDecimal amount) {
        this.reservedBalance  = this.reservedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }

    public void deductReserved(BigDecimal amount) {
        this.reservedBalance = this.reservedBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.availableBalance = this.availableBalance.add(amount);
    }
}