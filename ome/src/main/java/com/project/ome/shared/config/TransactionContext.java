// src/main/java/com/project/ome/shared/config/TransactionContext.java
package com.project.ome.shared.config;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class TransactionContext {
    public static boolean isReadOnly() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }
}