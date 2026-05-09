// src/main/java/com/project/ome/engine/disruptor/OrderCommandEvent.java
package com.project.ome.engine.disruptor;

import com.project.ome.engine.model.EngineOrder;
import lombok.Getter;
import lombok.Setter;

// This is the "slot" in the ring buffer
// Disruptor reuses these objects — never allocate new ones per event
@Getter
@Setter
public class OrderCommandEvent {

    private EngineOrder order;
    private CommandType type;

    public enum CommandType {
        PLACE,   // new order → match
        CANCEL   // cancel → remove from book
    }

    // Called by Disruptor to reset the slot for reuse
    public void clear() {
        this.order = null;
        this.type  = null;
    }
}