package com.fooddelivery.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceUpdateRequest {
    private Boolean orderUpdates;
    private Boolean promotions;
    private Boolean deliveryUpdates;
}
