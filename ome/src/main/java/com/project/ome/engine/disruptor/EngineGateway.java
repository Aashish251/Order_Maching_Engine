// src/main/java/com/project/ome/engine/disruptor/EngineGateway.java
package com.project.ome.engine.disruptor;

import com.lmax.disruptor.dsl.Disruptor;
import com.project.ome.engine.model.EngineOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineGateway {

    private final Disruptor<OrderCommandEvent> disruptor;

    // Called by OrderService — publishes to ring buffer, returns immediately
    public void submitOrder(EngineOrder order) {
        disruptor.getRingBuffer().publishEvent(
                (event, sequence) -> {
                    event.setOrder(order);
                    event.setType(OrderCommandEvent.CommandType.PLACE);
                });
        log.debug("Order published to ring buffer: {}", order.getOrderId());
    }

    public void cancelOrder(EngineOrder order) {
        disruptor.getRingBuffer().publishEvent(
                (event, sequence) -> {
                    event.setOrder(order);
                    event.setType(OrderCommandEvent.CommandType.CANCEL);
                });
        log.debug("Cancel published to ring buffer: {}", order.getOrderId());
    }
}