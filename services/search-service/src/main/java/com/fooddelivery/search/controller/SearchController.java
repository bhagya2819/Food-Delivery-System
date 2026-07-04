package com.fooddelivery.search.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.search.dto.MenuSearchResult;
import com.fooddelivery.search.dto.RestaurantSearchResult;
import com.fooddelivery.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/restaurants")
    public ApiResponse<List<RestaurantSearchResult>> searchRestaurants(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius) {
        return ApiResponse.ok(searchService.searchRestaurants(q, cuisine, city, lat, lng, radius));
    }

    @GetMapping("/menu")
    public ApiResponse<List<MenuSearchResult>> searchMenu(
            @RequestParam UUID restaurantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean vegetarian) {
        return ApiResponse.ok(searchService.searchMenu(restaurantId, q, vegetarian));
    }
}
