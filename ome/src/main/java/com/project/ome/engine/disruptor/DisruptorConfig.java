// src/main/java/com/project/ome/engine/disruptor/DisruptorConfig.java
package com.project.ome.engine.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.project.ome.engine.core.MatchingEngineRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Configuration
public class DisruptorConfig {

    // Ring buffer size MUST be a power of 2
    // 1024 means 1024 pre-allocated slots in memory
    private static final int RING_BUFFER_SIZE = 1024;

    @Bean
    public Disruptor<OrderCommandEvent> disruptor(
            MatchingEngineRegistry engineRegistry) {

        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "matching-engine-thread");
            t.setDaemon(true);
            return t;
        };

        Disruptor<OrderCommandEvent> disruptor = new Disruptor<>(
                new OrderCommandEventFactory(),
                RING_BUFFER_SIZE,
                threadFactory,
                ProducerType.MULTI,         // multiple HTTP threads publish
                new BlockingWaitStrategy()  // simple, good for low latency
        );

        // Wire the handler — ONE handler = ONE engine thread
        disruptor.handleEventsWith(
                new OrderCommandEventHandler(engineRegistry));

        disruptor.start();
        log.info("Disruptor started with ring buffer size: {}",
                RING_BUFFER_SIZE);

        return disruptor;
    }
}