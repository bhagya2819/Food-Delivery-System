package com.fooddelivery.user.repository;

import com.fooddelivery.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserAddress> findByUserIdAndIsDefaultTrue(UUID userId);
}
