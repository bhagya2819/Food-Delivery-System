package com.fooddelivery.search.repository;

import com.fooddelivery.search.entity.RestaurantIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RestaurantIndexRepository extends JpaRepository<RestaurantIndex, UUID> {

    List<RestaurantIndex> findByCuisineTypeIgnoreCase(String cuisineType);

    List<RestaurantIndex> findByCityIgnoreCase(String city);

    @Query(value = """
        SELECT * FROM restaurant_index
        WHERE to_tsvector('english', name) @@ plainto_tsquery('english', :query)
        AND is_active = true
        """,
        nativeQuery = true)
    List<RestaurantIndex> searchByName(@Param("query") String query);

    @Query(value = """
        SELECT r.*,
               ST_Distance(r.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) / 1000.0 AS distance_km
        FROM restaurant_index r
        WHERE ST_DWithin(r.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :radiusMeters)
        AND r.is_active = true
        AND (:cuisine IS NULL OR LOWER(r.cuisine_type) = LOWER(:cuisine))
        ORDER BY distance_km ASC, r.rating DESC
        """,
        nativeQuery = true)
    List<Object[]> findNearby(@Param("lat") Double lat,
                              @Param("lng") Double lng,
                              @Param("radiusMeters") Double radiusMeters,
                              @Param("cuisine") String cuisine);
}
