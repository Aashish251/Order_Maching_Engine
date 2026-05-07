// src/main/java/com/project/ome/api/service/OrderService.java
package com.project.ome.api.service;

import com.project.ome.api.dto.order.*;
import com.project.ome.shared.dto.PageResponse;
import com.project.ome.shared.entity.*;
import com.project.ome.shared.exception.*;
import com.project.ome.shared.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository      orderRepository;
    private final AccountRepository    accountRepository;
    private final InstrumentRepository instrumentRepository;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, UUID userId) {

        // 1. Idempotency check — same clientOrderId = return existing order
        if (request.getClientOrderId() != null) {
            var existing = orderRepository
                    .findByClientOrderId(request.getClientOrderId());
            if (existing.isPresent()) {
                log.info("Duplicate order detected, returning existing: {}",
                         request.getClientOrderId());
                return toResponse(existing.get());
            }
        }

        // 2. Validate instrument
        Instrument instrument = instrumentRepository
                .findBySymbol(request.getSymbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument", request.getSymbol()));

        if (!instrument.isTradingEnabled()) {
            throw BusinessException.instrumentDisabled(request.getSymbol());
        }

        // 3. Validate and reserve balance
        User userRef = new User();
        userRef.setId(userId);   // proxy — avoid full user load

        reserveBalance(userId, request, instrument);

        // 4. Persist order as PENDING
        Order order = Order.builder()
                .user(userRef)
                .instrument(instrument)
                .clientOrderId(request.getClientOrderId())
                .side(Order.Side.valueOf(request.getSide().name()))
                .type(Order.Type.valueOf(request.getType().name()))
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .status(Order.Status.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order placed: {} {} {} @ {} qty={}",
                 saved.getId(), saved.getSide(), saved.getInstrument().getSymbol(),
                 saved.getPrice(), saved.getQuantity());

        // Phase 3 (Week 3): publish to Disruptor here
        // engineGateway.submit(OrderCommand.from(saved));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", orderId.toString()));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrders(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.<OrderResponse>builder()
                .content(orders.getContent().stream().map(this::toResponse).toList())
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .last(orders.isLast())
                .build();
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", orderId.toString()));

        if (!order.isCancellable()) {
            throw BusinessException.orderNotCancellable(order.getStatus().name());
        }

        // Release reserved funds
        releaseReservedBalance(userId, order);

        order.setStatus(Order.Status.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    // ── Private helpers ──────────────────────────────────────────

    private void reserveBalance(UUID userId, PlaceOrderRequest req,
                                Instrument instrument) {
        // For BUY orders: reserve quote currency (USD)
        // For SELL orders: reserve base currency (BTC)
        String currency = req.getSide() == OrderSide.BUY
                ? instrument.getQuoteCurrency()
                : instrument.getBaseCurrency();

        BigDecimal amount = req.getSide() == OrderSide.BUY
                ? req.getPrice().multiply(req.getQuantity())
                : req.getQuantity();

        Account account = accountRepository
                .findByUserIdAndCurrencyForUpdate(userId, currency)
                .orElseThrow(() -> new BusinessException(
                        "ACCOUNT_NOT_FOUND",
                        "No " + currency + " account found",
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY));

        try {
            account.reserveFunds(amount);
            accountRepository.save(account);
        } catch (IllegalStateException e) {
            throw BusinessException.insufficientBalance();
        }
    }

    private void releaseReservedBalance(UUID userId, Order order) {
        String currency = order.getSide() == Order.Side.BUY
                ? order.getInstrument().getQuoteCurrency()
                : order.getInstrument().getBaseCurrency();

        BigDecimal amount = order.getSide() == Order.Side.BUY
                ? order.getPrice().multiply(order.getRemainingQty())
                : order.getRemainingQty();

        accountRepository
                .findByUserIdAndCurrencyForUpdate(userId, currency)
                .ifPresent(account -> {
                    account.releaseReservedFunds(amount);
                    accountRepository.save(account);
                });
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .symbol(o.getInstrument().getSymbol())
                .side(o.getSide().name())
                .type(o.getType().name())
                .status(o.getStatus().name())
                .quantity(o.getQuantity())
                .filledQty(o.getFilledQty())
                .remainingQty(o.getRemainingQty())
                .price(o.getPrice())
                .avgFillPrice(o.getAvgFillPrice())
                .clientOrderId(o.getClientOrderId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}