package com.fooddelivery.payment.repository;

import com.fooddelivery.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
}
