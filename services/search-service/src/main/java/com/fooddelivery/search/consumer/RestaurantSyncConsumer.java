package com.fooddelivery.search.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.search.entity.MenuItemIndex;
import com.fooddelivery.search.entity.RestaurantIndex;
import com.fooddelivery.search.repository.MenuItemIndexRepository;
import com.fooddelivery.search.repository.RestaurantIndexRepository;
import com.fooddelivery.search.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantSyncConsumer {

    private final RestaurantIndexRepository restaurantRepo;
    private final MenuItemIndexRepository menuItemRepo;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.RESTAURANT_SEARCH_QUEUE)
    public void handleRestaurantEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String type = event.get("type").asText();
            JsonNode data = event.get("data");

            switch (type) {
                case "restaurant.created", "restaurant.updated" -> upsertRestaurant(data);
                case "restaurant.deleted" -> deleteRestaurant(data.get("id").asText());
                case "menu.item.created" -> upsertMenuItem(data);
                case "menu.item.updated" -> upsertMenuItem(data);
                case "menu.item.deleted" -> deleteMenuItem(data.get("id").asText());
                default -> log.debug("Unknown event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process restaurant sync event: {}", e.getMessage());
        }
    }

    private void upsertRestaurant(JsonNode data) {
        UUID id = UUID.fromString(data.get("id").asText());
        RestaurantIndex ri = restaurantRepo.findById(id).orElse(new RestaurantIndex());
        ri.setId(id);
        ri.setName(data.get("name").asText());
        ri.setCuisineType(data.get("cuisineType").asText());
        ri.setCity(data.get("city").asText());
        ri.setState(data.get("state").asText());
        ri.setPincode(data.get("pincode").asText());
        if (data.has("description") && !data.get("description").isNull())
            ri.setDescription(data.get("description").asText());
        if (data.has("rating")) ri.setRating(data.get("rating").asDouble());
        ri.setActive(data.has("active") ? data.get("active").asBoolean() : true);

        if (data.has("latitude") && data.has("longitude") && !data.get("latitude").isNull()) {
            double lat = data.get("latitude").asDouble();
            double lng = data.get("longitude").asDouble();
            ri.setLocation(String.format("POINT(%f %f)", lng, lat));
        }
        ri.setUpdatedAt(Instant.now());
        if (ri.getCreatedAt() == null) ri.setCreatedAt(Instant.now());

        restaurantRepo.save(ri);
        log.info("Synced restaurant: {}", ri.getId());
    }

    private void deleteRestaurant(String id) {
        restaurantRepo.deleteById(UUID.fromString(id));
        log.info("Deleted restaurant from search index: {}", id);
    }

    private void upsertMenuItem(JsonNode data) {
        UUID id = UUID.fromString(data.get("id").asText());
        MenuItemIndex item = menuItemRepo.findById(id).orElse(new MenuItemIndex());
        item.setId(id);
        item.setRestaurantId(UUID.fromString(data.get("restaurantId").asText()));
        item.setName(data.get("name").asText());
        item.setPrice(new BigDecimal(data.get("price").asText()));
        if (data.has("description") && !data.get("description").isNull())
            item.setDescription(data.get("description").asText());
        item.setVegetarian(data.has("vegetarian") && data.get("vegetarian").asBoolean());
        item.setAvailable(data.has("available") && data.get("available").asBoolean());
        if (data.has("dietaryTags") && !data.get("dietaryTags").isNull())
            item.setDietaryTags(data.get("dietaryTags").asText());
        if (item.getCreatedAt() == null) item.setCreatedAt(Instant.now());

        menuItemRepo.save(item);
        log.info("Synced menu item: {}", item.getId());
    }

    private void deleteMenuItem(String id) {
        menuItemRepo.deleteById(UUID.fromString(id));
        log.info("Deleted menu item from search index: {}", id);
    }
}
