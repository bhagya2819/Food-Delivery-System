package com.fooddelivery.restaurant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestaurantEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public static final String EXCHANGE = "food.delivery.exchange";

    public void publishRestaurantCreated(Object restaurantData) {
        publish("restaurant.created", restaurantData);
    }

    public void publishRestaurantUpdated(Object restaurantData) {
        publish("restaurant.updated", restaurantData);
    }

    public void publishMenuItemCreated(Object itemData) {
        publish("menu.item.created", itemData);
    }

    public void publishMenuItemUpdated(Object itemData) {
        publish("menu.item.updated", itemData);
    }

    private void publish(String eventType, Object data) {
        try {
            Map<String, Object> event = Map.of("type", eventType, "data", data);
            String routingKey = getRoutingKey(eventType);
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event: " + eventType, e);
        }
    }

    private String getRoutingKey(String eventType) {
        if (eventType.startsWith("restaurant")) return "restaurant.event";
        if (eventType.startsWith("menu")) return "restaurant.event";
        return "restaurant.event";
    }
}
