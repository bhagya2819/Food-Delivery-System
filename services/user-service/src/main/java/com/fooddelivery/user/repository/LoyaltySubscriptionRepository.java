package com.fooddelivery.user.repository;

import com.fooddelivery.user.entity.LoyaltySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltySubscriptionRepository extends JpaRepository<LoyaltySubscription, UUID> {

    Optional<LoyaltySubscription> findByUserIdAndStatus(UUID userId, String status);

    List<LoyaltySubscription> findByStatusAndEndDateBefore(String status, Instant date);

    List<LoyaltySubscription> findByStatusAndAutoRenewTrueAndEndDateBefore(String status, Instant date);
}
