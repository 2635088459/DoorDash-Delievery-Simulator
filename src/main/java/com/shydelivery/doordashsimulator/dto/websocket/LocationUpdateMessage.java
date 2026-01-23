package com.shydelivery.doordashsimulator.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WebSocket 位置更新消息
 * 
 * Phase 2: 实时位置追踪
 * 
 * 用于配送员向服务器发送位置更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateMessage {
    
    /**
     * 配送ID
     */
    private Long deliveryId;
    
    /**
     * 配送员ID
     */
    private Long driverId;
    
    /**
     * 当前纬度
     */
    private BigDecimal latitude;
    
    /**
     * 当前经度
     */
    private BigDecimal longitude;
    
    /**
     * 速度 (km/h)
     */
    private Double speed;
    
    /**
     * 方向 (0-360度)
     */
    private Double heading;
    
    /**
     * 更新时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 预计送达时间（分钟）
     */
    private Integer estimatedArrivalMinutes;
}
