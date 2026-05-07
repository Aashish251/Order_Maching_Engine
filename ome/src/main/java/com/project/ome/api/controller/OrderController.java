// src/main/java/com/project/ome/api/controller/OrderController.java
package com.project.ome.api.controller;

import com.project.ome.api.dto.order.*;
import com.project.ome.api.service.OrderService;
import com.project.ome.shared.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Temporary: hardcoded userId until JWT filter is wired in Phase 6
    // Replace with: SecurityContextHolder → principal → userId
    private static final UUID TEMP_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request, TEMP_USER_ID);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Order accepted for matching", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrder(orderId, TEMP_USER_ID);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<OrderResponse> response =
                orderService.getOrders(TEMP_USER_ID, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID orderId) {
        OrderResponse response = orderService.cancelOrder(orderId, TEMP_USER_ID);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Order cancellation accepted", response));
    }
}