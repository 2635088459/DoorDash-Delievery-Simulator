package com.shydelivery.doordashsimulator.entity;

/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    /**
     * 信用卡
     */
    CREDIT_CARD,
    
    /**
     * 借记卡
     */
    DEBIT_CARD,
    
    /**
     * 数字钱包（如 Apple Pay, Google Pay）
     */
    DIGITAL_WALLET,
    
    /**
     * 现金（货到付款）
     */
    CASH,
    
    /**
     * PayPal
     */
    PAYPAL
}
