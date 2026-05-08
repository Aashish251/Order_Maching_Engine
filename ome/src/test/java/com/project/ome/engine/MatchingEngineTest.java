// src/test/java/com/project/ome/engine/MatchingEngineTest.java
package com.project.ome.engine;

import com.project.ome.engine.core.MatchingEngine;
import com.project.ome.engine.model.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MatchingEngineTest {

    private MatchingEngine       engine;
    private List<TradeEvent>     trades;
    private List<OrderBookUpdateEvent> bookUpdates;

    @BeforeEach
    void setUp() {
        trades      = new ArrayList<>();
        bookUpdates = new ArrayList<>();
        engine = new MatchingEngine(
                "BTC-USD",
                trades::add,
                bookUpdates::add
        );
    }

    @Test
    @DisplayName("No match when book is empty")
    void noMatchOnEmptyBook() {
        engine.process(buyLimit("order-1", "65000", "1.0"));
        assertThat(trades).isEmpty();
        assertThat(engine.getOrderBook().getBids()).hasSize(1);
    }

    @Test
    @DisplayName("Exact match — full fill both sides")
    void exactMatch() {
        engine.process(buyLimit("buy-1", "65000", "1.0"));
        engine.process(sellLimit("sell-1", "65000", "1.0"));

        assertThat(trades).hasSize(1);
        TradeEvent trade = trades.get(0);
        assertThat(trade.getPrice())
                .isEqualByComparingTo(new BigDecimal("65000"));
        assertThat(trade.getQuantity())
                .isEqualByComparingTo(new BigDecimal("1.0"));

        // Book should be empty after full fill
        assertThat(engine.getOrderBook().getBids()).isEmpty();
        assertThat(engine.getOrderBook().getAsks()).isEmpty();
    }

    @Test
    @DisplayName("Partial fill — incoming smaller than resting")
    void partialFill() {
        engine.process(buyLimit("buy-1", "65000", "2.0"));  // resting
        engine.process(sellLimit("sell-1", "65000", "0.5")); // aggressor

        assertThat(trades).hasSize(1);
        assertThat(trades.get(0).getQuantity())
                .isEqualByComparingTo(new BigDecimal("0.5"));

        // BUY order should still be in book with 1.5 remaining
        Deque<EngineOrder> level =
                engine.getOrderBook().getBids()
                      .get(new BigDecimal("65000"));
        assertThat(level).hasSize(1);
        assertThat(level.peekFirst().getRemainingQty())
                .isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("Price priority — best ask filled first")
    void pricePriority() {
    // Two sell orders at different prices
    engine.process(sellLimit("sell-high", "65100", "1.0"));
    engine.process(sellLimit("sell-low",  "65000", "1.0")); // better ask

    // Buy order should match sell-low first (lowest ask = best ask)
    engine.process(buyLimit("buy-1", "65200", "1.0"));

    assertThat(trades).hasSize(1);

    // Fill price must be 65000 (the lower/better ask price)
    assertThat(trades.get(0).getPrice())
            .isEqualByComparingTo(new BigDecimal("65000"));

    // Resting order must be sell-low — use uuid() helper, NOT hardcoded string
    assertThat(trades.get(0).getRestingOrderId())
            .isEqualTo(uuid("sell-low"));   // ← THIS is the fix
    }

    @Test
    @DisplayName("Time priority — earlier order filled first at same price")
    void timePriority() {
        // Two sell orders at same price — first in, first out
        engine.process(sellLimit("sell-first",  "65000", "0.5"));
        engine.process(sellLimit("sell-second", "65000", "0.5"));

        engine.process(buyLimit("buy-1", "65000", "0.5"));

        assertThat(trades).hasSize(1);
        // sell-first should be filled, not sell-second
        assertThat(trades.get(0).getRestingOrderId())
                .isEqualTo(uuid("sell-first"));
    }

    @Test
    @DisplayName("No match when prices don't cross")
    void noMatchWhenPricesDoNotCross() {
        engine.process(buyLimit("buy-1",  "64900", "1.0")); // bid below ask
        engine.process(sellLimit("sell-1","65100", "1.0")); // ask above bid

        assertThat(trades).isEmpty();
        assertThat(engine.getOrderBook().getBids()).hasSize(1);
        assertThat(engine.getOrderBook().getAsks()).hasSize(1);
    }

    @Test
    @DisplayName("Sweep through multiple price levels")
    void sweepMultipleLevels() {
        // Three sell orders at increasing prices
        engine.process(sellLimit("sell-1", "65000", "0.5"));
        engine.process(sellLimit("sell-2", "65100", "0.5"));
        engine.process(sellLimit("sell-3", "65200", "0.5"));

        // Large buy order that crosses all three levels
        engine.process(buyLimit("buy-big", "65500", "1.5"));

        assertThat(trades).hasSize(3);
        assertThat(engine.getOrderBook().getAsks()).isEmpty();
        assertThat(engine.getOrderBook().getBids()).isEmpty();
    }

    // ── Helper methods ─────────────────────────────────────────

    private EngineOrder buyLimit(String id, String price, String qty) {
        return EngineOrder.builder()
                .orderId(uuid(id))
                .userId(UUID.randomUUID())
                .symbol("BTC-USD")
                .side(EngineOrder.Side.BUY)
                .type(EngineOrder.Type.LIMIT)
                .price(new BigDecimal(price))
                .remainingQty(new BigDecimal(qty))
                .timestamp(Instant.now())
                .build();
    }

    private EngineOrder sellLimit(String id, String price, String qty) {
        return EngineOrder.builder()
                .orderId(uuid(id))
                .userId(UUID.randomUUID())
                .symbol("BTC-USD")
                .side(EngineOrder.Side.SELL)
                .type(EngineOrder.Type.LIMIT)
                .price(new BigDecimal(price))
                .remainingQty(new BigDecimal(qty))
                .timestamp(Instant.now())
                .build();
    }

    private UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
}