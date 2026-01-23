package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.websocket.DeliveryStatusMessage;
import com.shydelivery.doordashsimulator.dto.websocket.LocationUpdateMessage;
import com.shydelivery.doordashsimulator.entity.Delivery;
import com.shydelivery.doordashsimulator.entity.Driver;
import com.shydelivery.doordashsimulator.repository.DeliveryRepository;
import com.shydelivery.doordashsimulator.repository.DriverRepository;
import com.shydelivery.doordashsimulator.service.DriverService;
import com.shydelivery.doordashsimulator.util.DeliveryFeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket Controller
 * 实时位置和状态更新
 * 
 * Phase 2: WebSocket 实时通信
 * 
 * Endpoints:
 * - /app/location/update - 配送员发送位置更新
 * - /topic/delivery/{deliveryId} - 订阅配送进度（客户端订阅）
 * - /topic/driver/{driverId} - 订阅配送员位置（管理端订阅）
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final DriverRepository driverRepository;
    private final DeliveryRepository deliveryRepository;
    private final DriverService driverService;
    private final DeliveryFeeCalculator feeCalculator;
    
    /**
     * 处理配送员位置更新
     * 
     * 客户端发送到: /app/location/update
     * 服务器广播到: /topic/delivery/{deliveryId}
     * 
     * @param message 位置更新消息
     */
    @MessageMapping("/location/update")
    public void handleLocationUpdate(@Payload LocationUpdateMessage message) {
        log.info("收到位置更新: deliveryId={}, lat={}, lon={}", 
                message.getDeliveryId(), message.getLatitude(), message.getLongitude());
        
        try {
            // 1. 更新配送员位置
            Driver driver = driverRepository.findById(message.getDriverId())
                    .orElseThrow(() -> new RuntimeException("配送员不存在"));
            
            driver.updateLocation(message.getLatitude(), message.getLongitude());
            driverRepository.save(driver);
            
            // 2. 获取配送信息
            Delivery delivery = deliveryRepository.findById(message.getDeliveryId())
                    .orElseThrow(() -> new RuntimeException("配送不存在"));
            
            // 3. 计算预计送达时间
            Double remainingDistance = driverService.calculateDistance(
                    message.getLatitude(),
                    message.getLongitude(),
                    delivery.getDeliveryLatitude(),
                    delivery.getDeliveryLongitude()
            );
            
            int estimatedMinutes = feeCalculator.estimateDeliveryTime(remainingDistance);
            
            // 4. 构建状态消息
            DeliveryStatusMessage statusMessage = DeliveryStatusMessage.builder()
                    .deliveryId(delivery.getId())
                    .orderId(delivery.getOrder().getId())
                    .driverId(driver.getId())
                    .driverName(driver.getUser().getFirstName() + " " + driver.getUser().getLastName())
                    .status(delivery.getStatus().name())
                    .currentLatitude(message.getLatitude())
                    .currentLongitude(message.getLongitude())
                    .estimatedArrival(LocalDateTime.now().plusMinutes(estimatedMinutes))
                    .timestamp(LocalDateTime.now())
                    .message(String.format("配送员距离您还有 %.1f 公里，预计 %d 分钟送达", 
                            remainingDistance, estimatedMinutes))
                    .build();
            
            // 5. 向特定配送的订阅者广播
            messagingTemplate.convertAndSend(
                    "/topic/delivery/" + delivery.getId(),
                    statusMessage
            );
            
            // 6. 向配送员位置的订阅者广播（管理端可以订阅）
            messagingTemplate.convertAndSend(
                    "/topic/driver/" + driver.getId(),
                    message
            );
            
            log.info("位置更新已广播: deliveryId={}, 剩余距离={}km, ETA={}分钟", 
                    delivery.getId(), remainingDistance, estimatedMinutes);
            
        } catch (Exception e) {
            log.error("处理位置更新失败", e);
        }
    }
    
    /**
     * 处理配送状态变更
     * 
     * 当配送状态改变时（如接单、取餐、送达等），推送通知给客户
     * 
     * @param deliveryId 配送ID
     * @param message 状态消息
     */
    public void broadcastDeliveryStatus(Long deliveryId, DeliveryStatusMessage message) {
        log.info("广播配送状态: deliveryId={}, status={}", deliveryId, message.getStatus());
        
        messagingTemplate.convertAndSend(
                "/topic/delivery/" + deliveryId,
                message
        );
    }
    
    /**
     * 测试端点：模拟位置更新
     * 
     * 客户端发送到: /app/location/test
     * 服务器广播到: /topic/location/test
     */
    @MessageMapping("/location/test")
    @SendTo("/topic/location/test")
    public String testLocation(String message) {
        log.info("收到测试消息: {}", message);
        return "Echo: " + message + " at " + LocalDateTime.now();
    }
}
