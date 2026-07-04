package com.fooddelivery.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public static final String EXCHANGE = "food.delivery.exchange";

    public void publishOrderPlaced(Order order) {
        publish("order.placed", Map.of(
                "orderId", order.getId().toString(),
                "userId", order.getUserId().toString(),
                "restaurantId", order.getRestaurantId().toString(),
                "totalAmount", order.getTotalAmount().toString()
        ));
    }

    public void publishOrderCancelled(Order order) {
        publish("order.cancelled", Map.of(
                "orderId", order.getId().toString(),
                "paymentId", order.getPaymentId() != null ? order.getPaymentId().toString() : null
        ));
    }

    private void publish(String eventType, Object data) {
        Map<String, Object> event = Map.of("type", eventType, "data", data);
        rabbitTemplate.convertAndSend(EXCHANGE, "order.event", event);
        log.info("Published order event: {}", eventType);
    }
}
