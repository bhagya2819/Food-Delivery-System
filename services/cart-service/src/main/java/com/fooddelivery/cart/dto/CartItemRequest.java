package com.fooddelivery.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    @NotNull(message = "Restaurant ID is required")
    private UUID restaurantId;

    private String itemName;
    private BigDecimal unitPrice;

    @Positive(message = "Quantity must be positive")
    @Builder.Default
    private int quantity = 1;
}
