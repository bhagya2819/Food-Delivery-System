package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
