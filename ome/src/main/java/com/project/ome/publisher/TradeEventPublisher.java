// src/main/java/com/project/ome/publisher/TradeEventPublisher.java
package com.project.ome.publisher;

import com.project.ome.engine.model.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.trade-executed}")
    private String tradeExecutedTopic;

    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishFallback")
    public void publish(TradeEvent event) {
        kafkaTemplate.send(
                tradeExecutedTopic,
                event.getSymbol(),
                event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish trade {}: {}",
                            event.getTradeId(), ex.getMessage());
                    throw new RuntimeException(ex);
                } else {
                    log.debug("Trade published: {} partition={}",
                            event.getTradeId(),
                            result.getRecordMetadata().partition());
                }
            });
    }

    // Fallback: store to local queue for retry (simplified version)
    private void publishFallback(TradeEvent event, Exception ex) {
        log.error("Kafka circuit open — trade {} will be retried later: {}",
                event.getTradeId(), ex.getMessage());
        // In production: persist to outbox table for guaranteed delivery
        // For now: log the trade ID for manual recovery
    }
}