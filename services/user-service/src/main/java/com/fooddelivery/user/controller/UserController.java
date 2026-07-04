package com.fooddelivery.user.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.user.dto.SubscriptionRequest;
import com.fooddelivery.user.dto.SubscriptionResponse;
import com.fooddelivery.user.dto.UserAddressRequest;
import com.fooddelivery.user.dto.UserAddressResponse;
import com.fooddelivery.user.dto.UserResponse;
import com.fooddelivery.user.service.SubscriptionService;
import com.fooddelivery.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID id, @RequestBody UserResponse request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressResponse>> getAddresses() {
        return ApiResponse.ok(userService.getAddresses(getCurrentUserId()));
    }

    @PostMapping("/addresses")
    public ApiResponse<UserAddressResponse> addAddress(@Valid @RequestBody UserAddressRequest request) {
        return ApiResponse.ok(userService.addAddress(getCurrentUserId(), request));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(@PathVariable UUID addressId) {
        userService.deleteAddress(getCurrentUserId(), addressId);
        return ApiResponse.ok("Address deleted", null);
    }

    @PostMapping("/subscriptions")
    public ApiResponse<SubscriptionResponse> subscribe(@Valid @RequestBody SubscriptionRequest request) {
        return ApiResponse.ok("Subscription created", subscriptionService.subscribe(getCurrentUserId(), request));
    }

    @GetMapping("/{id}/subscription")
    public ApiResponse<SubscriptionResponse> getSubscription(@PathVariable UUID id) {
        return ApiResponse.ok(subscriptionService.getSubscription(id));
    }

    @PatchMapping("/subscriptions/{subscriptionId}/cancel")
    public ApiResponse<SubscriptionResponse> cancelSubscription(@PathVariable UUID subscriptionId) {
        return ApiResponse.ok("Subscription cancelled", subscriptionService.cancel(subscriptionId));
    }

    private UUID getCurrentUserId() {
        return UUID.randomUUID();
    }
}
