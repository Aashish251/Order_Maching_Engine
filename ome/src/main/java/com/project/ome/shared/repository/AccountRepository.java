// src/main/java/com/project/ome/shared/repository/AccountRepository.java
package com.project.ome.shared.repository;

import com.project.ome.shared.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);

    // Pessimistic write lock — used during balance reservation
    // We use pessimistic here as a safety net even with optimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.currency = :currency")
    Optional<Account> findByUserIdAndCurrencyForUpdate(UUID userId, String currency);

    // Fetch multiple accounts in a single query — avoids N+1
    @Query("""
            SELECT a FROM Account a
            WHERE a.user.id = :userId
            AND a.currency IN :currencies
            """)
    List<Account> findByUserIdAndCurrencies(UUID userId, List<String> currencies);
}