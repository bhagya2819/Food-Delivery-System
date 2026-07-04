package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, UUID> {

    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
