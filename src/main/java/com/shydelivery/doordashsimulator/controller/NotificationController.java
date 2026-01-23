package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.response.NotificationDTO;
import com.shydelivery.doordashsimulator.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification Controller - Phase 2
 * 
 * 实时通知系统 REST API
 * 
 * Endpoints:
 * - GET /notifications - 获取所有通知
 * - GET /notifications/unread - 获取未读通知
 * - GET /notifications/unread/count - 获取未读通知数量
 * - PUT /notifications/{id}/read - 标记通知为已读
 * - PUT /notifications/read-all - 批量标记所有通知为已读
 * - DELETE /notifications/{id} - 删除通知
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理", description = "实时通知系统 API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的所有通知
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取所有通知", description = "获取当前用户的所有通知（按时间倒序）")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("获取所有通知: user={}", userEmail);

        List<NotificationDTO> notifications = notificationService.getUserNotifications(userEmail);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取未读通知
     */
    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读通知", description = "获取当前用户的所有未读通知")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("获取未读通知: user={}", userEmail);

        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userEmail);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取未读通知数量", description = "获取当前用户的未读通知数量（用于显示小红点）")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("获取未读通知数量: user={}", userEmail);

        Long count = notificationService.getUnreadCount(userEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("hasUnread", count > 0);

        return ResponseEntity.ok(response);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读状态")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("标记通知为已读: notificationId={}, user={}", id, userEmail);

        NotificationDTO notification = notificationService.markAsRead(id, userEmail);

        return ResponseEntity.ok(notification);
    }

    /**
     * 批量标记所有通知为已读
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "全部标记为已读", description = "将当前用户的所有未读通知标记为已读")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("批量标记通知为已读: user={}", userEmail);

        int count = notificationService.markAllAsRead(userEmail);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "已标记 " + count + " 条通知为已读");
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除通知", description = "删除指定的通知")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("删除通知: notificationId={}, user={}", id, userEmail);

        notificationService.deleteNotification(id, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "通知已删除");

        return ResponseEntity.ok(response);
    }
}
