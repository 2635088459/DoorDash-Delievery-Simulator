package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Delivery 实体 - 配送记录
 * 
 * 功能:
 * - 订单配送管理
 * - 配送状态跟踪
 * - 配送员分配
 * - 时间追踪（接单、取餐、送达）
 * - 配送费用和小费管理
 * - 配送评价
 */
@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_delivery_order", columnList = "order_id"),
    @Index(name = "idx_delivery_driver", columnList = "driver_id"),
    @Index(name = "idx_delivery_status", columnList = "status"),
    @Index(name = "idx_delivery_assigned_at", columnList = "assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ==================== 关联关系 ====================
    
    /**
     * 关联的订单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    /**
     * 配送员
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    // ==================== 配送状态 ====================
    
    /**
     * 配送状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;
    
    // ==================== 时间信息 ====================
    
    /**
     * 分配时间
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    /**
     * 接受时间
     */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    /**
     * 取餐时间
     */
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;
    
    /**
     * 送达时间
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    /**
     * 预计送达时间
     */
    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;
    
    // ==================== 位置信息 ====================
    
    /**
     * 取餐地点 - 纬度（餐厅位置）
     */
    @Column(name = "pickup_latitude", precision = 10, scale = 8)
    private BigDecimal pickupLatitude;
    
    /**
     * 取餐地点 - 经度（餐厅位置）
     */
    @Column(name = "pickup_longitude", precision = 11, scale = 8)
    private BigDecimal pickupLongitude;
    
    /**
     * 送达地点 - 纬度（客户地址）
     */
    @Column(name = "delivery_latitude", precision = 10, scale = 8)
    private BigDecimal deliveryLatitude;
    
    /**
     * 送达地点 - 经度（客户地址）
     */
    @Column(name = "delivery_longitude", precision = 11, scale = 8)
    private BigDecimal deliveryLongitude;
    
    // ==================== 距离和时间 ====================
    
    /**
     * 配送距离（公里）
     */
    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;
    
    /**
     * 预计配送时长（分钟）
     */
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    
    /**
     * 实际配送时长（分钟）
     */
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    // ==================== 收益信息 ====================
    
    /**
     * 配送费
     */
    @Column(name = "delivery_fee", precision = 8, scale = 2)
    private BigDecimal deliveryFee;
    
    /**
     * 小费金额
     */
    @Column(name = "tip_amount", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;
    
    /**
     * 配送员总收益（配送费 + 小费）
     */
    @Column(name = "total_earnings", precision = 8, scale = 2)
    private BigDecimal totalEarnings;
    
    // ==================== 评价信息 ====================
    
    /**
     * 客户对配送员的评分（1-5星）
     */
    @Column(name = "driver_rating")
    private Integer driverRating;
    
    /**
     * 客户反馈
     */
    @Column(name = "customer_feedback", columnDefinition = "TEXT")
    private String customerFeedback;
    
    // ==================== 其他信息 ====================
    
    /**
     * 配送备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * 取消原因
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    
    /**
     * 送达证明（照片URL等）
     */
    @Column(name = "delivery_proof_url", length = 500)
    private String deliveryProofUrl;
    
    // ==================== 时间戳 ====================
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // ==================== 业务方法 ====================
    
    /**
     * 配送员接受订单
     */
    public void accept() {
        if (this.status != DeliveryStatus.ASSIGNED) {
            throw new IllegalStateException("只有已分配的订单可以被接受");
        }
        this.status = DeliveryStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }
    
    /**
     * 配送员取餐
     */
    public void pickUp() {
        if (this.status != DeliveryStatus.ACCEPTED) {
            throw new IllegalStateException("只有已接受的订单可以取餐");
        }
        this.status = DeliveryStatus.PICKED_UP;
        this.pickedUpAt = LocalDateTime.now();
    }
    
    /**
     * 开始配送
     */
    public void startDelivery() {
        if (this.status != DeliveryStatus.PICKED_UP) {
            throw new IllegalStateException("只有已取餐的订单可以开始配送");
        }
        this.status = DeliveryStatus.IN_TRANSIT;
    }
    
    /**
     * 完成配送
     */
    public void complete() {
        if (this.status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("只有配送中的订单可以完成");
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        
        // 计算实际配送时长
        if (this.assignedAt != null) {
            this.actualDurationMinutes = (int) java.time.Duration
                .between(this.assignedAt, this.deliveredAt).toMinutes();
        }
        
        // 计算总收益
        this.totalEarnings = this.deliveryFee.add(this.tipAmount);
    }
    
    /**
     * 取消配送
     */
    public void cancel(String reason) {
        if (this.status == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("已完成的配送不能取消");
        }
        this.status = DeliveryStatus.CANCELLED;
        this.cancellationReason = reason;
    }
    
    /**
     * 添加小费
     */
    public void addTip(BigDecimal tipAmount) {
        this.tipAmount = this.tipAmount.add(tipAmount);
        this.totalEarnings = this.deliveryFee.add(this.tipAmount);
    }
    
    /**
     * 客户评分
     */
    public void rate(Integer rating, String feedback) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }
        this.driverRating = rating;
        this.customerFeedback = feedback;
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return this.status == DeliveryStatus.DELIVERED;
    }
    
    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return this.status == DeliveryStatus.CANCELLED;
    }
    
    /**
     * 检查是否可以接受
     */
    public boolean canBeAccepted() {
        return this.status == DeliveryStatus.ASSIGNED;
    }
}
