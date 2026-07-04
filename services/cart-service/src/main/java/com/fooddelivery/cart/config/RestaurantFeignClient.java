package com.fooddelivery.cart.config;

import com.fooddelivery.cart.dto.RestaurantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "restaurant-service")
public interface RestaurantFeignClient {

    @GetMapping("/restaurants/{id}")
    RestaurantResponse getRestaurant(@PathVariable UUID id);
}
