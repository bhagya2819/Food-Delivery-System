package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.restaurant.dto.RestaurantResponse;
import com.fooddelivery.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantSearchController {

    private final RestaurantService restaurantService;

    @GetMapping("/search")
    public ApiResponse<List<RestaurantResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String city) {
        return ApiResponse.ok(restaurantService.search(name, cuisine, city));
    }
}
