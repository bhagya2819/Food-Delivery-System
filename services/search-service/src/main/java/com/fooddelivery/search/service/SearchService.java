package com.fooddelivery.search.service;

import com.fooddelivery.search.dto.MenuSearchResult;
import com.fooddelivery.search.dto.RestaurantSearchResult;
import com.fooddelivery.search.entity.RestaurantIndex;
import com.fooddelivery.search.repository.MenuItemIndexRepository;
import com.fooddelivery.search.repository.RestaurantIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final RestaurantIndexRepository restaurantRepo;
    private final MenuItemIndexRepository menuItemRepo;

    public List<RestaurantSearchResult> searchRestaurants(String query, String cuisine, String city,
                                                           Double lat, Double lng, Double radiusKm) {
        List<RestaurantIndex> results;

        if (lat != null && lng != null && radiusKm != null) {
            List<Object[]> rows = restaurantRepo.findNearby(lat, lng, radiusKm * 1000.0, cuisine);
            results = rows.stream().map(row -> {
                RestaurantIndex ri = (RestaurantIndex) row[0];
                ri.setRating(ri.getRating());
                return ri;
            }).toList();
            return results.stream()
                    .map(r -> RestaurantSearchResult.builder()
                            .id(r.getId()).name(r.getName()).cuisineType(r.getCuisineType())
                            .city(r.getCity()).state(r.getState()).rating(r.getRating()).build())
                    .toList();
        }

        if (query != null && !query.isBlank()) {
            results = restaurantRepo.searchByName(query);
        } else if (cuisine != null && !cuisine.isBlank()) {
            results = restaurantRepo.findByCuisineTypeIgnoreCase(cuisine);
        } else if (city != null && !city.isBlank()) {
            results = restaurantRepo.findByCityIgnoreCase(city);
        } else {
            results = restaurantRepo.findAll(PageRequest.of(0, 50)).getContent();
        }

        return results.stream()
                .map(r -> RestaurantSearchResult.builder()
                        .id(r.getId()).name(r.getName()).cuisineType(r.getCuisineType())
                        .city(r.getCity()).state(r.getState()).rating(r.getRating()).build())
                .toList();
    }

    public List<MenuSearchResult> searchMenu(UUID restaurantId, String query, Boolean vegetarian) {
        var items = menuItemRepo.searchByRestaurant(restaurantId, query,
                vegetarian != null ? vegetarian : false);
        return items.stream()
                .map(i -> MenuSearchResult.builder()
                        .id(i.getId()).restaurantId(i.getRestaurantId()).name(i.getName())
                        .description(i.getDescription()).price(i.getPrice())
                        .vegetarian(i.isVegetarian()).available(i.isAvailable()).build())
                .toList();
    }
}
