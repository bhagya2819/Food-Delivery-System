package com.fooddelivery.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.lib.exception.BadRequestException;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.order.config.CartFeignClient;
import com.fooddelivery.order.config.OrderEventPublisher;
import com.fooddelivery.order.dto.CheckoutDto;
import com.fooddelivery.order.dto.OrderItemResponse;
import com.fooddelivery.order.dto.OrderRequest;
import com.fooddelivery.order.dto.OrderResponse;
import com.fooddelivery.order.dto.OrderStatusLogResponse;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.OrderStatusLog;
import com.fooddelivery.order.entity.PaymentStatus;
import com.fooddelivery.order.repository.OrderItemRepository;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.OrderStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusLogRepository logRepository;
    private final OrderEventPublisher eventPublisher;
    private final CartFeignClient cartClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse placeOrder(UUID userId, OrderRequest request) {
        CheckoutDto checkout = cartClient.getCheckout();
        if (!checkout.isCanPlaceOrder()) {
            throw new BadRequestException(checkout.getMessage());
        }

        var cart = checkout.getCart();

        Order order = Order.builder()
                .userId(userId)
                .restaurantId(cart.getRestaurantId())
                .deliveryAddressId(request.getDeliveryAddressId())
                .status(OrderStatus.PLACED)
                .subtotal(cart.getSubtotal())
                .deliveryFee(cart.getDeliveryFee())
                .tax(checkout.getTaxAmount())
                .discount(cart.getDiscount())
                .totalAmount(checkout.getFinalTotal())
                .paymentStatus(PaymentStatus.PENDING)
                .couponCode(cart.getCouponCode())
                .notes(request.getNotes())
                .build();

        order = orderRepository.save(order);

        for (var cartItem : cart.getItems()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .menuItemId(cartItem.getItemId())
                    .itemName(cartItem.getItemName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .build();
            itemRepository.save(item);
        }

        logStatus(order, "PLACED", "Order placed by customer");
        eventPublisher.publishOrderPlaced(order);
        return toResponse(order);
    }

    public OrderResponse getOrder(UUID orderId) {
        return toResponse(findOrder(orderId));
    }

    public Page<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.PICKED_UP || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel order in " + order.getStatus() + " status");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        logStatus(order, "CANCELLED", "Cancelled by customer");
        eventPublisher.publishOrderCancelled(order);
        return toResponse(order);
    }

    public String getStatus(UUID orderId) {
        return findOrder(orderId).getStatus().name();
    }

    @RabbitListener(queues = "order.status.updates")
    public void handlePaymentEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String type = event.get("type").asText();
            JsonNode data = event.get("data");

            if ("payment.completed".equals(type)) {
                UUID orderId = UUID.fromString(data.get("orderId").asText());
                Order order = findOrder(orderId);
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setPaymentId(UUID.fromString(data.get("paymentId").asText()));
                orderRepository.save(order);
                logStatus(order, "CONFIRMED", "Payment confirmed");
                log.info("Order {} payment completed", orderId);
            } else if ("payment.failed".equals(type)) {
                UUID orderId = UUID.fromString(data.get("orderId").asText());
                Order order = findOrder(orderId);
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
                logStatus(order, "PAYMENT_FAILED", "Payment failed");
            } else if ("payment.refunded".equals(type)) {
                UUID orderId = UUID.fromString(data.get("orderId").asText());
                Order order = findOrder(orderId);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                logStatus(order, "REFUNDED", "Payment refunded");
            }
        } catch (Exception e) {
            log.error("Failed to process order status event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "order.delivery.updates")
    public void handleDeliveryEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String type = event.get("type").asText();
            JsonNode data = event.get("data");
            UUID orderId = UUID.fromString(data.get("orderId").asText());
            Order order = findOrder(orderId);

            switch (type) {
                case "delivery.assigned" -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    logStatus(order, "CONFIRMED", "Delivery partner assigned");
                }
                case "delivery.picked_up" -> {
                    order.setStatus(OrderStatus.PICKED_UP);
                    logStatus(order, "PICKED_UP", "Order picked up");
                }
                case "delivery.completed" -> {
                    order.setStatus(OrderStatus.DELIVERED);
                    logStatus(order, "DELIVERED", "Order delivered");
                }
            }
            orderRepository.save(order);
            log.info("Order {} status updated from delivery event: {}", orderId, type);
        } catch (Exception e) {
            log.error("Failed to process delivery event: {}", e.getMessage());
        }
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));
    }

    private void logStatus(Order order, String status, String note) {
        logRepository.save(OrderStatusLog.builder()
                .order(order).status(status).note(note).build());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItem> items = itemRepository.findAll()
                .stream().filter(i -> i.getOrder().getId().equals(order.getId())).toList();

        return OrderResponse.builder()
                .id(order.getId()).userId(order.getUserId()).restaurantId(order.getRestaurantId())
                .deliveryAddressId(order.getDeliveryAddressId()).status(order.getStatus())
                .subtotal(order.getSubtotal()).deliveryFee(order.getDeliveryFee())
                .tax(order.getTax()).discount(order.getDiscount()).totalAmount(order.getTotalAmount())
                .paymentStatus(order.getPaymentStatus()).paymentId(order.getPaymentId())
                .couponCode(order.getCouponCode()).notes(order.getNotes())
                .items(items.stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId()).menuItemId(item.getMenuItemId()).itemName(item.getItemName())
                        .quantity(item.getQuantity()).unitPrice(item.getUnitPrice()).totalPrice(item.getTotalPrice())
                        .build()).toList())
                .statusHistory(logRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                        .map(log -> OrderStatusLogResponse.builder()
                                .id(log.getId()).status(log.getStatus()).note(log.getNote()).createdAt(log.getCreatedAt())
                                .build()).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
