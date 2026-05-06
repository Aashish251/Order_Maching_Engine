// src/main/java/com/project/ome/shared/exception/GlobalExceptionHandler.java
package com.project.ome.shared.exception;

import com.project.ome.shared.dto.ApiError;
import com.project.ome.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (@Valid failures) ─────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null
                                ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a   // keep first error per field
                ));

        return ResponseEntity.badRequest().body(
                ApiResponse.error(ApiError.builder()
                        .code("VALIDATION_FAILED")
                        .message("Request validation failed")
                        .fields(fieldErrors)
                        .build()));
    }

    // ── Business rule violations ─────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(
                ApiResponse.error(ApiError.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .build()));
    }

    // ── Resource not found ───────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(ApiError.builder()
                        .code("NOT_FOUND")
                        .message(ex.getMessage())
                        .build()));
    }

    // ── Catch-all ────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(ApiError.builder()
                        .code("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .build()));
    }
}