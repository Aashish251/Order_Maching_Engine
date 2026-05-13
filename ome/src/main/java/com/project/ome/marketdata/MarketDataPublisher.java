// src/main/java/com/project/ome/marketdata/MarketDataPublisher.java
package com.project.ome.marketdata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ome.engine.model.OrderBookUpdateEvent;
import com.project.ome.engine.model.TradeEvent;
import com.project.ome.marketdata.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper          objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

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
        try {
            // Publish to Redis — all nodes pick this up and push to their WS clients
            String channel = "market:" + event.getSymbol() + ":orderbook";
            redisTemplate.convertAndSend(channel,
                    objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // Fallback: direct push if Redis fails
            messagingTemplate.convertAndSend(
                    "/topic/" + event.getSymbol() + "/orderbook", message);
            log.warn("Redis publish failed, direct WS fallback: {}", e.getMessage());
        }
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
       try {
            String channel = "market:" + event.getSymbol() + ":trades";
            redisTemplate.convertAndSend(channel,
                    objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/" + event.getSymbol() + "/trades", message);
            log.warn("Redis publish failed, direct WS fallback: {}", e.getMessage());
        }
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

    private OrderBookMessage buildOrderBookMessage(OrderBookUpdateEvent event) {
        return OrderBookMessage.builder()
                .symbol(event.getSymbol())
                .bids(event.getBids().stream()
                        .map(l -> OrderBookMessage.PriceLevel.builder()
                                .price(l.getPrice())
                                .quantity(l.getTotalQuantity())
                                .orderCount(l.getOrderCount())
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .asks(event.getAsks().stream()
                        .map(l -> OrderBookMessage.PriceLevel.builder()
                                .price(l.getPrice())
                                .quantity(l.getTotalQuantity())
                                .orderCount(l.getOrderCount())
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .timestamp(event.getTimestamp())
                .sequence(event.getSequenceNumber())
                .build();
    }
}