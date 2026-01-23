package com.shydelivery.doordashsimulator.entity;

/**
 * 背景调查状态枚举
 */
public enum BackgroundCheckStatus {
    /**
     * 待审核
     */
    PENDING,
    
    /**
     * 审核中
     */
    IN_REVIEW,
    
    /**
     * 已通过
     */
    APPROVED,
    
    /**
     * 已拒绝
     */
    REJECTED
}
