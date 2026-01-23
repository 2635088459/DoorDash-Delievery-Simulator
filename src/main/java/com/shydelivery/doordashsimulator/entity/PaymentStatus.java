package com.shydelivery.doordashsimulator.entity;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    /**
     * 待支付
     */
    PENDING,
    
    /**
     * 处理中
     */
    PROCESSING,
    
    /**
     * 支付成功
     */
    COMPLETED,
    
    /**
     * 支付失败
     */
    FAILED,
    
    /**
     * 已退款
     */
    REFUNDED,
    
    /**
     * 部分退款
     */
    PARTIALLY_REFUNDED,
    
    /**
     * 已取消
     */
    CANCELLED
}
