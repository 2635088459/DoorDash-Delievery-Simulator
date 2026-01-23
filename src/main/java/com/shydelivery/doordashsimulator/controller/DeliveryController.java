package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.response.DeliveryDTO;
import com.shydelivery.doordashsimulator.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Delivery Controller - REST API for delivery management
 * 
 * RBAC 权限控制:
 * - 所有接口需要 DRIVER 角色
 * - 第二层防御: @PreAuthorize 注解
 * - 第三层防御: DeliveryService 中的权限验证
 */
@Slf4j
@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    
    private final DeliveryService deliveryService;
    
    /**
     * 获取可配送的订单列表
     * 
     * RBAC: DRIVER 角色
     * 返回状态为 READY_FOR_PICKUP 且未分配配送员的订单
     * 
     * @return 可配送订单列表
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<DeliveryDTO>> getAvailableOrders(Authentication authentication) {
        log.info("API 调用: GET /deliveries/available, driver={}", authentication.getName());
        List<DeliveryDTO> deliveries = deliveryService.getAvailableOrders(authentication.getName());
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * 获取配送员的所有订单
     * 
     * RBAC: DRIVER 角色
     * 返回已分配给该配送员的所有订单
     * 
     * @return 配送员的订单列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<DeliveryDTO>> getMyDeliveries(Authentication authentication) {
        log.info("API 调用: GET /deliveries/my, driver={}", authentication.getName());
        List<DeliveryDTO> deliveries = deliveryService.getMyDeliveries(authentication.getName());
        return ResponseEntity.ok(deliveries);
    }
    
    /**
     * 接受配送订单
     * 
     * RBAC: DRIVER 角色
     * 将订单分配给该配送员
     * 
     * @param orderId 订单 ID
     * @return 更新后的配送信息
     */
    @PostMapping("/accept/{orderId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DeliveryDTO> acceptOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        log.info("API 调用: POST /deliveries/accept/{}, driver={}", orderId, authentication.getName());
        DeliveryDTO delivery = deliveryService.acceptOrder(orderId, authentication.getName());
        return ResponseEntity.ok(delivery);
    }
    
    /**
     * 标记订单为已取餐
     * 
     * RBAC: DRIVER 角色 + 订单分配验证
     * 配送员从餐厅取餐后调用
     * 
     * @param orderId 订单 ID
     * @return 更新后的配送信息
     */
    @PutMapping("/{orderId}/picked-up")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DeliveryDTO> markAsPickedUp(
            @PathVariable Long orderId,
            Authentication authentication) {
        log.info("API 调用: PUT /deliveries/{}/picked-up, driver={}", orderId, authentication.getName());
        DeliveryDTO delivery = deliveryService.markAsPickedUp(orderId, authentication.getName());
        return ResponseEntity.ok(delivery);
    }
    
    /**
     * 标记订单为配送中
     * 
     * RBAC: DRIVER 角色 + 订单分配验证
     * 配送员开始配送后调用
     * 
     * @param orderId 订单 ID
     * @return 更新后的配送信息
     */
    @PutMapping("/{orderId}/in-transit")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DeliveryDTO> markAsInTransit(
            @PathVariable Long orderId,
            Authentication authentication) {
        log.info("API 调用: PUT /deliveries/{}/in-transit, driver={}", orderId, authentication.getName());
        DeliveryDTO delivery = deliveryService.markAsInTransit(orderId, authentication.getName());
        return ResponseEntity.ok(delivery);
    }
    
    /**
     * 标记订单为已送达
     * 
     * RBAC: DRIVER 角色 + 订单分配验证
     * 配送员送达后调用
     * 
     * @param orderId 订单 ID
     * @return 更新后的配送信息
     */
    @PutMapping("/{orderId}/delivered")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<DeliveryDTO> markAsDelivered(
            @PathVariable Long orderId,
            Authentication authentication) {
        log.info("API 调用: PUT /deliveries/{}/delivered, driver={}", orderId, authentication.getName());
        DeliveryDTO delivery = deliveryService.markAsDelivered(orderId, authentication.getName());
        return ResponseEntity.ok(delivery);
    }
}
