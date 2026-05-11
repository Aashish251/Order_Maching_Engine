// src/main/java/com/project/ome/marketdata/MarketDataPublisher.java
package com.project.ome.marketdata;

import com.project.ome.engine.model.OrderBookUpdateEvent;
import com.project.ome.engine.model.TradeEvent;
import com.project.ome.marketdata.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // ── Called by EngineInitializer on every order book change ──
    public void publishOrderBook(OrderBookUpdateEvent event) {
        OrderBookMessage message = OrderBookMessage.builder()
                .symbol(event.getSymbol())
                .bids(event.getBids().stream()
                        .map(l -> OrderBookMessage.PriceLevel.builder()
                                .price(l.getPrice())
                                .quantity(l.getTotalQuantity())
                                .orderCount(l.getOrderCount())
                                .build())
                        .collect(Collectors.toList()))
                .asks(event.getAsks().stream()
                        .map(l -> OrderBookMessage.PriceLevel.builder()
                                .price(l.getPrice())
                                .quantity(l.getTotalQuantity())
                                .orderCount(l.getOrderCount())
                                .build())
                        .collect(Collectors.toList()))
                .timestamp(event.getTimestamp())
                .sequence(event.getSequenceNumber())
                .build();

        // Broadcast to all subscribers of this symbol's order book
        String destination = "/topic/" + event.getSymbol() + "/orderbook";
        messagingTemplate.convertAndSend(destination, message);

        log.debug("Order book published: {} bids={} asks={}",
                event.getSymbol(),
                event.getBids().size(),
                event.getAsks().size());
    }

    // ── Called by TradeEventConsumer after a trade executes ────
    public void publishTrade(TradeEvent event) {
        TradeMessage message = TradeMessage.builder()
                .tradeId(event.getTradeId())
                .symbol(event.getSymbol())
                .price(event.getPrice())
                .quantity(event.getQuantity())
                .side(event.getAggressorSide().name())
                .executedAt(event.getExecutedAt())
                .build();

        // Broadcast to all subscribers of this symbol's trade feed
        String destination = "/topic/" + event.getSymbol() + "/trades";
        messagingTemplate.convertAndSend(destination, message);

        log.debug("Trade published to WebSocket: {} @ {}",
                event.getSymbol(), event.getPrice());
    }

    // ── Called for private order fill notifications ─────────────
    public void publishOrderUpdate(String userId, OrderUpdateMessage message) {
        // /user/{userId}/queue/orders — only that user receives this
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/orders",
                message);

        log.debug("Order update sent to user {}: order={} status={}",
                userId, message.getOrderId(), message.getStatus());
    }
}