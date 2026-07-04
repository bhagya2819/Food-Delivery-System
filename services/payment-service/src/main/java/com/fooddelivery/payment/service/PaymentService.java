package com.fooddelivery.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.payment.config.PaymentEventPublisher;
import com.fooddelivery.payment.dto.PaymentRequest;
import com.fooddelivery.payment.dto.PaymentResponse;
import com.fooddelivery.payment.dto.RefundResponse;
import com.fooddelivery.payment.entity.Payment;
import com.fooddelivery.payment.entity.Refund;
import com.fooddelivery.payment.repository.PaymentRepository;
import com.fooddelivery.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final RefundRepository refundRepo;
    private final PaymentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse initiate(PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .gateway(request.getGateway())
                .status("INITIATED")
                .build();

        payment = paymentRepo.save(payment);
        log.info("Payment initiated: {} for order {}", payment.getId(), payment.getOrderId());

        return PaymentResponse.builder()
                .id(payment.getId()).orderId(payment.getOrderId())
                .userId(payment.getUserId()).amount(payment.getAmount())
                .gateway(payment.getGateway()).status(payment.getStatus())
                .checkoutUrl("https://checkout.dummy/" + payment.getId())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment", id));
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirmWebhook(String gateway, String gatewayPaymentId, String status) {
        Payment payment = paymentRepo.findByGatewayPaymentId(gatewayPaymentId)
                .orElseGet(() -> Payment.builder().gatewayPaymentId(gatewayPaymentId).build());

        payment.setGateway(gateway);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setStatus(status.toUpperCase());

        if ("COMPLETED".equalsIgnoreCase(status)) {
            payment = paymentRepo.save(payment);
            eventPublisher.publishPaymentCompleted(payment);
        } else {
            payment = paymentRepo.save(payment);
            eventPublisher.publishPaymentFailed(payment);
        }

        return toResponse(payment);
    }

    public void handleRazorpayCallback(String paymentId, String razorpayPaymentId, String razorpayOrderId, String status, String signature) {
        log.info("Razorpay callback: payment={}, razorpay={}, status={}", paymentId, razorpayPaymentId, status);
        Payment payment = paymentRepo.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new NotFoundException("Payment", UUID.fromString(paymentId)));

        payment.setGateway("RAZORPAY");
        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setGatewayOrderId(razorpayOrderId);

        if ("VERIFIED".equals(signature) && "CAPTURED".equals(status)) {
            payment.setStatus("COMPLETED");
            payment = paymentRepo.save(payment);
            eventPublisher.publishPaymentCompleted(payment);
        } else {
            payment.setStatus("FAILED");
            payment = paymentRepo.save(payment);
            eventPublisher.publishPaymentFailed(payment);
        }
    }

    public void handleStripeCallback(String paymentIntentId, String status) {
        log.info("Stripe callback: paymentIntent={}, status={}", paymentIntentId, status);
        confirmWebhook("STRIPE", paymentIntentId, "succeeded".equals(status) ? "COMPLETED" : "FAILED");
    }

    @Transactional
    public RefundResponse refund(UUID paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(payment.getAmount())
                .reason(reason)
                .status("COMPLETED")
                .gatewayRefundId("ref_dummy_" + UUID.randomUUID().toString().substring(0, 8))
                .build();

        refund = refundRepo.save(refund);
        payment.setStatus("REFUNDED");
        paymentRepo.save(payment);

        eventPublisher.publishPaymentRefunded(payment, refund.getAmount());

        return RefundResponse.builder()
                .id(refund.getId()).paymentId(payment.getId())
                .amount(refund.getAmount()).reason(refund.getReason())
                .status(refund.getStatus()).gatewayRefundId(refund.getGatewayRefundId())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    @RabbitListener(queues = "payment.order.events")
    public void handleOrderEvent(Map<String, Object> message) {
        try {
            JsonNode event = objectMapper.valueToTree(message);
            String type = event.get("type").asText();

            if ("order.placed".equals(type)) {
                JsonNode data = event.get("data");
                PaymentRequest req = PaymentRequest.builder()
                        .orderId(UUID.fromString(data.get("orderId").asText()))
                        .userId(UUID.fromString(data.get("userId").asText()))
                        .amount(new BigDecimal(data.get("totalAmount").asText()))
                        .gateway("RAZORPAY")
                        .build();
                initiate(req);
            } else if ("order.cancelled".equals(type)) {
                JsonNode data = event.get("data");
                String paymentIdStr = data.has("paymentId") && !data.get("paymentId").isNull()
                        ? data.get("paymentId").asText() : null;
                if (paymentIdStr != null) {
                    refund(UUID.fromString(paymentIdStr), "Order cancelled");
                }
            }
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage());
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).orderId(p.getOrderId()).userId(p.getUserId())
                .amount(p.getAmount()).gateway(p.getGateway())
                .gatewayPaymentId(p.getGatewayPaymentId()).gatewayOrderId(p.getGatewayOrderId())
                .status(p.getStatus()).method(p.getMethod())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
