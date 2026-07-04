package com.fooddelivery.delivery.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.delivery.dto.AssignmentResponse;
import com.fooddelivery.delivery.dto.StatusUpdateRequest;
import com.fooddelivery.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/delivery/assignments")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PatchMapping("/{id}/status")
    public ApiResponse<AssignmentResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        AssignmentResponse assignment = deliveryService.updateStatus(id, request);
        return ApiResponse.ok("Status updated to " + request.getStatus(), assignment);
    }
}
