package com.fooddelivery.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "food.delivery.exchange";

    public void publishPaymentCompleted(Payment payment) {
        publish("payment.completed", Map.of(
                "paymentId", payment.getId().toString(),
                "orderId", payment.getOrderId().toString(),
                "amount", payment.getAmount().toString(),
                "gatewayPaymentId", payment.getGatewayPaymentId()
        ));
    }

    public void publishPaymentFailed(Payment payment) {
        publish("payment.failed", Map.of(
                "orderId", payment.getOrderId().toString()
        ));
    }

    public void publishPaymentRefunded(Payment payment, java.math.BigDecimal refundAmount) {
        publish("payment.refunded", Map.of(
                "orderId", payment.getOrderId().toString(),
                "paymentId", payment.getId().toString(),
                "refundAmount", refundAmount.toString()
        ));
    }

    private void publish(String eventType, Object data) {
        Map<String, Object> event = Map.of("type", eventType, "data", data);
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.event", event);
        log.info("Published payment event: {}", eventType);
    }
}
