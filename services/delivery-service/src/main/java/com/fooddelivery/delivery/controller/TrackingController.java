package com.fooddelivery.delivery.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.delivery.dto.LocationUpdate;
import com.fooddelivery.delivery.dto.TrackingResponse;
import com.fooddelivery.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class TrackingController {

    private final DeliveryService deliveryService;

    @PostMapping("/partners/{id}/location")
    public ApiResponse<Void> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody LocationUpdate request) {
        deliveryService.updatePartnerLocation(id, request.getLatitude(), request.getLongitude());
        return ApiResponse.ok("Location updated", null);
    }

    @GetMapping("/orders/{orderId}/tracking")
    public ApiResponse<TrackingResponse> getTracking(@PathVariable UUID orderId) {
        return ApiResponse.ok(deliveryService.getTracking(orderId));
    }
}
