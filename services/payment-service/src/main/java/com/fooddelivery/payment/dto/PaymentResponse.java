package com.fooddelivery.payment.dto;

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
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String gateway;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String status;
    private String method;
    private String checkoutUrl;
    private Instant createdAt;
}
