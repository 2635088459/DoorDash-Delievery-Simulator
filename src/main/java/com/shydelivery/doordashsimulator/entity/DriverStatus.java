package com.shydelivery.doordashsimulator.entity;

/**
 * 配送员状态枚举
 */
public enum DriverStatus {
    /**
     * 离线 - 配送员未登录或已下线
     */
    OFFLINE,
    
    /**
     * 在线 - 配送员已上线，可以接单
     */
    ONLINE,
    
    /**
     * 忙碌 - 配送员正在配送中，暂时无法接新单
     */
    BUSY,
    
    /**
     * 暂停 - 配送员已上线但暂停接单（休息中）
     */
    INACTIVE
}
