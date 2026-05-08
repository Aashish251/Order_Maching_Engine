// src/main/java/com/project/ome/engine/core/MatchingEngine.java
package com.project.ome.engine.core;

import com.project.ome.engine.model.EngineOrder;
import com.project.ome.engine.model.OrderBook;          // ← ADD THIS
import com.project.ome.engine.model.TradeEvent;
import com.project.ome.engine.model.OrderBookUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import java.math.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class MatchingEngine {

    private final OrderBook   orderBook;
    private final AtomicLong  sequenceCounter = new AtomicLong(0);

    // Callbacks — engine pushes events OUT, never calls infrastructure directly
    private final Consumer<TradeEvent>          onTrade;
    private final Consumer<OrderBookUpdateEvent> onBookUpdate;

    public OrderBook getOrderBook() {
        return orderBook;
    }
    public MatchingEngine(String symbol,
                          Consumer<TradeEvent> onTrade,
                          Consumer<OrderBookUpdateEvent> onBookUpdate) {
        this.orderBook   = new OrderBook(symbol);
        this.onTrade     = onTrade;
        this.onBookUpdate = onBookUpdate;
    }

    // ── Entry point — called by Disruptor event handler ────────
    // THIS METHOD MUST ONLY BE CALLED FROM ONE THREAD PER INSTRUMENT
    public void process(EngineOrder incomingOrder) {
        log.debug("Processing order: {} {} {} @ {} qty={}",
                incomingOrder.getOrderId(),
                incomingOrder.getSide(),
                incomingOrder.getSymbol(),
                incomingOrder.getPrice(),
                incomingOrder.getRemainingQty());

        List<TradeEvent> trades = match(incomingOrder);

        // If order still has remaining quantity → add to book
        if (!incomingOrder.isFilled()) {
            if (incomingOrder.getType() == EngineOrder.Type.LIMIT) {
                orderBook.addOrder(incomingOrder);
                log.debug("Order added to book: {} remaining={}",
                        incomingOrder.getOrderId(),
                        incomingOrder.getRemainingQty());
            }
            // MARKET orders that can't fill completely are cancelled
            // (no partial market order resting in book)
        }

        // Emit all trades
        trades.forEach(onTrade);

        // Always emit book update after any order processing
        onBookUpdate.accept(buildBookSnapshot());
    }

    public void cancel(EngineOrder order) {
        boolean removed = orderBook.removeOrder(order);
        if (removed) {
            log.debug("Order cancelled from book: {}", order.getOrderId());
            onBookUpdate.accept(buildBookSnapshot());
        }
    }

    // ── Core matching loop ─────────────────────────────────────
    private List<TradeEvent> match(EngineOrder incoming) {
        List<TradeEvent> trades = new ArrayList<>();

        // Get the opposite side of the book
        TreeMap<BigDecimal, Deque<EngineOrder>> oppositeBook =
                incoming.getSide() == EngineOrder.Side.BUY
                        ? orderBook.getAsks()
                        : orderBook.getBids();

        // Keep matching while:
        // 1. Incoming order still has remaining quantity
        // 2. Opposite side is not empty
        // 3. Price condition is met
        while (!incoming.isFilled() && !oppositeBook.isEmpty()) {

            // Best resting order on opposite side
            Map.Entry<BigDecimal, Deque<EngineOrder>> bestLevel =
                    oppositeBook.firstEntry();

            BigDecimal bestPrice    = bestLevel.getKey();
            Deque<EngineOrder> queue = bestLevel.getValue();

            // ── Price check ──────────────────────────────────────
            // BUY  order: incoming price >= best ask (willing to pay)
            // SELL order: incoming price <= best bid (willing to accept)
            if (!priceMatches(incoming, bestPrice)) {
                break;   // No more matches possible at this level
            }

            // ── Time priority: take from front of queue (FIFO) ──
            EngineOrder resting = queue.peekFirst();
            if (resting == null) {
                oppositeBook.pollFirstEntry();
                continue;
            }

            // ── Calculate fill quantity ──────────────────────────
            // Fill = min(incoming remaining, resting remaining)
            BigDecimal fillQty = incoming.getRemainingQty()
                    .min(resting.getRemainingQty());

            // ── Fill price = resting order price ────────────────
            // The aggressor (incoming) gets the resting price
            // This is standard price-time priority
            BigDecimal fillPrice = resting.getPrice();

            // ── Apply the fill ───────────────────────────────────
            incoming.reduceQuantity(fillQty);
            resting.reduceQuantity(fillQty);

            // ── Emit trade event ─────────────────────────────────
            TradeEvent trade = TradeEvent.builder()
                    .tradeId(UUID.randomUUID())
                    .symbol(incoming.getSymbol())
                    .aggressorOrderId(incoming.getOrderId())
                    .aggressorUserId(incoming.getUserId())
                    .aggressorSide(incoming.getSide())
                    .restingOrderId(resting.getOrderId())
                    .restingUserId(resting.getUserId())
                    .price(fillPrice)
                    .quantity(fillQty)
                    .executedAt(Instant.now())
                    .build();

            trades.add(trade);

            log.info("TRADE: {} {} @ {} qty={}",
                    incoming.getSymbol(), fillPrice, fillQty,
                    incoming.getOrderId());

            // ── Clean up filled resting order ────────────────────
            if (resting.isFilled()) {
                queue.pollFirst();                    // remove from queue
                if (queue.isEmpty()) {
                    oppositeBook.remove(bestPrice);   // remove empty level
                }
            }
        }

        return trades;
    }

    // ── Price match check ──────────────────────────────────────
    private boolean priceMatches(EngineOrder incoming, BigDecimal restingPrice) {
        if (incoming.getType() == EngineOrder.Type.MARKET) {
            return true;   // MARKET orders match at any price
        }
        if (incoming.getSide() == EngineOrder.Side.BUY) {
            // BUY: willing to pay incomingPrice, best ask is restingPrice
            // Match if we're willing to pay at least the ask price
            return incoming.getPrice().compareTo(restingPrice) >= 0;
        } else {
            // SELL: want at least incomingPrice, best bid is restingPrice
            // Match if the bid price is at least what we want
            return restingPrice.compareTo(incoming.getPrice()) >= 0;
        }
    }

    // ── Build book snapshot for WebSocket ─────────────────────
    private OrderBookUpdateEvent buildBookSnapshot() {
        return OrderBookUpdateEvent.builder()
                .symbol(orderBook.getSymbol())
                .bids(buildLevels(orderBook.getBids(), 10))
                .asks(buildLevels(orderBook.getAsks(), 10))
                .timestamp(Instant.now())
                .sequenceNumber(sequenceCounter.incrementAndGet())
                .build();
    }

    private List<OrderBookUpdateEvent.PriceLevel> buildLevels(
            TreeMap<BigDecimal, Deque<EngineOrder>> side, int depth) {

        List<OrderBookUpdateEvent.PriceLevel> levels = new ArrayList<>();
        int count = 0;

        for (Map.Entry<BigDecimal, Deque<EngineOrder>> entry
                : side.entrySet()) {
            if (count++ >= depth) break;

            BigDecimal totalQty = entry.getValue().stream()
                    .map(EngineOrder::getRemainingQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            levels.add(OrderBookUpdateEvent.PriceLevel.builder()
                    .price(entry.getKey())
                    .totalQuantity(totalQty)
                    .orderCount(entry.getValue().size())
                    .build());
        }
        return levels;
    }
}