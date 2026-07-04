package com.fooddelivery.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.delivery.entity.DeliveryAssignment;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
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

    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
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
                triggerAssignment(orderId);
            }
        } catch (Exception e) {
            log.error("Failed to process delivery order event: {}", e.getMessage());
        }
    }

    private void triggerAssignment(UUID orderId) {
        var onlinePartners = partnerRepository.findByIsOnlineTrueAndIsVerifiedTrue();
        if (onlinePartners.isEmpty()) {
            log.warn("No online delivery partners available for order {}", orderId);
            return;
        }

        DeliveryPartner selected = onlinePartners.get(0);
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .partner(selected)
                .status("ASSIGNED")
                .build();
        assignmentRepository.save(assignment);
        log.info("Delivery partner {} assigned to order {}", selected.getId(), orderId);
    }
}
