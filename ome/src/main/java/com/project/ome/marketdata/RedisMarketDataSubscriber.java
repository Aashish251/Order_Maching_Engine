// src/main/java/com/project/ome/marketdata/RedisMarketDataSubscriber.java
package com.project.ome.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ome.marketdata.dto.OrderBookMessage;
import com.project.ome.marketdata.dto.TradeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMarketDataSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper          objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body    = new String(message.getBody());

            // channel format: market:BTC-USD:orderbook
            //                 market:BTC-USD:trades
            String[] parts  = channel.split(":");
            String symbol   = parts[1];
            String dataType = parts[2];

            if ("orderbook".equals(dataType)) {
                OrderBookMessage ob = objectMapper
                        .readValue(body, OrderBookMessage.class);
                messagingTemplate.convertAndSend(
                        "/topic/" + symbol + "/orderbook", ob);

            } else if ("trades".equals(dataType)) {
                TradeMessage trade = objectMapper
                        .readValue(body, TradeMessage.class);
                messagingTemplate.convertAndSend(
                        "/topic/" + symbol + "/trades", trade);
            }
        } catch (Exception e) {
            log.error("Redis message processing failed: {}", e.getMessage());
        }
    }
}