package com.fooddelivery.notification.controller;

import com.fooddelivery.common.lib.dto.ApiResponse;
import com.fooddelivery.notification.dto.NotificationResponse;
import com.fooddelivery.notification.dto.PreferenceUpdateRequest;
import com.fooddelivery.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageResult = notificationService.getUserNotifications(page, size);
        return ApiResponse.ok(pageResult.getContent());
    }

    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable UUID id) {
        return ApiResponse.ok("Marked as read", notificationService.markAsRead(id));
    }

    @GetMapping("/preferences")
    public ApiResponse<Map<String, Boolean>> getPreferences() {
        return ApiResponse.ok(notificationService.getPreferencesResponse());
    }

    @PutMapping("/preferences")
    public ApiResponse<Map<String, Boolean>> updatePreferences(@RequestBody PreferenceUpdateRequest request) {
        return ApiResponse.ok("Preferences updated", notificationService.updatePreferences(request));
    }
}
