package com.fooddelivery.payment.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.payment.dto.PaymentRequest;
import com.fooddelivery.payment.dto.PaymentResponse;
import com.fooddelivery.payment.dto.RefundResponse;
import com.fooddelivery.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ApiResponse<PaymentResponse> initiate(@RequestBody PaymentRequest request) {
        return ApiResponse.ok("Payment initiated", paymentService.initiate(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ApiResponse.ok(paymentService.getPayment(id));
    }

    @PostMapping("/callback/razorpay")
    public ApiResponse<String> razorpayCallback(@RequestBody Map<String, String> payload) {
        paymentService.handleRazorpayCallback(
                payload.get("paymentId"),
                payload.get("razorpayPaymentId"),
                payload.get("razorpayOrderId"),
                payload.get("status"),
                payload.get("signature")
        );
        return ApiResponse.ok("Callback processed");
    }

    @PostMapping("/callback/stripe")
    public ApiResponse<String> stripeCallback(@RequestBody Map<String, String> payload) {
        paymentService.handleStripeCallback(
                payload.get("paymentIntentId"),
                payload.get("status")
        );
        return ApiResponse.ok("Callback processed");
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<RefundResponse> refund(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("Refund processed", paymentService.refund(id, body.get("reason")));
    }
}
