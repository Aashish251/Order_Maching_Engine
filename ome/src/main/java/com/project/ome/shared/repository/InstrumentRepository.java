// src/main/java/com/project/ome/shared/repository/InstrumentRepository.java
package com.project.ome.shared.repository;

import com.project.ome.shared.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    Optional<Instrument> findBySymbol(String symbol);
    List<Instrument> findByTradingEnabledTrue();
}