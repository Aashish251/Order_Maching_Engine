// src/main/java/com/project/ome/engine/disruptor/OrderCommandEventFactory.java
package com.project.ome.engine.disruptor;

import com.lmax.disruptor.EventFactory;

// Disruptor uses this to pre-allocate all ring buffer slots at startup
public class OrderCommandEventFactory
        implements EventFactory<OrderCommandEvent> {

    @Override
    public OrderCommandEvent newInstance() {
        return new OrderCommandEvent();
    }
}