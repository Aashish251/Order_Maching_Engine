// src/main/java/com/project/ome/shared/exception/BusinessException.java
package com.project.ome.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final String     errorCode;
    private final HttpStatus status;

    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    // Factory methods for common cases
    public static BusinessException insufficientBalance() {
        return new BusinessException(
            "INSUFFICIENT_BALANCE",
            "Account has insufficient available balance",
            HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException instrumentDisabled(String symbol) {
        return new BusinessException(
            "INSTRUMENT_DISABLED",
            "Trading is currently disabled for " + symbol,
            HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException orderNotCancellable(String status) {
        return new BusinessException(
            "ORDER_NOT_CANCELLABLE",
            "Order in status " + status + " cannot be cancelled",
            HttpStatus.CONFLICT);
    }

    public static BusinessException rateLimitExceeded() {
        return new BusinessException(
            "RATE_LIMIT_EXCEEDED",
            "Too many requests. Please slow down.",
            HttpStatus.TOO_MANY_REQUESTS);
    }
}