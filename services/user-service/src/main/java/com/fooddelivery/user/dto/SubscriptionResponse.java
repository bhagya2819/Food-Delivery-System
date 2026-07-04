package com.fooddelivery.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private UUID id;
    private UUID userId;
    private String planType;
    private String status;
    private Instant startDate;
    private Instant endDate;
    private Boolean autoRenew;
    private Instant createdAt;
}
