package com.fooddelivery.order.config;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.order.dto.CheckoutDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "cart-service")
public interface CartFeignClient {

    @GetMapping("/cart/checkout")
    ApiResponse<CheckoutDto> getCheckout();
}
