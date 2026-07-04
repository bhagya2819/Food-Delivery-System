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
public class RefundResponse {

    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String gatewayRefundId;
    private Instant createdAt;
}
