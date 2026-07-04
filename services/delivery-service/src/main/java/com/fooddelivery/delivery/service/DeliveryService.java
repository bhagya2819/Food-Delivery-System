package com.fooddelivery.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.delivery.entity.DeliveryAssignment;
import com.fooddelivery.delivery.repository.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryAssignmentRepository assignmentRepository;
    private final AssignmentEngine assignmentEngine;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "delivery.order.events")
    public void handleOrderPlaced(Map<String, Object> message) {
        try {
            JsonNode event = objectMapper.valueToTree(message);
            String type = event.get("type").asText();

            if ("order.placed".equals(type)) {
                JsonNode data = event.get("data");
                UUID orderId = UUID.fromString(data.get("orderId").asText());
                log.info("Received order.placed for order {}, triggering assignment", orderId);

                DeliveryAssignment assignment = assignmentEngine.assign(orderId);
                if (assignment == null) {
                    DeliveryAssignment pending = DeliveryAssignment.builder()
                            .orderId(orderId)
                            .status("PENDING_ASSIGNMENT")
                            .build();
                    assignmentRepository.save(pending);
                    log.info("Order {} queued for retry assignment", orderId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process delivery order event: {}", e.getMessage());
        }
    }
}
