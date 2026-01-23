package com.shydelivery.doordashsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shydelivery.doordashsimulator.dto.response.NotificationDTO;
import com.shydelivery.doordashsimulator.entity.Notification;
import com.shydelivery.doordashsimulator.entity.Notification.NotificationType;
import com.shydelivery.doordashsimulator.entity.Notification.Priority;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.NotificationRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notification Service - Phase 2
 * 
 * 实时通知系统，支持：
 * 1. 创建和发送通知
 * 2. WebSocket 实时推送
 * 3. 通知查询和管理
 * 4. 批量操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建并发送通知
     * 
     * @param userId 用户 ID
     * @param type 通知类型
     * @param title 标题
     * @param message 消息内容
     * @return 创建的通知 DTO
     */
    @Transactional
    @Async
    public void createAndSendNotification(
            Long userId,
            NotificationType type,
            String title,
            String message) {
        createAndSendNotification(userId, type, title, message, null, null, Priority.NORMAL, null);
    }

    /**
     * 创建并发送通知（完整版）
     */
    @Transactional
    @Async
    public void createAndSendNotification(
            Long userId,
            NotificationType type,
            String title,
            String message,
            Long orderId,
            Long deliveryId,
            Priority priority,
            Map<String, Object> extraData) {

        log.info("创建通知: userId={}, type={}, title={}", userId, type, title);

        // 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在，ID: " + userId));

        // 创建通知实体
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setOrderId(orderId);
        notification.setDeliveryId(deliveryId);
        notification.setPriority(priority);

        // 处理额外数据
        if (extraData != null && !extraData.isEmpty()) {
            try {
                notification.setExtraData(objectMapper.writeValueAsString(extraData));
            } catch (Exception e) {
                log.warn("无法序列化额外数据: {}", e.getMessage());
            }
        }

        // 保存通知
        Notification saved = notificationRepository.save(notification);

        // 转换为 DTO
        NotificationDTO dto = convertToDTO(saved);

        // WebSocket 实时推送
        sendWebSocketNotification(user.getEmail(), dto);

        log.info("通知已创建并发送: notificationId={}, userId={}, type={}", 
                saved.getId(), userId, type);
    }

    /**
     * 订单状态更新通知
     */
    @Async
    public void notifyOrderStatusChange(Order order, String statusMessage) {
        NotificationType type = mapOrderStatusToNotificationType(order.getStatus());
        
        createAndSendNotification(
                order.getCustomer().getId(),
                type,
                "订单状态更新",
                statusMessage,
                order.getId(),
                null,
                Priority.HIGH,
                createOrderExtraData(order)
        );

        log.info("订单状态通知已发送: orderId={}, status={}", order.getId(), order.getStatus());
    }

    /**
     * 配送员接单通知
     */
    @Async
    public void notifyDriverAssigned(Long customerId, Long orderId, String driverName) {
        String message = String.format("配送员 %s 已接单，正在前往餐厅取餐", driverName);
        
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("driverName", driverName);
        
        createAndSendNotification(
                customerId,
                NotificationType.DELIVERY_ASSIGNED,
                "配送员已分配",
                message,
                orderId,
                null,
                Priority.HIGH,
                extraData
        );
    }

    /**
     * 配送员即将到达通知
     */
    @Async
    public void notifyDriverNearby(Long customerId, Long orderId, int estimatedMinutes) {
        String message = String.format("配送员距离您还有约 %d 分钟，请准备接收订单", estimatedMinutes);
        
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("estimatedMinutes", estimatedMinutes);
        
        createAndSendNotification(
                customerId,
                NotificationType.DELIVERY_NEAR,
                "配送员即将到达",
                message,
                orderId,
                null,
                Priority.URGENT,
                extraData
        );
    }

    /**
     * 支付通知
     */
    @Async
    public void notifyPaymentStatus(Long userId, Long orderId, boolean success, String message) {
        NotificationType type = success ? NotificationType.PAYMENT_SUCCESS : NotificationType.PAYMENT_FAILED;
        Priority priority = success ? Priority.NORMAL : Priority.HIGH;
        
        createAndSendNotification(
                userId,
                type,
                success ? "支付成功" : "支付失败",
                message,
                orderId,
                null,
                priority,
                null
        );
    }

    /**
     * 促销活动通知
     */
    @Async
    public void notifyPromotion(Long userId, String title, String message) {
        createAndSendNotification(
                userId,
                NotificationType.PROMOTION,
                title,
                message,
                null,
                null,
                Priority.LOW,
                null
        );
    }

    /**
     * 获取用户的所有通知
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userEmail));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的未读通知
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userEmail));

        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未读通知数量
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userEmail));

        return notificationRepository.countUnreadByUser(user);
    }

    /**
     * 标记通知为已读
     */
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, String userEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在，ID: " + notificationId));

        // 验证通知所有权
        if (!notification.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("无权访问此通知");
        }

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);

        log.info("通知已标记为已读: notificationId={}, userId={}", notificationId, userEmail);

        return convertToDTO(updated);
    }

    /**
     * 批量标记所有通知为已读
     */
    @Transactional
    public int markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + userEmail));

        int count = notificationRepository.markAllAsReadByUser(user, LocalDateTime.now());

        log.info("批量标记通知为已读: userId={}, count={}", userEmail, count);

        return count;
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long notificationId, String userEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在，ID: " + notificationId));

        // 验证通知所有权
        if (!notification.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("无权删除此通知");
        }

        notificationRepository.delete(notification);

        log.info("通知已删除: notificationId={}, userId={}", notificationId, userEmail);
    }

    /**
     * WebSocket 实时推送通知
     */
    private void sendWebSocketNotification(String userEmail, NotificationDTO notification) {
        try {
            String destination = "/topic/notifications/" + userEmail;
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("WebSocket 通知已发送: destination={}, type={}", destination, notification.getType());
        } catch (Exception e) {
            log.error("WebSocket 通知发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 将通知实体转换为 DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .orderId(notification.getOrderId())
                .deliveryId(notification.getDeliveryId())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .priority(notification.getPriority())
                .extraData(notification.getExtraData())
                .createdAt(notification.getCreatedAt())
                .timeAgo(calculateTimeAgo(notification.getCreatedAt()))
                .build();
    }

    /**
     * 计算时间描述（如："5分钟前"）
     */
    private String calculateTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return duration.toMinutes() + " 分钟前";
        } else if (seconds < 86400) {
            return duration.toHours() + " 小时前";
        } else if (seconds < 604800) {
            return duration.toDays() + " 天前";
        } else {
            return createdAt.toLocalDate().toString();
        }
    }

    /**
     * 映射订单状态到通知类型
     */
    private NotificationType mapOrderStatusToNotificationType(Order.OrderStatus status) {
        return switch (status) {
            case PENDING -> NotificationType.ORDER_CREATED;
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case PREPARING -> NotificationType.ORDER_PREPARING;
            case READY_FOR_PICKUP -> NotificationType.ORDER_READY;
            case PICKED_UP -> NotificationType.ORDER_PICKED_UP;
            case IN_TRANSIT -> NotificationType.ORDER_IN_TRANSIT;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
        };
    }

    /**
     * 创建订单相关的额外数据
     */
    private Map<String, Object> createOrderExtraData(Order order) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderNumber", order.getOrderNumber());
        data.put("restaurantName", order.getRestaurant().getName());
        data.put("totalAmount", order.getTotalAmount().toString());
        data.put("estimatedDelivery", order.getEstimatedDelivery().toString());
        return data;
    }
}
