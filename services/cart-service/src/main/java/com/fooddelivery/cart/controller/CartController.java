package com.fooddelivery.cart.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.cart.dto.CartItemRequest;
import com.fooddelivery.cart.dto.CartResponse;
import com.fooddelivery.cart.dto.CheckoutResponse;
import com.fooddelivery.cart.dto.CouponRequest;
import com.fooddelivery.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private static final UUID CURRENT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.ok(cartService.getCart(CURRENT_USER));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ApiResponse.ok("Item added", cartService.addItem(CURRENT_USER, request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateQuantity(@PathVariable UUID itemId, @RequestBody CartItemRequest request) {
        return ApiResponse.ok(cartService.updateQuantity(CURRENT_USER, itemId, request.getQuantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable UUID itemId) {
        return ApiResponse.ok(cartService.removeItem(CURRENT_USER, itemId));
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(CURRENT_USER);
        return ApiResponse.ok("Cart cleared", null);
    }

    @PostMapping("/coupon")
    public ApiResponse<CartResponse> applyCoupon(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok("Coupon applied", cartService.applyCoupon(CURRENT_USER, request.getCode()));
    }

    @DeleteMapping("/coupon")
    public ApiResponse<CartResponse> removeCoupon() {
        return ApiResponse.ok("Coupon removed", cartService.removeCoupon(CURRENT_USER));
    }

    @GetMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout() {
        return ApiResponse.ok(cartService.checkout(CURRENT_USER));
    }
}
