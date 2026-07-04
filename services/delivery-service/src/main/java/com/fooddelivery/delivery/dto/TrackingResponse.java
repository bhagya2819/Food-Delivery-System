package com.fooddelivery.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {

    private Double currentLatitude;
    private Double currentLongitude;
    private long estimatedArrivalMinutes;
    private String partnerName;
}
