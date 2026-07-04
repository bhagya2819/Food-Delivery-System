package com.fooddelivery.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "order_updates", nullable = false)
    @Builder.Default
    private Boolean orderUpdates = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean promotions = true;

    @Column(name = "delivery_updates", nullable = false)
    @Builder.Default
    private Boolean deliveryUpdates = true;
}
