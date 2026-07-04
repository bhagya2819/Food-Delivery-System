package com.fooddelivery.delivery.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.delivery.dto.DeliveryPartnerRequest;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/delivery/partners")
@RequiredArgsConstructor
public class DeliveryPartnerController {

    private final DeliveryPartnerService partnerService;

    @PostMapping
    public ApiResponse<DeliveryPartnerResponse> register(@Valid @RequestBody DeliveryPartnerRequest request) {
        return ApiResponse.ok("Partner registered", partnerService.register(request));
    }

    @PatchMapping("/{id}/verify")
    public ApiResponse<DeliveryPartnerResponse> verify(@PathVariable UUID id) {
        return ApiResponse.ok("Partner verified", partnerService.verify(id));
    }

    @PatchMapping("/{id}/online")
    public ApiResponse<DeliveryPartnerResponse> toggleOnline(@PathVariable UUID id) {
        DeliveryPartnerResponse partner = partnerService.toggleOnline(id);
        return ApiResponse.ok(
                partner.getIsOnline() ? "Partner is now online" : "Partner is now offline",
                partner
        );
    }
}
