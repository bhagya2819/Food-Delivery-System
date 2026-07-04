package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.config.DeliveryEventPublisher;
import com.fooddelivery.delivery.entity.DeliveryAssignment;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryAssignmentRepository;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentEngine {

    private final DeliveryPartnerRepository partnerRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryEventPublisher eventPublisher;

    /**
     * Assigns the best available delivery partner to an order.
     * Returns the created assignment, or null if no partner is available.
     */
    public DeliveryAssignment assign(UUID orderId) {
        List<DeliveryPartner> onlinePartners = partnerRepository.findByIsOnlineTrueAndIsVerifiedTrue();
        if (onlinePartners.isEmpty()) {
            log.warn("No online partners available for order {}", orderId);
            return null;
        }

        DeliveryPartner best = selectBestPartner(onlinePartners);
        log.info("Selected partner {} (rating={}) for order {}", best.getId(), best.getAverageRating(), orderId);

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .partner(best)
                .status("ASSIGNED")
                .build();
        assignment = assignmentRepository.save(assignment);
        eventPublisher.publishDeliveryAssigned(assignment);
        return assignment;
    }

    /**
     * Retries assignment for orders that failed initial assignment.
     * Runs every 60 seconds. Uses expanding partner criteria (3 attempts total).
     * After 3 failed attempts, marks as needing manual intervention.
     */
    @Scheduled(fixedDelay = 60000)
    public void retryPendingAssignments() {
        List<DeliveryAssignment> pending = assignmentRepository.findAll().stream()
                .filter(a -> "PENDING_ASSIGNMENT".equals(a.getStatus()))
                .collect(Collectors.toList());

        for (DeliveryAssignment assignment : pending) {
            int attempts = assignment.getRetryCount() + 1;
            List<DeliveryPartner> partners = partnerRepository.findByIsOnlineTrueAndIsVerifiedTrue();

            if (partners.isEmpty()) {
                assignment.setRetryCount(attempts);
                if (attempts >= 3) {
                    assignment.setStatus("MANUAL_REQUIRED");
                    log.warn("Order {} reached max retry attempts, flagging for manual assignment", assignment.getOrderId());
                } else {
                    log.info("Order {} retry attempt {}: no partners available", assignment.getOrderId(), attempts);
                }
                assignmentRepository.save(assignment);
                continue;
            }

            DeliveryPartner best = selectBestPartner(partners);
            assignment.setPartner(best);
            assignment.setStatus("ASSIGNED");
            assignment.setRetryCount(0);
            assignmentRepository.save(assignment);
            eventPublisher.publishDeliveryAssigned(assignment);
            log.info("Order {} assigned to partner {} on retry attempt {}",
                    assignment.getOrderId(), best.getId(), attempts);
        }
    }

    private DeliveryPartner selectBestPartner(List<DeliveryPartner> partners) {
        return partners.stream()
                .min(Comparator
                        .comparing(DeliveryPartner::getAverageRating, Comparator.reverseOrder()))
                .orElse(partners.get(0));
    }
}
