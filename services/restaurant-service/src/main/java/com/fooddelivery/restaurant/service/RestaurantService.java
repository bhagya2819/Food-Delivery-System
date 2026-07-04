package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.restaurant.dto.RestaurantRequest;
import com.fooddelivery.restaurant.dto.RestaurantResponse;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantResponse create(RestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .ownerId(request.getOwnerId())
                .name(request.getName())
                .description(request.getDescription())
                .cuisineType(request.getCuisineType())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .active(true)
                .build();

        return toResponse(restaurantRepository.save(restaurant));
    }

    public RestaurantResponse getById(UUID id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant", id));
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse update(UUID id, RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant", id));

        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getCuisineType() != null) restaurant.setCuisineType(request.getCuisineType());
        if (request.getAddressLine1() != null) restaurant.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) restaurant.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) restaurant.setCity(request.getCity());
        if (request.getState() != null) restaurant.setState(request.getState());
        if (request.getPincode() != null) restaurant.setPincode(request.getPincode());
        if (request.getLatitude() != null) restaurant.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) restaurant.setLongitude(request.getLongitude());

        return toResponse(restaurantRepository.save(restaurant));
    }

    public List<RestaurantResponse> getByOwner(UUID ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .ownerId(r.getOwnerId())
                .name(r.getName())
                .description(r.getDescription())
                .cuisineType(r.getCuisineType())
                .addressLine1(r.getAddressLine1())
                .addressLine2(r.getAddressLine2())
                .city(r.getCity())
                .state(r.getState())
                .pincode(r.getPincode())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .rating(r.getRating())
                .active(r.isActive())
                .openingTime(r.getOpeningTime() != null ? r.getOpeningTime().toString() : null)
                .closingTime(r.getClosingTime() != null ? r.getClosingTime().toString() : null)
                .build();
    }
}
