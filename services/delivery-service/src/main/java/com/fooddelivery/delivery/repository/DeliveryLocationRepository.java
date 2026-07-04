package com.fooddelivery.delivery.repository;

import com.fooddelivery.delivery.entity.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, UUID> {

    List<DeliveryLocation> findByAssignmentIdOrderByTimestampAsc(UUID assignmentId);

    List<DeliveryLocation> findByAssignmentIdOrderByTimestampDesc(UUID assignmentId);
}
