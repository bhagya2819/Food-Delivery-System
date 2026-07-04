package com.fooddelivery.cart.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.cart.config.RestaurantFeignClient;
import com.fooddelivery.cart.dto.CartItemRequest;
import com.fooddelivery.cart.dto.CartItemResponse;
import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.dto.CheckoutResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestaurantFeignClient restaurantClient;
    private final ObjectMapper objectMapper;

    public CartResponse getCart(UUID userId) {
        Map<String, Object> cartData = loadCart(userId);
        if (cartData == null) {
            return emptyCart(userId);
        }
        return buildCartResponse(userId, cartData);
    }

    public CartResponse addItem(UUID userId, CartItemRequest request) {
        Map<String, Object> cart = loadCart(userId);

        if (cart != null && cart.containsKey("restaurantId")) {
            UUID existingRestaurantId = toUUID(cart.get("restaurantId"));
            if (!existingRestaurantId.equals(request.getRestaurantId())) {
                clearCart(userId);
                cart = null;
                log.info("Switching restaurant, cleared previous cart for user {}", userId);
            }
        }

        List<Map<String, Object>> items;
        if (cart == null) {
            cart = new HashMap<>();
            cart.put("userId", userId.toString());
            cart.put("restaurantId", request.getRestaurantId().toString());
            items = new ArrayList<>();
        } else {
            items = toItemList(cart);
        }

        var existing = items.stream()
                .filter(i -> toUUID(i.get("itemId")).equals(request.getItemId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().put("quantity", (int) existing.get().get("quantity") + request.getQuantity());
        } else {
            Map<String, Object> newItem = new HashMap<>();
            newItem.put("itemId", request.getItemId().toString());
            newItem.put("itemName", request.getItemName());
            newItem.put("unitPrice", request.getUnitPrice().doubleValue());
            newItem.put("quantity", request.getQuantity());
            items.add(newItem);
        }

        cart.put("items", items);
        saveCart(userId, cart);
        return buildCartResponse(userId, cart);
    }

    public CartResponse updateQuantity(UUID userId, UUID itemId, int quantity) {
        Map<String, Object> cart = loadCart(userId);
        if (cart == null) return emptyCart(userId);

        List<Map<String, Object>> items = toItemList(cart);
        items.stream()
                .filter(i -> toUUID(i.get("itemId")).equals(itemId))
                .findFirst()
                .ifPresentOrElse(i -> i.put("quantity", quantity),
                        () -> { throw new RuntimeException("Item not in cart"); });

        cart.put("items", items);
        saveCart(userId, cart);
        return buildCartResponse(userId, cart);
    }

    public CartResponse removeItem(UUID userId, UUID itemId) {
        Map<String, Object> cart = loadCart(userId);
        if (cart == null) return emptyCart(userId);

        List<Map<String, Object>> items = toItemList(cart);
        items.removeIf(i -> toUUID(i.get("itemId")).equals(itemId));
        cart.put("items", items);

        if (items.isEmpty()) {
            deleteCart(userId);
            return emptyCart(userId);
        }

        saveCart(userId, cart);
        return buildCartResponse(userId, cart);
    }

    public void clearCart(UUID userId) {
        deleteCart(userId);
    }

    public CartResponse applyCoupon(UUID userId, String couponCode) {
        Map<String, Object> cart = loadCart(userId);
        if (cart == null) return emptyCart(userId);

        cart.put("couponCode", couponCode);
        saveCart(userId, cart);
        return buildCartResponse(userId, cart);
    }

    public CartResponse removeCoupon(UUID userId) {
        Map<String, Object> cart = loadCart(userId);
        if (cart == null) return emptyCart(userId);
        cart.remove("couponCode");
        saveCart(userId, cart);
        return buildCartResponse(userId, cart);
    }

    public CheckoutResponse checkout(UUID userId) {
        Map<String, Object> cart = loadCart(userId);
        if (cart == null || toItemList(cart).isEmpty()) {
            return CheckoutResponse.builder().canPlaceOrder(false).message("Cart is empty").build();
        }

        CartResponse cartResponse = buildCartResponse(userId, cart);
        BigDecimal tax = cartResponse.getSubtotal().multiply(new BigDecimal("0.05"));
        BigDecimal finalTotal = cartResponse.getTotal().add(tax);

        return CheckoutResponse.builder()
                .cart(cartResponse)
                .taxAmount(tax)
                .finalTotal(finalTotal)
                .canPlaceOrder(true)
                .message("Ready to place order")
                .build();
    }

    private Map<String, Object> loadCart(UUID userId) {
        Object data = redisTemplate.opsForValue().get(CART_PREFIX + userId);
        if (data == null) return null;
        return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
    }

    private void saveCart(UUID userId, Map<String, Object> cart) {
        redisTemplate.opsForValue().set(CART_PREFIX + userId, cart, CART_TTL);
    }

    private void deleteCart(UUID userId) {
        redisTemplate.delete(CART_PREFIX + userId);
    }

    private CartResponse emptyCart(UUID userId) {
        return CartResponse.builder().userId(userId).items(List.of())
                .subtotal(BigDecimal.ZERO).deliveryFee(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO).total(BigDecimal.ZERO).build();
    }

    private CartResponse buildCartResponse(UUID userId, Map<String, Object> cart) {
        UUID restaurantId = toUUID(cart.get("restaurantId"));
        List<CartItemResponse> items = toItemResponseList(cart);

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = calculateDelivery(subtotal);
        BigDecimal discount = calculateDiscount((String) cart.get("couponCode"), subtotal);
        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        return CartResponse.builder()
                .userId(userId).restaurantId(restaurantId)
                .restaurantName(getRestaurantName(restaurantId))
                .items(items).subtotal(subtotal)
                .deliveryFee(deliveryFee).discount(discount).total(total)
                .couponCode((String) cart.get("couponCode"))
                .build();
    }

    private List<CartItemResponse> toItemResponseList(Map<String, Object> cart) {
        List<Map<String, Object>> raw = toItemList(cart);
        return raw.stream().map(i -> {
            BigDecimal price = toBigDecimal(i.get("unitPrice"));
            int qty = (int) i.get("quantity");
            return CartItemResponse.builder()
                    .itemId(toUUID(i.get("itemId"))).itemName((String) i.get("itemName"))
                    .quantity(qty).unitPrice(price).totalPrice(price.multiply(BigDecimal.valueOf(qty)))
                    .build();
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toItemList(Map<String, Object> cart) {
        var items = cart.get("items");
        if (items instanceof List) return (List<Map<String, Object>>) items;
        return new ArrayList<>();
    }

    private UUID toUUID(Object val) { return val != null ? UUID.fromString(val.toString()) : null; }

    private BigDecimal toBigDecimal(Object val) {
        return val != null ? new BigDecimal(val.toString()) : BigDecimal.ZERO;
    }

    private BigDecimal calculateDelivery(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("200")) >= 0 ? BigDecimal.ZERO : new BigDecimal("30");
    }

    private BigDecimal calculateDiscount(String couponCode, BigDecimal subtotal) {
        if (couponCode == null) return BigDecimal.ZERO;
        return switch (couponCode.toUpperCase()) {
            case "WELCOME50" -> subtotal.multiply(new BigDecimal("0.50")).min(new BigDecimal("100"));
            case "FLAT20" -> new BigDecimal("20");
            default -> BigDecimal.ZERO;
        };
    }

    private String getRestaurantName(UUID restaurantId) {
        try {
            return restaurantClient.getRestaurant(restaurantId).getName();
        } catch (Exception e) {
            return "Unknown Restaurant";
        }
    }
}
