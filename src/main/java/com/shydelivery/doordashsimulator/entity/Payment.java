package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 * 记录所有订单的支付信息和交易历史
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_customer", columnList = "customer_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_transaction", columnList = "transaction_id"),
        @Index(name = "idx_payment_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的订单
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    /**
     * 付款用户（冗余字段，便于查询）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    /**
     * 支付金额
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * 支付方式
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod paymentMethod;
    
    /**
     * 支付状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;
    
    /**
     * 第三方支付交易ID（模拟）
     */
    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;
    
    /**
     * 支付提供商（如 Stripe, PayPal）
     */
    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;
    
    /**
     * 最后四位卡号（用于信用卡/借记卡）
     */
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;
    
    /**
     * 已退款金额
     */
    @Column(name = "refunded_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    /**
     * 支付失败原因
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    /**
     * 支付完成时间
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    /**
     * 退款时间
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
    
    /**
     * 备注
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // ==================== 业务方法 ====================
    
    /**
     * 标记支付成功
     */
    public void markAsCompleted(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.paidAt = LocalDateTime.now();
    }
    
    /**
     * 标记支付失败
     */
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
    
    /**
     * 处理退款
     */
    public void processRefund(BigDecimal refundAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额必须大于0");
        }
        
        BigDecimal totalRefundable = this.amount.subtract(this.refundedAmount);
        if (refundAmount.compareTo(totalRefundable) > 0) {
            throw new IllegalArgumentException("退款金额超过可退款金额");
        }
        
        this.refundedAmount = this.refundedAmount.add(refundAmount);
        
        // 判断是全额退款还是部分退款
        if (this.refundedAmount.compareTo(this.amount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
        
        this.refundedAt = LocalDateTime.now();
    }
    
    /**
     * 获取可退款金额
     */
    public BigDecimal getRefundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }
    
    /**
     * 检查是否可以退款
     */
    public boolean canRefund() {
        return this.status == PaymentStatus.COMPLETED || 
               this.status == PaymentStatus.PARTIALLY_REFUNDED;
    }
    
    /**
     * 检查是否已完成支付
     */
    public boolean isPaid() {
        return this.status == PaymentStatus.COMPLETED || 
               this.status == PaymentStatus.PARTIALLY_REFUNDED ||
               this.status == PaymentStatus.REFUNDED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment payment = (Payment) o;
        return id != null && id.equals(payment.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
