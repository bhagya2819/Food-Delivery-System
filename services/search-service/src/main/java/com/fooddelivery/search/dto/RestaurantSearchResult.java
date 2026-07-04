package com.fooddelivery.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSearchResult {
    private UUID id;
    private String name;
    private String cuisineType;
    private String city;
    private String state;
    private Double rating;
    private Double distanceKm;
}
