package com.fooddelivery.order.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.order.dto.OrderRequest;
import com.fooddelivery.order.dto.OrderResponse;
import com.fooddelivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private static final UUID CURRENT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.ok("Order placed", orderService.placeOrder(CURRENT_USER, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.getOrder(id));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageResult = orderService.getUserOrders(CURRENT_USER, page, size);
        return ApiResponse.ok(pageResult.getContent());
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ApiResponse.ok("Order cancelled", orderService.cancelOrder(id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<String> getStatus(@PathVariable UUID id) {
        return ApiResponse.ok(orderService.getStatus(id));
    }
}
