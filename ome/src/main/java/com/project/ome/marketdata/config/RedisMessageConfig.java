// src/main/java/com/project/ome/marketdata/config/RedisMessageConfig.java
package com.project.ome.marketdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.*;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import com.project.ome.marketdata.RedisMarketDataSubscriber;

@Configuration
public class RedisMessageConfig {

    // All app nodes subscribe to this Redis channel
    // When any node publishes a trade, ALL nodes receive it
    // and push to their own local WebSocket clients
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listenerAdapter,
                new PatternTopic("market:*")); // subscribe to all symbols
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(
            RedisMarketDataSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}