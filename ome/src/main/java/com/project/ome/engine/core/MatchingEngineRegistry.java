// src/main/java/com/project/ome/engine/core/MatchingEngineRegistry.java
package com.project.ome.engine.core;

import com.project.ome.engine.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
public class MatchingEngineRegistry {

    // One engine per instrument — each runs in its own thread
    private final Map<String, MatchingEngine> engines =
            new ConcurrentHashMap<>();

    private Consumer<TradeEvent>           tradeHandler;
    private Consumer<OrderBookUpdateEvent> bookUpdateHandler;

    // Called on app startup for each instrument
    public void registerInstrument(String symbol,
                                   Consumer<TradeEvent> onTrade,
                                   Consumer<OrderBookUpdateEvent> onBookUpdate) {
        engines.computeIfAbsent(symbol, s -> {
            log.info("Registering matching engine for: {}", symbol);
            return new MatchingEngine(symbol, onTrade, onBookUpdate);
        });
    }

    public MatchingEngine getEngine(String symbol) {
        MatchingEngine engine = engines.get(symbol);
        if (engine == null) {
            throw new IllegalArgumentException(
                    "No matching engine for symbol: " + symbol);
        }
        return engine;
    }

    public boolean hasEngine(String symbol) {
        return engines.containsKey(symbol);
    }

    public Map<String, MatchingEngine> getAllEngines() {
        return Map.copyOf(engines);
    }
}