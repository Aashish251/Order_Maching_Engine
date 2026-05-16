// src/main/java/com/project/ome/shared/observability/MetricsService.java
package com.project.ome.shared.observability;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class MetricsService {

    // ── Counters (always increasing) ──────────────────────────
    private final Counter ordersPlaced;
    private final Counter ordersCancelled;
    private final Counter tradesExecuted;
    private final Counter validationFailures;
    private final Counter rateLimitHits;

    // ── Gauges (current value) ─────────────────────────────────
    private final AtomicLong activeOrders  = new AtomicLong(0);
    private final AtomicLong openBids      = new AtomicLong(0);
    private final AtomicLong openAsks      = new AtomicLong(0);

    // ── Timers (latency distribution) ─────────────────────────
    private final Timer orderPlacementLatency;
    private final Timer matchingLatency;

    // ── Distribution Summaries (value distributions) ──────────
    private final DistributionSummary tradeQuantityDist;
    private final DistributionSummary tradePriceDist;

    public MetricsService(MeterRegistry registry) {

        // Counters
        ordersPlaced = Counter.builder("ome.orders.placed")
                .description("Total orders placed")
                .tag("type", "all")
                .register(registry);

        ordersCancelled = Counter.builder("ome.orders.cancelled")
                .description("Total orders cancelled")
                .register(registry);

        tradesExecuted = Counter.builder("ome.trades.executed")
                .description("Total trades executed")
                .register(registry);

        validationFailures = Counter.builder("ome.orders.validation.failures")
                .description("Order validation failures")
                .register(registry);

        rateLimitHits = Counter.builder("ome.rate.limit.hits")
                .description("Rate limit rejections")
                .register(registry);

        // Gauges
        Gauge.builder("ome.orders.active", activeOrders, AtomicLong::get)
                .description("Currently active orders")
                .register(registry);

        Gauge.builder("ome.orderbook.bids", openBids, AtomicLong::get)
                .description("Open bid levels in order book")
                .register(registry);

        Gauge.builder("ome.orderbook.asks", openAsks, AtomicLong::get)
                .description("Open ask levels in order book")
                .register(registry);

        // Timers
        orderPlacementLatency = Timer.builder("ome.order.placement.latency")
                .description("Time to process and place an order")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        matchingLatency = Timer.builder("ome.matching.latency")
                .description("Time for engine to match an order")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Distribution summaries
        tradeQuantityDist = DistributionSummary.builder("ome.trade.quantity")
                .description("Distribution of trade quantities")
                .register(registry);

        tradePriceDist = DistributionSummary.builder("ome.trade.price")
                .description("Distribution of trade prices")
                .register(registry);
    }

    // ── Public methods called from services ──────────────────

    public void recordOrderPlaced()       { ordersPlaced.increment(); activeOrders.incrementAndGet(); }
    public void recordOrderCancelled()    { ordersCancelled.increment(); activeOrders.decrementAndGet(); }
    public void recordOrderFilled()       { activeOrders.decrementAndGet(); }
    public void recordTradeExecuted(double qty, double price) {
        tradesExecuted.increment();
        tradeQuantityDist.record(qty);
        tradePriceDist.record(price);
    }
    public void recordValidationFailure() { validationFailures.increment(); }
    public void recordRateLimitHit()      { rateLimitHits.increment(); }

    public void updateOrderBookDepth(long bids, long asks) {
        openBids.set(bids);
        openAsks.set(asks);
    }

    public Timer.Sample startOrderTimer()    { return Timer.start(); }
    public Timer.Sample startMatchingTimer() { return Timer.start(); }

    public void stopOrderTimer(Timer.Sample sample) {
        sample.stop(orderPlacementLatency);
    }

    public void stopMatchingTimer(Timer.Sample sample) {
        sample.stop(matchingLatency);
    }
}