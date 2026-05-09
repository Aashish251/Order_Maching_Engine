// src/main/java/com/project/ome/publisher/TradeEventPublisher.java
package com.project.ome.publisher;

import com.project.ome.engine.model.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.trade-executed}")
    private String tradeExecutedTopic;

    // Called by engine via callback — must be fast, non-blocking
    public void publish(TradeEvent event) {
        kafkaTemplate.send(
                tradeExecutedTopic,
                event.getSymbol(),   // partition key — same symbol → same partition
                event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish trade event: {}",
                            event.getTradeId(), ex);
                } else {
                    log.debug("Trade published to Kafka: {} partition={}",
                            event.getTradeId(),
                            result.getRecordMetadata().partition());
                }
            });
    }
}