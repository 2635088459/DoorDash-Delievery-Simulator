package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.Notification.NotificationType;
import com.shydelivery.doordashsimulator.entity.Notification.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification DTO - Phase 2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    private Long id;
    
    /**
     * 通知类型
     */
    private NotificationType type;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String message;
    
    /**
     * 关联的订单 ID
     */
    private Long orderId;
    
    /**
     * 关联的配送 ID
     */
    private Long deliveryId;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 阅读时间
     */
    private LocalDateTime readAt;
    
    /**
     * 优先级
     */
    private Priority priority;
    
    /**
     * 额外数据
     */
    private String extraData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 时间描述（如："5分钟前"）
     */
    private String timeAgo;
}
