// src/main/java/com/project/ome/shared/repository/TradeLegRepository.java
package com.project.ome.shared.repository;

import com.project.ome.shared.entity.TradeLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TradeLegRepository extends JpaRepository<TradeLeg, UUID> {
    List<TradeLeg> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}