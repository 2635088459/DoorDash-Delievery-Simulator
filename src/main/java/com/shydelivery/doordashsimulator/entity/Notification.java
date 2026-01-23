package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification 实体 - Phase 2
 * 
 * 存储系统通知，支持多种通知类型：
 * - 订单状态更新
 * - 配送状态更新
 * - 系统消息
 * - 促销活动
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_is_read", columnList = "is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知接收者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType type;

    /**
     * 通知标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 通知内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * 关联的订单 ID（可选）
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * 关联的配送 ID（可选）
     */
    @Column(name = "delivery_id")
    private Long deliveryId;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 阅读时间
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 优先级
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.NORMAL;

    /**
     * 额外数据（JSON 格式，用于存储动态数据）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 通知类型枚举
     */
    public enum NotificationType {
        ORDER_CREATED,          // 订单已创建
        ORDER_CONFIRMED,        // 订单已确认
        ORDER_PREPARING,        // 餐厅正在准备
        ORDER_READY,            // 订单已准备好
        ORDER_PICKED_UP,        // 配送员已取餐
        ORDER_IN_TRANSIT,       // 配送中
        ORDER_DELIVERED,        // 订单已送达
        ORDER_CANCELLED,        // 订单已取消
        
        DELIVERY_ASSIGNED,      // 配送任务已分配
        DELIVERY_ACCEPTED,      // 配送员已接单
        DELIVERY_REJECTED,      // 配送员拒绝订单
        DELIVERY_NEAR,          // 配送员即将到达
        
        PAYMENT_SUCCESS,        // 支付成功
        PAYMENT_FAILED,         // 支付失败
        REFUND_PROCESSED,       // 退款已处理
        
        PROMOTION,              // 促销活动
        SYSTEM_MESSAGE          // 系统消息
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW,        // 低优先级
        NORMAL,     // 普通
        HIGH,       // 高优先级
        URGENT      // 紧急
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 检查通知是否已过期（7天未读）
     */
    public boolean isExpired() {
        if (isRead) {
            return false;
        }
        return createdAt.plusDays(7).isBefore(LocalDateTime.now());
    }
}
