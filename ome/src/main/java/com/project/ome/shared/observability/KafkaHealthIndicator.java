// src/main/java/com/project/ome/shared/observability/KafkaHealthIndicator.java
package com.project.ome.shared.observability;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.*;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(
                kafkaAdmin.getConfigurationProperties())) {

            // Check if we can list topics within 3 seconds
            var topics = client.listTopics()
                    .names()
                    .get(3, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("topics", topics.size())
                    .withDetail("broker", "localhost:9092")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Kafka unavailable: " + e.getMessage())
                    .build();
        }
    }
}