package com.shydelivery.doordashsimulator.entity;

/**
 * 配送状态枚举
 */
public enum DeliveryStatus {
    /**
     * 已分配 - 订单已分配给配送员，等待配送员接受
     */
    ASSIGNED,
    
    /**
     * 已接受 - 配送员已接受订单，准备前往餐厅
     */
    ACCEPTED,
    
    /**
     * 已取餐 - 配送员已从餐厅取餐
     */
    PICKED_UP,
    
    /**
     * 配送中 - 配送员正在前往客户地址
     */
    IN_TRANSIT,
    
    /**
     * 已送达 - 配送员已送达
     */
    DELIVERED,
    
    /**
     * 已取消 - 配送已取消
     */
    CANCELLED
}
