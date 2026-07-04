package com.fooddelivery.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.delivery.config.DeliveryEventPublisher;
import com.fooddelivery.delivery.dto.AssignmentResponse;
import com.fooddelivery.delivery.dto.StatusUpdateRequest;
import com.fooddelivery.delivery.dto.TrackingResponse;
import com.fooddelivery.delivery.entity.DeliveryAssignment;
import com.fooddelivery.delivery.entity.DeliveryLocation;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.delivery.repository.DeliveryLocationRepository;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryLocationRepository locationRepository;
    private final AssignmentEngine assignmentEngine;
    private final DeliveryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final Set<String> VALID_TRANSITIONS_ASSIGNED = Set.of("ACCEPTED", "REJECTED");
    private static final Set<String> VALID_TRANSITIONS_ACCEPTED = Set.of("PICKED_UP");
    private static final Set<String> VALID_TRANSITIONS_PICKED_UP = Set.of("DELIVERED");

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

    @Transactional
    public AssignmentResponse updateStatus(UUID assignmentId, StatusUpdateRequest request) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Delivery assignment", assignmentId));

        String newStatus = request.getStatus().toUpperCase();

        switch (assignment.getStatus()) {
            case "ASSIGNED" -> {
                if (!VALID_TRANSITIONS_ASSIGNED.contains(newStatus)) {
                    throw new BadRequestException("Cannot transition from ASSIGNED to " + newStatus);
                }
            }
            case "ACCEPTED" -> {
                if (!VALID_TRANSITIONS_ACCEPTED.contains(newStatus)) {
                    throw new BadRequestException("Cannot transition from ACCEPTED to " + newStatus);
                }
            }
            case "PICKED_UP" -> {
                if (!VALID_TRANSITIONS_PICKED_UP.contains(newStatus)) {
                    throw new BadRequestException("Cannot transition from PICKED_UP to " + newStatus);
                }
            }
            case "DELIVERED", "REJECTED" -> throw new BadRequestException(
                    "Assignment is in terminal state: " + assignment.getStatus());
            default -> throw new BadRequestException(
                    "Cannot transition from " + assignment.getStatus());
        }

        assignment.setStatus(newStatus);
        switch (newStatus) {
            case "ACCEPTED" -> assignment.setAcceptedAt(Instant.now());
            case "PICKED_UP" -> assignment.setPickedUpAt(Instant.now());
            case "DELIVERED" -> assignment.setDeliveredAt(Instant.now());
            case "REJECTED" -> {
                assignmentRepository.save(assignment);
                eventPublisher.publishStatusUpdated(assignment);
                assignmentEngine.assign(assignment.getOrderId());
                return toResponse(assignment);
            }
        }

        assignment = assignmentRepository.save(assignment);
        eventPublisher.publishStatusUpdated(assignment);

        if ("DELIVERED".equals(newStatus)) {
            eventPublisher.publishDeliveryCompleted(assignment);
        }

        return toResponse(assignment);
    }

    public AssignmentResponse getAssignment(UUID assignmentId) {
        return toResponse(assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Delivery assignment", assignmentId)));
    }

    @Transactional
    public void updatePartnerLocation(UUID partnerId, Double latitude, Double longitude) {
        DeliveryPartner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Delivery partner", partnerId));

        partner.setCurrentLatitude(latitude);
        partner.setCurrentLongitude(longitude);
        partnerRepository.save(partner);

        var activeAssignment = assignmentRepository.findAll().stream()
                .filter(a -> a.getPartner() != null && a.getPartner().getId().equals(partnerId)
                        && Set.of("ASSIGNED", "ACCEPTED", "PICKED_UP").contains(a.getStatus()))
                .findFirst();

        activeAssignment.ifPresent(assignment -> {
            DeliveryLocation loc = DeliveryLocation.builder()
                    .assignment(assignment)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            locationRepository.save(loc);
        });

        log.info("Updated location for partner {}: {}, {}", partnerId, latitude, longitude);
    }

    public TrackingResponse getTracking(UUID orderId) {
        var assignment = assignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Delivery assignment for order", orderId));

        var partner = assignment.getPartner();
        if (partner == null) {
            return TrackingResponse.builder()
                    .currentLatitude(0.0).currentLongitude(0.0)
                    .estimatedArrivalMinutes(-1).partnerName("Not yet assigned")
                    .build();
        }

        long eta = estimateEta(partner.getCurrentLatitude(), partner.getCurrentLongitude());
        return TrackingResponse.builder()
                .currentLatitude(partner.getCurrentLatitude())
                .currentLongitude(partner.getCurrentLongitude())
                .estimatedArrivalMinutes(eta)
                .partnerName("Partner-" + partner.getId().toString().substring(0, 8))
                .build();
    }

    private long estimateEta(Double lat, Double lng) {
        if (lat == null || lng == null) return -1;
        double distanceKm = 2.0;
        return Math.round((distanceKm / 20.0) * 60);
    }

    private AssignmentResponse toResponse(DeliveryAssignment a) {
        return AssignmentResponse.builder()
                .id(a.getId()).orderId(a.getOrderId())
                .partnerId(a.getPartner() != null ? a.getPartner().getId() : null)
                .status(a.getStatus()).assignedAt(a.getAssignedAt())
                .acceptedAt(a.getAcceptedAt()).pickedUpAt(a.getPickedUpAt())
                .deliveredAt(a.getDeliveredAt())
                .build();
    }
}
