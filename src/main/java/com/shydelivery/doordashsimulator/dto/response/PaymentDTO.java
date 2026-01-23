package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.PaymentMethod;
import com.shydelivery.doordashsimulator.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    
    /**
     * 支付ID
     */
    private Long id;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 客户ID
     */
    private Long customerId;
    
    /**
     * 客户邮箱
     */
    private String customerEmail;
    
    /**
     * 客户姓名
     */
    private String customerName;
    
    /**
     * 支付金额
     */
    private BigDecimal amount;
    
    /**
     * 已退款金额
     */
    private BigDecimal refundedAmount;
    
    /**
     * 可退款金额
     */
    private BigDecimal refundableAmount;
    
    /**
     * 支付方式
     */
    private PaymentMethod paymentMethod;
    
    /**
     * 支付状态
     */
    private PaymentStatus status;
    
    /**
     * 交易ID
     */
    private String transactionId;
    
    /**
     * 支付提供商
     */
    private String paymentProvider;
    
    /**
     * 卡号最后四位
     */
    private String cardLastFour;
    
    /**
     * 失败原因
     */
    private String failureReason;
    
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    
    /**
     * 退款时间
     */
    private LocalDateTime refundedAt;
    
    /**
     * 备注
     */
    private String notes;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否已支付
     */
    private Boolean isPaid;
    
    /**
     * 是否可以退款
     */
    private Boolean canRefund;
}
