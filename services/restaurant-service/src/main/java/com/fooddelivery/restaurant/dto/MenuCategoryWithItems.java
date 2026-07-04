package com.fooddelivery.restaurant.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class MenuCategoryWithItems {
    UUID id;
    String name;
    int displayOrder;
    List<MenuItemResponse> items;
}
