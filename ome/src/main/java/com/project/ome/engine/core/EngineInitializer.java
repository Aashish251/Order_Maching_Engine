package com.project.ome.engine.core;

import com.project.ome.engine.model.*;
import com.project.ome.marketdata.MarketDataPublisher;
import com.project.ome.publisher.TradeEventPublisher;
import com.project.ome.shared.observability.MetricsService;
import com.project.ome.shared.repository.*;
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
    private final TradeEventPublisher    tradeEventPublisher;
    private final MetricsService         metricsService;
    private final MarketDataPublisher    marketDataPublisher;  // ← ADD

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing matching engines...");

        instrumentRepository.findByTradingEnabledTrue()
                .forEach(instrument -> {
                    engineRegistry.registerInstrument(
                            instrument.getSymbol(),
                            tradeEventPublisher::publish,
                            this::handleBookUpdate
                    );
                    reloadOpenOrders(instrument.getSymbol());
                });

        log.info("Matching engines initialized for {} instruments.",
                instrumentRepository.findByTradingEnabledTrue().size());
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
                    .timestamp(order.getCreatedAt() != null
                            ? order.getCreatedAt() : Instant.now())
                    .build();
            engine.getOrderBook().addOrder(engineOrder);
        });

        log.info("Reloaded {} open orders for {}", openOrders.size(), symbol);
    }

    private void handleBookUpdate(OrderBookUpdateEvent event) {
        marketDataPublisher.publishOrderBook(event);
        metricsService.updateOrderBookDepth(
                event.getBids().size(),
                event.getAsks().size());
    }
}
