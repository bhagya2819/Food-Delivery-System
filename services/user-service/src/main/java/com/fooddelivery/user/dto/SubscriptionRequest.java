package com.fooddelivery.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    @NotBlank(message = "Plan type is required")
    private String planType;

    @Builder.Default
    private Boolean autoRenew = true;
}
