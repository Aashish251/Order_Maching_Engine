// src/main/java/com/project/ome/engine/model/OrderBook.java
package com.project.ome.engine.model;

import lombok.Getter;
import java.math.BigDecimal;
import java.util.*;

@Getter
public class OrderBook {

    private final String symbol;

    // BID side: highest price first (buyers want to pay as little as possible
    // but we show best/highest bid first)
    private final TreeMap<BigDecimal, Deque<EngineOrder>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    // ASK side: lowest price first (sellers want highest but best ask is lowest)
    private final TreeMap<BigDecimal, Deque<EngineOrder>> asks =
            new TreeMap<>(Comparator.naturalOrder());

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    // ── Add order to book ──────────────────────────────────────
    public void addOrder(EngineOrder order) {
        TreeMap<BigDecimal, Deque<EngineOrder>> side =
                order.getSide() == EngineOrder.Side.BUY ? bids : asks;

        side.computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>())
            .addLast(order);   // FIFO within same price level
    }

    // ── Remove order from book (cancellation) ─────────────────
    public boolean removeOrder(EngineOrder order) {
        TreeMap<BigDecimal, Deque<EngineOrder>> side =
                order.getSide() == EngineOrder.Side.BUY ? bids : asks;

        Deque<EngineOrder> level = side.get(order.getPrice());
        if (level == null) return false;

        boolean removed = level.removeIf(
                o -> o.getOrderId().equals(order.getOrderId()));

        // Clean up empty price levels
        if (level.isEmpty()) side.remove(order.getPrice());
        return removed;
    }

    // ── Snapshot for API/WebSocket ─────────────────────────────
    public BigDecimal getBestBid() {
        return bids.isEmpty() ? null : bids.firstKey();
    }

    public BigDecimal getBestAsk() {
        return asks.isEmpty() ? null : asks.firstKey();
    }

    public boolean canMatch() {
        if (bids.isEmpty() || asks.isEmpty()) return false;
        return getBestBid().compareTo(getBestAsk()) >= 0;
    }
}