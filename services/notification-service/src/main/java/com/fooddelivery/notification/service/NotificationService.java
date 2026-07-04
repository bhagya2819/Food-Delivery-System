package com.fooddelivery.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.lib.exception.NotFoundException;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.notification.dto.NotificationResponse;
import com.fooddelivery.notification.dto.PreferenceUpdateRequest;
import com.fooddelivery.notification.entity.Notification;
import com.fooddelivery.notification.entity.NotificationPreference;
import com.fooddelivery.notification.repository.NotificationPreferenceRepository;
import com.fooddelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    private static final UUID CURRENT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationPreference prefs = getPreferences(request.getUserId());
        if (!shouldSend(prefs, request.getType())) {
            log.info("Notification suppressed by preferences: user={}, type={}", request.getUserId(), request.getType());
            return null;
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .body(request.getBody())
                .dataJson(serializeData(request.getData()))
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}, type={}, user={}", notification.getId(), notification.getType(), notification.getUserId());
        return toResponse(notification);
    }

    @RabbitListener(queues = "notification.order.events")
    public void handleOrderEvent(Map<String, Object> message) {
        try {
            JsonNode event = objectMapper.valueToTree(message);
            String type = event.get("type").asText();
            JsonNode data = event.get("data");
            UUID userId = CURRENT_USER;

            String title;
            String body;
            switch (type) {
                case "order.placed" -> {
                    title = "Order Placed";
                    body = "Your order has been placed successfully!";
                }
                case "order.cancelled" -> {
                    title = "Order Cancelled";
                    body = "Your order has been cancelled.";
                }
                default -> { return; }
            }

            createNotification(NotificationRequest.builder()
                    .userId(userId).type(type).title(title).body(body).build());
        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "notification.delivery.events")
    public void handleDeliveryEvent(Map<String, Object> message) {
        try {
            JsonNode event = objectMapper.valueToTree(message);
            String type = event.get("type").asText();
            UUID userId = CURRENT_USER;

            String title;
            String body;
            switch (type) {
                case "delivery.assigned" -> {
                    title = "Delivery Partner Assigned";
                    body = "A delivery partner has been assigned to your order!";
                }
                case "delivery.status.updated" -> {
                    JsonNode data = event.get("data");
                    String status = data.get("status").asText();
                    title = "Delivery Update";
                    body = "Delivery status: " + status;
                }
                case "delivery.completed" -> {
                    title = "Order Delivered";
                    body = "Your order has been delivered. Enjoy!";
                }
                default -> { return; }
            }

            createNotification(NotificationRequest.builder()
                    .userId(userId).type(type).title(title).body(body).build());
        } catch (Exception e) {
            log.error("Failed to process delivery event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "notification.payment.events")
    public void handlePaymentEvent(Map<String, Object> message) {
        try {
            JsonNode event = objectMapper.valueToTree(message);
            String type = event.get("type").asText();
            UUID userId = CURRENT_USER;

            String title;
            String body;
            switch (type) {
                case "payment.completed" -> {
                    title = "Payment Successful";
                    body = "Your payment has been processed successfully!";
                }
                case "payment.failed" -> {
                    title = "Payment Failed";
                    body = "Your payment could not be processed. Please try again.";
                }
                case "payment.refunded" -> {
                    title = "Refund Processed";
                    body = "Your refund has been initiated.";
                }
                default -> { return; }
            }

            createNotification(NotificationRequest.builder()
                    .userId(userId).type(type).title(title).body(body).build());
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }

    public Page<NotificationResponse> getUserNotifications(int page, int size) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(CURRENT_USER, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public long getUnreadCount() {
        return notificationRepository.countByUserIdAndIsReadFalse(CURRENT_USER);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification", id));
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    public Map<String, Boolean> getPreferencesResponse() {
        NotificationPreference prefs = getPreferences(CURRENT_USER);
        return Map.of(
                "orderUpdates", prefs.getOrderUpdates(),
                "promotions", prefs.getPromotions(),
                "deliveryUpdates", prefs.getDeliveryUpdates()
        );
    }

    @Transactional
    public Map<String, Boolean> updatePreferences(PreferenceUpdateRequest request) {
        NotificationPreference prefs = preferenceRepository.findByUserId(CURRENT_USER)
                .orElseGet(() -> NotificationPreference.builder().userId(CURRENT_USER).build());

        if (request.getOrderUpdates() != null) prefs.setOrderUpdates(request.getOrderUpdates());
        if (request.getPromotions() != null) prefs.setPromotions(request.getPromotions());
        if (request.getDeliveryUpdates() != null) prefs.setDeliveryUpdates(request.getDeliveryUpdates());

        prefs = preferenceRepository.save(prefs);
        return Map.of(
                "orderUpdates", prefs.getOrderUpdates(),
                "promotions", prefs.getPromotions(),
                "deliveryUpdates", prefs.getDeliveryUpdates()
        );
    }

    private NotificationPreference getPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .orderUpdates(true)
                        .promotions(true)
                        .deliveryUpdates(true)
                        .build());
    }

    private boolean shouldSend(NotificationPreference prefs, String type) {
        if (type.startsWith("order.")) return prefs.getOrderUpdates();
        if (type.startsWith("payment.")) return prefs.getOrderUpdates();
        if (type.startsWith("delivery.")) return prefs.getDeliveryUpdates();
        if (type.startsWith("promo")) return prefs.getPromotions();
        return true;
    }

    private String serializeData(Map<String, Object> data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).userId(n.getUserId())
                .type(n.getType()).title(n.getTitle())
                .body(n.getBody()).dataJson(n.getDataJson())
                .isRead(n.getIsRead()).createdAt(n.getCreatedAt())
                .build();
    }
}
