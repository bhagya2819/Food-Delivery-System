package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.restaurant.dto.MenuCategoryRequest;
import com.fooddelivery.restaurant.dto.MenuCategoryResponse;
import com.fooddelivery.restaurant.dto.MenuItemRequest;
import com.fooddelivery.restaurant.dto.MenuItemResponse;
import com.fooddelivery.restaurant.dto.RestaurantMenuResponse;
import com.fooddelivery.restaurant.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<RestaurantMenuResponse> getMenu(@PathVariable UUID restaurantId) {
        return ApiResponse.ok(menuService.getMenu(restaurantId));
    }

    @PostMapping("/categories")
    public ApiResponse<MenuCategoryResponse> addCategory(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok("Category added", menuService.addCategory(restaurantId, request));
    }

    @PostMapping("/items")
    public ApiResponse<MenuItemResponse> addItem(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok("Item added", menuService.addItem(restaurantId, request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> updateItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok("Item updated", menuService.updateItem(restaurantId, itemId, request));
    }

    @PatchMapping("/items/{itemId}/availability")
    public ApiResponse<MenuItemResponse> toggleAvailability(
            @PathVariable UUID restaurantId,
            @PathVariable UUID itemId) {
        return ApiResponse.ok(menuService.toggleAvailability(itemId));
    }
}
