package com.fooddelivery.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restaurant_index")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantIndex {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "cuisine_type", nullable = false)
    private String cuisineType;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(length = 10, nullable = false)
    private String pincode;

    @Column(columnDefinition = "GEOGRAPHY(Point,4326)")
    private String location;

    private Double rating;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
