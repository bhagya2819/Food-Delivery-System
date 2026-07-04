package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    List<Restaurant> findByOwnerId(UUID ownerId);

    List<Restaurant> findByCityAndActiveTrue(String city);

    List<Restaurant> findByCuisineTypeAndActiveTrue(String cuisineType);

    List<Restaurant> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}
