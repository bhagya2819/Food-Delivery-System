package com.fooddelivery.order.config;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.order.dto.SubscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/users/{id}/subscription")
    ApiResponse<SubscriptionDto> getSubscription(@PathVariable UUID id);
}
