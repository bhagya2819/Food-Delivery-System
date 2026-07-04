package com.fooddelivery.search.repository;

import com.fooddelivery.search.entity.RestaurantIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantIndexRepository extends JpaRepository<RestaurantIndex, UUID> {
}
