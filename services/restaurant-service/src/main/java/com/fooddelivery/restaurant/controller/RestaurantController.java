package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.restaurant.dto.RestaurantRequest;
import com.fooddelivery.restaurant.dto.RestaurantResponse;
import com.fooddelivery.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ApiResponse<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request) {
        return ApiResponse.ok("Restaurant created", restaurantService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(restaurantService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> update(@PathVariable UUID id, @Valid @RequestBody RestaurantRequest request) {
        return ApiResponse.ok("Restaurant updated", restaurantService.update(id, request));
    }
}
