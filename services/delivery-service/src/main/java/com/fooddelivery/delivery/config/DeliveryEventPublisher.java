package com.fooddelivery.delivery.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.delivery.entity.DeliveryAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public static final String EXCHANGE = "food.delivery.exchange";

    public void publishDeliveryAssigned(DeliveryAssignment assignment) {
        publish("delivery.assigned", Map.of(
                "assignmentId", assignment.getId().toString(),
                "orderId", assignment.getOrderId().toString(),
                "partnerId", assignment.getPartner().getId().toString()
        ));
    }

    public void publishStatusUpdated(DeliveryAssignment assignment) {
        publish("delivery.status.updated", Map.of(
                "assignmentId", assignment.getId().toString(),
                "orderId", assignment.getOrderId().toString(),
                "status", assignment.getStatus()
        ));
    }

    public void publishDeliveryCompleted(DeliveryAssignment assignment) {
        publish("delivery.completed", Map.of(
                "orderId", assignment.getOrderId().toString(),
                "assignmentId", assignment.getId().toString()
        ));
    }

    private void publish(String eventType, Object data) {
        Map<String, Object> event = Map.of("type", eventType, "data", data);
        rabbitTemplate.convertAndSend(EXCHANGE, "delivery.event", event);
        log.info("Published delivery event: {}", eventType);
    }
}
