// src/main/java/com/project/ome/shared/repository/OrderRepository.java
package com.project.ome.shared.repository;

import com.project.ome.shared.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // User's paginated order history (uses idx_orders_user_created)
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // User's open orders (uses partial index idx_orders_user_status)
    @Query("""
        SELECT o FROM Order o
        WHERE o.user.id = :userId
          AND o.status IN ('OPEN', 'PARTIALLY_FILLED', 'PENDING')
        ORDER BY o.createdAt DESC
        """)
    List<Order> findActiveOrdersByUserId(UUID userId);

    // Used on engine restart to reload open orders into memory
    @Query("""
        SELECT o FROM Order o
        JOIN FETCH o.instrument i
        WHERE i.symbol = :symbol
          AND o.status IN ('OPEN', 'PARTIALLY_FILLED')
        ORDER BY o.createdAt ASC
        """)
    List<Order> findOpenOrdersBySymbol(String symbol);

    // Idempotency check
    Optional<Order> findByClientOrderId(String clientOrderId);

    // Ownership check before cancellation
    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);
}