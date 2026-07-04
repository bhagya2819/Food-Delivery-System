package com.fooddelivery.user.service;

import com.fooddelivery.user.entity.LoyaltySubscription;
import com.fooddelivery.user.repository.LoyaltySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRenewalService {

    private final LoyaltySubscriptionRepository repository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void renewExpiringSubscriptions() {
        Instant threeDaysFromNow = Instant.now().plus(3, ChronoUnit.DAYS);
        List<LoyaltySubscription> expiring = repository
                .findByStatusAndAutoRenewTrueAndEndDateBefore("ACTIVE", threeDaysFromNow);

        for (LoyaltySubscription sub : expiring) {
            int months = switch (sub.getPlanType()) {
                case "MONTHLY" -> 1;
                case "QUARTERLY" -> 3;
                case "ANNUAL" -> 12;
                default -> 1;
            };

            sub.setEndDate(Instant.now().plus(months * 30L, ChronoUnit.DAYS));
            repository.save(sub);
            log.info("Auto-renewed subscription {} for user {}, plan={} until {}",
                    sub.getId(), sub.getUserId(), sub.getPlanType(), sub.getEndDate());
        }

        List<LoyaltySubscription> expired = repository
                .findByStatusAndEndDateBefore("ACTIVE", Instant.now());
        for (LoyaltySubscription sub : expired) {
            sub.setStatus("EXPIRED");
            repository.save(sub);
            log.info("Subscription {} for user {} expired", sub.getId(), sub.getUserId());
        }
    }
}
