// src/main/java/com/project/ome/publisher/TradeEventConsumer.java
package com.project.ome.publisher;

import com.project.ome.engine.model.EngineOrder;
import com.project.ome.engine.model.TradeEvent;
import com.project.ome.engine.model.EngineOrder;
import com.project.ome.shared.entity.*;
import com.project.ome.shared.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final TradeRepository    tradeRepository;
    private final TradeLegRepository tradeLegRepository;
    private final OrderRepository    orderRepository;

    @KafkaListener(
        topics   = "${app.kafka.topics.trade-executed}",
        groupId  = "trade-persistence-group"
    )
    @Transactional
    public void consume(TradeEvent event) {
        log.info("Persisting trade: {} {} @ {} qty={}",
                event.getTradeId(), event.getSymbol(),
                event.getPrice(), event.getQuantity());

        try {
            // 1. Find the instrument reference
            Instrument instrument = new Instrument();
            instrument.setId(resolveInstrumentId(event.getSymbol()));

            // 2. Persist the trade
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

            // 4. Persist resting leg (opposite side)
            Order.Side restingSide = event.getAggressorSide()
                    == EngineOrder.Side.BUY                // ← CORRECT
                    ? Order.Side.SELL : Order.Side.BUY;
            persistRestingLeg(savedTrade, event.getRestingOrderId(),
                               restingSide, event);

            // 5. Update order statuses
            updateOrderStatus(event.getAggressorOrderId(), event);
            updateOrderStatus(event.getRestingOrderId(), event);

        } catch (Exception e) {
            log.error("Failed to persist trade {}: {}",
                    event.getTradeId(), e.getMessage(), e);
            // In production: push to dead letter topic
        }
    }

    private void persistLeg(Trade trade, UUID orderId,
                        EngineOrder.Side side, TradeEvent event) {   // ← changed here
    Order orderRef = new Order();
    orderRef.setId(orderId);

    TradeLeg leg = TradeLeg.builder()
            .trade(trade)
            .order(orderRef)
            .side(side == EngineOrder.Side.BUY                       // ← changed here
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

    private UUID resolveInstrumentId(String symbol) {
        // Simplified — in production cache this
        return UUID.randomUUID();
    }
}