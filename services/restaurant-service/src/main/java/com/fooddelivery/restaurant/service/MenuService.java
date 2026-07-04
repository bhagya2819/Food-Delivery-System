package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.restaurant.dto.MenuCategoryRequest;
import com.fooddelivery.restaurant.dto.MenuCategoryResponse;
import com.fooddelivery.restaurant.dto.MenuCategoryWithItems;
import com.fooddelivery.restaurant.dto.MenuItemRequest;
import com.fooddelivery.restaurant.dto.MenuItemResponse;
import com.fooddelivery.restaurant.dto.RestaurantMenuResponse;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    public RestaurantMenuResponse getMenu(UUID restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant", restaurantId));

        List<MenuCategory> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);

        List<MenuCategoryWithItems> catWithItems = categories.stream()
                .map(cat -> MenuCategoryWithItems.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .displayOrder(cat.getDisplayOrder())
                        .items(menuItemRepository.findByCategoryIdOrderByNameAsc(cat.getId()).stream()
                                .map(this::toItemResponse)
                                .toList())
                        .build())
                .toList();

        return RestaurantMenuResponse.builder()
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .categories(catWithItems)
                .build();
    }

    @Transactional
    public MenuCategoryResponse addCategory(UUID restaurantId, MenuCategoryRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant", restaurantId));

        MenuCategory category = MenuCategory.builder()
                .restaurant(restaurant)
                .name(request.getName())
                .displayOrder(request.getDisplayOrder())
                .build();

        category = categoryRepository.save(category);

        return MenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    @Transactional
    public MenuItemResponse addItem(UUID restaurantId, MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("Restaurant", restaurantId));

        MenuCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category", request.getCategoryId()));

        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .vegetarian(request.isVegetarian())
                .dietaryTags(request.getDietaryTags())
                .available(true)
                .build();

        item = menuItemRepository.save(item);

        return toItemResponse(item);
    }

    @Transactional
    public MenuItemResponse updateItem(UUID restaurantId, UUID itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Menu Item", itemId));

        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl());
        item.setVegetarian(request.isVegetarian());
        if (request.getDietaryTags() != null) item.setDietaryTags(request.getDietaryTags());

        return toItemResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse toggleAvailability(UUID itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Menu Item", itemId));
        item.setAvailable(!item.isAvailable());
        return toItemResponse(menuItemRepository.save(item));
    }

    private MenuItemResponse toItemResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategory().getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .vegetarian(item.isVegetarian())
                .available(item.isAvailable())
                .dietaryTags(item.getDietaryTags())
                .build();
    }
}
