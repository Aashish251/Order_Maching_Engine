// src/main/java/com/project/ome/engine/disruptor/OrderCommandEventHandler.java
package com.project.ome.engine.disruptor;

import com.lmax.disruptor.EventHandler;
import com.project.ome.engine.core.MatchingEngineRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// This runs on the single engine thread — processes one event at a time
@Slf4j
@RequiredArgsConstructor
public class OrderCommandEventHandler
        implements EventHandler<OrderCommandEvent> {

    private final MatchingEngineRegistry engineRegistry;

    @Override
    public void onEvent(OrderCommandEvent event,
                        long sequence,
                        boolean endOfBatch) {

        if (event.getOrder() == null) return;

        String symbol = event.getOrder().getSymbol();

        try {
            switch (event.getType()) {
                case PLACE -> {
                    if (engineRegistry.hasEngine(symbol)) {
                        engineRegistry.getEngine(symbol)
                                      .process(event.getOrder());
                    }
                }
                case CANCEL -> {
                    if (engineRegistry.hasEngine(symbol)) {
                        engineRegistry.getEngine(symbol)
                                      .cancel(event.getOrder());
                    }
                }
            }
        } catch (Exception e) {
            // NEVER let an exception kill the engine thread
            log.error("Engine error processing order {}: {}",
                    event.getOrder().getOrderId(), e.getMessage(), e);
        } finally {
            event.clear();  // reset slot for reuse
        }
    }
}