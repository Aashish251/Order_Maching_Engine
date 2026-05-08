// src/main/java/com/project/ome/engine/core/EngineInitializer.java
package com.project.ome.engine.core;

import com.project.ome.shared.repository.InstrumentRepository;
import com.project.ome.shared.repository.OrderRepository;
import com.project.ome.engine.model.OrderBook;  
import com.project.ome.engine.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineInitializer implements ApplicationRunner {

    private final MatchingEngineRegistry engineRegistry;
    private final InstrumentRepository   instrumentRepository;
    private final OrderRepository        orderRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing matching engines...");

        instrumentRepository.findByTradingEnabledTrue()
                .forEach(instrument -> {
                    engineRegistry.registerInstrument(
                            instrument.getSymbol(),
                            this::handleTrade,
                            this::handleBookUpdate
                    );

                    // Reload open orders into memory on restart
                    reloadOpenOrders(instrument.getSymbol());
                });

        log.info("All matching engines initialized.");
    }

    private void reloadOpenOrders(String symbol) {
        var openOrders = orderRepository.findOpenOrdersBySymbol(symbol);
        if (openOrders.isEmpty()) return;

        MatchingEngine engine = engineRegistry.getEngine(symbol);
        openOrders.forEach(order -> {
            EngineOrder engineOrder = EngineOrder.builder()
                    .orderId(order.getId())
                    .userId(order.getUser().getId())
                    .symbol(symbol)
                    .side(EngineOrder.Side.valueOf(order.getSide().name()))
                    .type(EngineOrder.Type.valueOf(order.getType().name()))
                    .price(order.getPrice())
                    .remainingQty(order.getRemainingQty())
                    .timestamp(order.getCreatedAt())
                    .build();

            // Add directly to book, don't match (already validated)
            engine.getOrderBook().addOrder(engineOrder);
        });

        log.info("Reloaded {} open orders for {}", openOrders.size(), symbol);
    }

    // Temporary handlers — replaced by real publishers in Week 3
    private void handleTrade(TradeEvent event) {
        log.info("TRADE EXECUTED: {} {} @ {} qty={}",
                event.getSymbol(),
                event.getAggressorSide(),
                event.getPrice(),
                event.getQuantity());
    }

    private void handleBookUpdate(OrderBookUpdateEvent event) {
        log.debug("BOOK UPDATE: {} bids={} asks={}",
                event.getSymbol(),
                event.getBids().size(),
                event.getAsks().size());
    }
}