package com.fooddelivery.payment.repository;

import com.fooddelivery.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
