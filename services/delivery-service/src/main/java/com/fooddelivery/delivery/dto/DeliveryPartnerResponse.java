package com.fooddelivery.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerResponse {

    private UUID id;
    private UUID userId;
    private String vehicleType;
    private String licenseNumber;
    private Boolean isVerified;
    private Boolean isOnline;
    private Double currentLatitude;
    private Double currentLongitude;
    private BigDecimal averageRating;
    private Instant createdAt;
}
