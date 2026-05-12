// src/main/java/com/project/ome/publisher/TradeEventConsumer.java
package com.project.ome.publisher;

import com.project.ome.engine.model.EngineOrder;
import com.project.ome.engine.model.TradeEvent;
import com.project.ome.marketdata.MarketDataPublisher;
import com.project.ome.marketdata.dto.OrderUpdateMessage;
import com.project.ome.shared.cache.AccountCacheService;
import com.project.ome.shared.entity.*;
import com.project.ome.shared.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final TradeRepository      tradeRepository;
    private final TradeLegRepository   tradeLegRepository;
    private final OrderRepository      orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final AccountRepository    accountRepository;
    private final MarketDataPublisher marketDataPublisher;
    private final AccountCacheService accountCacheService;

    @KafkaListener(
    topics   = "${app.kafka.topics.trade-executed}",
    groupId  = "trade-persistence-group"
)
@Transactional
public void consume(TradeEvent event) {
    log.info("Persisting trade: {} {} @ {} qty={}",
            event.getTradeId(), event.getSymbol(),
            event.getPrice(), event.getQuantity());

    // 1. Find instrument
    Instrument instrument = instrumentRepository.findBySymbol(event.getSymbol())
            .orElseThrow(() -> new IllegalArgumentException(
                    "Instrument not found: " + event.getSymbol()));

    // 2. Persist trade
    Trade trade = Trade.builder()
            .instrument(instrument)
            .price(event.getPrice())
            .quantity(event.getQuantity())
            .executedAt(event.getExecutedAt())
            .build();
    Trade savedTrade = tradeRepository.save(trade);

    // 3. Persist aggressor leg
    persistLeg(savedTrade, event.getAggressorOrderId(),
               event.getAggressorSide(), event);

    // 4. Persist resting leg
    Order.Side restingSide = event.getAggressorSide() == EngineOrder.Side.BUY
            ? Order.Side.SELL : Order.Side.BUY;
    persistRestingLeg(savedTrade, event.getRestingOrderId(), restingSide, event);

    // 5. Update order statuses
    updateOrderStatus(event.getAggressorOrderId(), event);
    updateOrderStatus(event.getRestingOrderId(), event);

    // 6. Settle balances
    settleBalances(event, instrument);

    // 7. Publish WebSocket trade + order fill notifications  ← INSIDE consume()
    marketDataPublisher.publishTrade(event);
    publishOrderFillNotifications(event);

    log.info("Trade {} fully persisted and settled", event.getTradeId());
}

   private void publishOrderFillNotifications(TradeEvent event) {
    // Notify aggressor
    orderRepository.findById(event.getAggressorOrderId())
            .ifPresent(order -> {
                OrderUpdateMessage msg = OrderUpdateMessage.builder()
                        .orderId(order.getId())
                        .symbol(event.getSymbol())
                        .status(order.getStatus().name())
                        .filledQty(order.getFilledQty())
                        .remainingQty(order.getRemainingQty())
                        .lastFillPrice(event.getPrice())
                        .timestamp(event.getExecutedAt())
                        .build();
                marketDataPublisher.publishOrderUpdate(
                        event.getAggressorUserId().toString(), msg);
            });

    // Notify resting order owner
    orderRepository.findById(event.getRestingOrderId())
            .ifPresent(order -> {
                OrderUpdateMessage msg = OrderUpdateMessage.builder()
                        .orderId(order.getId())
                        .symbol(event.getSymbol())
                        .status(order.getStatus().name())
                        .filledQty(order.getFilledQty())
                        .remainingQty(order.getRemainingQty())
                        .lastFillPrice(event.getPrice())
                        .timestamp(event.getExecutedAt())
                        .build();
                marketDataPublisher.publishOrderUpdate(
                        event.getRestingUserId().toString(), msg);
            });

    log.info("Order fill notifications sent for aggressor={} resting={}",
            event.getAggressorOrderId(), event.getRestingOrderId());
}

    private void persistLeg(Trade trade, UUID orderId,
                        EngineOrder.Side side, TradeEvent event) {
        Order orderRef = new Order();
        orderRef.setId(orderId);

        TradeLeg leg = TradeLeg.builder()
                .trade(trade)
                .order(orderRef)
                .side(side == EngineOrder.Side.BUY
                        ? Order.Side.BUY : Order.Side.SELL)
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .build();
        tradeLegRepository.save(leg);
    }

    private void persistRestingLeg(Trade trade, UUID orderId,
                                   Order.Side side, TradeEvent event) {
        Order orderRef = new Order();
        orderRef.setId(orderId);

        TradeLeg leg = TradeLeg.builder()
                .trade(trade)
                .order(orderRef)
                .side(side)
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .build();
        tradeLegRepository.save(leg);
    }

    private void updateOrderStatus(UUID orderId, TradeEvent event) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.applyFill(event.getQuantity(), event.getPrice());
            orderRepository.save(order);
        });
    }

    private void settleBalances(TradeEvent event, Instrument instrument) {
        BigDecimal cost = event.getPrice().multiply(event.getQuantity());

        UUID buyerId;
        UUID sellerId;

        if (event.getAggressorSide() == EngineOrder.Side.BUY) {
            buyerId  = event.getAggressorUserId();
            sellerId = event.getRestingUserId();
        } else {
            buyerId  = event.getRestingUserId();
            sellerId = event.getAggressorUserId();
        }

        // Buyer: deduct quote currency from reserved, credit base currency
        accountRepository.findByUserIdAndCurrencyForUpdate(
                buyerId, instrument.getQuoteCurrency())
            .ifPresent(acc -> {
                acc.deductReserved(cost);
                accountRepository.save(acc);
            });

        accountRepository.findByUserIdAndCurrencyForUpdate(
                buyerId, instrument.getBaseCurrency())
            .ifPresent(acc -> {
                acc.credit(event.getQuantity());
                accountRepository.save(acc);
            });

        // Seller: deduct base currency from reserved, credit quote currency
        accountRepository.findByUserIdAndCurrencyForUpdate(
                sellerId, instrument.getBaseCurrency())
            .ifPresent(acc -> {
                acc.deductReserved(event.getQuantity());
                accountRepository.save(acc);
            });

        accountRepository.findByUserIdAndCurrencyForUpdate(
                sellerId, instrument.getQuoteCurrency())
            .ifPresent(acc -> {
                acc.credit(cost);
                accountRepository.save(acc);
            });

        log.info("Balances settled: buyer={} seller={} cost={} qty={}",
                buyerId, sellerId, cost, event.getQuantity());

        accountCacheService.invalidateAll(buyerId);
        accountCacheService.invalidateAll(sellerId);
        log.debug("Balance cache invalidated for buyer={} seller={}", buyerId, sellerId);
    }
}