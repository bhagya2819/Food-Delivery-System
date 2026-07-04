package com.fooddelivery.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDto {
    private CartDto cart;
    private BigDecimal taxAmount;
    private BigDecimal finalTotal;
    private boolean canPlaceOrder;
    private String message;
}
