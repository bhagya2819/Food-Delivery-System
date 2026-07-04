package com.fooddelivery.search.repository;

import com.fooddelivery.search.entity.MenuItemIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MenuItemIndexRepository extends JpaRepository<MenuItemIndex, UUID> {

    List<MenuItemIndex> findByRestaurantIdAndAvailableTrue(UUID restaurantId);

    @Query(value = """
        SELECT * FROM menu_item_index
        WHERE restaurant_id = :restaurantId
        AND is_available = true
        AND to_tsvector('english', name) @@ plainto_tsquery('english', :query)
        """,
        nativeQuery = true)
    List<MenuItemIndex> searchByRestaurantAndName(@Param("restaurantId") UUID restaurantId,
                                                   @Param("query") String query);

    @Query(value = """
        SELECT * FROM menu_item_index
        WHERE restaurant_id = :restaurantId
        AND is_available = true
        AND is_vegetarian = :vegetarian
        AND (:query IS NULL OR to_tsvector('english', name) @@ plainto_tsquery('english', :query))
        """,
        nativeQuery = true)
    List<MenuItemIndex> searchByRestaurant(@Param("restaurantId") UUID restaurantId,
                                            @Param("query") String query,
                                            @Param("vegetarian") boolean vegetarian);
}
