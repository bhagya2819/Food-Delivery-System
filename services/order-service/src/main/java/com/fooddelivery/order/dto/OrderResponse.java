package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private UUID restaurantId;
    private UUID deliveryAddressId;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private UUID paymentId;
    private String couponCode;
    private String notes;
    private List<OrderItemResponse> items;
    private List<OrderStatusLogResponse> statusHistory;
    private Instant createdAt;
}
