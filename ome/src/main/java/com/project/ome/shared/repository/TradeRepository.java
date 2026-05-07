// src/main/java/com/project/ome/shared/repository/TradeRepository.java
package com.project.ome.shared.repository;

import com.project.ome.shared.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    Page<Trade> findByInstrumentSymbolOrderByExecutedAtDesc(
            String symbol, Pageable pageable);
}