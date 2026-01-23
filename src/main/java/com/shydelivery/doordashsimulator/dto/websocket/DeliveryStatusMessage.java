package com.shydelivery.doordashsimulator.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket 配送状态更新消息
 * 
 * Phase 2: 实时状态通知
 * 
 * 服务器向客户端推送配送状态变更
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusMessage {
    
    /**
     * 配送ID
     */
    private Long deliveryId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 配送员ID
     */
    private Long driverId;
    
    /**
     * 配送员姓名
     */
    private String driverName;
    
    /**
     * 配送状态
     */
    private String status;
    
    /**
     * 当前位置 - 纬度
     */
    private BigDecimal currentLatitude;
    
    /**
     * 当前位置 - 经度
     */
    private BigDecimal currentLongitude;
    
    /**
     * 预计送达时间
     */
    private LocalDateTime estimatedArrival;
    
    /**
     * 状态更新时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 状态描述
     */
    private String message;
}
