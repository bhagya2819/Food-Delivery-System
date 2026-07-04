package com.fooddelivery.user.service;

import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.user.dto.SubscriptionRequest;
import com.fooddelivery.user.dto.SubscriptionResponse;
import com.fooddelivery.user.entity.LoyaltySubscription;
import com.fooddelivery.user.repository.LoyaltySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final LoyaltySubscriptionRepository repository;

    private static final Set<String> VALID_PLANS = Set.of("MONTHLY", "QUARTERLY", "ANNUAL");

    @Transactional
    public SubscriptionResponse subscribe(UUID userId, SubscriptionRequest request) {
        if (!VALID_PLANS.contains(request.getPlanType().toUpperCase())) {
            throw new BadRequestException("Invalid plan type: " + request.getPlanType());
        }

        repository.findByUserIdAndStatus(userId, "ACTIVE").ifPresent(s -> {
            throw new BadRequestException("User already has an active subscription");
        });

        String planType = request.getPlanType().toUpperCase();
        int months = switch (planType) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "ANNUAL" -> 12;
            default -> 1;
        };

        LoyaltySubscription sub = LoyaltySubscription.builder()
                .userId(userId)
                .planType(planType)
                .status("ACTIVE")
                .startDate(Instant.now())
                .endDate(Instant.now().plus(months * 30L, ChronoUnit.DAYS))
                .autoRenew(request.getAutoRenew())
                .build();

        sub = repository.save(sub);
        log.info("Subscription created: {} plan={} user={}", sub.getId(), planType, userId);
        return toResponse(sub);
    }

    public SubscriptionResponse getSubscription(UUID userId) {
        return repository.findByUserIdAndStatus(userId, "ACTIVE")
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Active subscription for user", userId));
    }

    @Transactional
    public SubscriptionResponse cancel(UUID subscriptionId) {
        LoyaltySubscription sub = repository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription", subscriptionId));
        sub.setStatus("CANCELLED");
        sub.setAutoRenew(false);
        sub = repository.save(sub);
        log.info("Subscription cancelled: {}", subscriptionId);
        return toResponse(sub);
    }

    private SubscriptionResponse toResponse(LoyaltySubscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId()).userId(s.getUserId())
                .planType(s.getPlanType()).status(s.getStatus())
                .startDate(s.getStartDate()).endDate(s.getEndDate())
                .autoRenew(s.getAutoRenew()).createdAt(s.getCreatedAt())
                .build();
    }
}
