package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.CreateOrderRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateOrderStatusRequest;
import com.shydelivery.doordashsimulator.dto.response.OrderDTO;
import com.shydelivery.doordashsimulator.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller - 订单管理 REST API
 * 
 * RBAC 权限模型:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 端点                            │ 角色              │ 权限   │
 * ├─────────────────────────────────────────────────────────────┤
 * │ POST   /orders                  │ CUSTOMER          │ 创建订单│
 * │ GET    /orders/my               │ CUSTOMER          │ 我的订单│
 * │ GET    /orders/{id}             │ CUSTOMER/OWNER    │ 订单详情│
 * │ PUT    /orders/{id}/status      │ OWNER/DRIVER      │ 更新状态│
 * │ DELETE /orders/{id}             │ CUSTOMER          │ 取消订单│
 * │ GET    /orders/restaurant/{id}  │ RESTAURANT_OWNER  │ 餐厅订单│
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 安全层次:
 * 1. JWT Filter - 验证 token 有效性
 * 2. @PreAuthorize - 验证用户角色
 * 3. AuthorizationService - 验证资源所有权
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 创建订单 (仅 CUSTOMER)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 
     * @param request 创建订单请求
     * @param authentication 认证信息
     * @return 创建的订单
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        
        log.info("API - 创建订单: restaurant={}, user={}", 
                request.getRestaurantId(), authentication.getName());
        
        OrderDTO order = orderService.createOrder(request, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    /**
     * 获取我的订单列表 (仅 CUSTOMER)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 
     * @param authentication 认证信息
     * @return 我的订单列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        
        log.info("API - 获取我的订单: user={}", authentication.getName());
        
        List<OrderDTO> orders = orderService.getMyOrders(authentication.getName());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * 获取订单详情 (CUSTOMER 或 RESTAURANT_OWNER)
     * 
     * RBAC: @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER')")
     * 权限验证: AuthorizationService.verifyOrderAccess()
     * 
     * @param id 订单 ID
     * @param authentication 认证信息
     * @return 订单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER')")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("API - 获取订单详情: orderId={}, user={}", id, authentication.getName());
        
        OrderDTO order = orderService.getOrderById(id, authentication.getName());
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * 更新订单状态 (RESTAURANT_OWNER 或 DRIVER)
     * 
     * RBAC: @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'DRIVER')")
     * 权限验证: AuthorizationService.verifyOrderAccess()
     * 
     * @param id 订单 ID
     * @param request 更新状态请求
     * @param authentication 认证信息
     * @return 更新后的订单
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'DRIVER')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {
        
        log.info("API - 更新订单状态: orderId={}, newStatus={}, user={}", 
                id, request.getStatus(), authentication.getName());
        
        OrderDTO order = orderService.updateOrderStatus(id, request, authentication.getName());
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * 取消订单 (仅 CUSTOMER, 仅 PENDING 状态)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 权限验证: AuthorizationService.verifyOrderCustomer()
     * 
     * @param id 订单 ID
     * @param authentication 认证信息
     * @return 取消后的订单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("API - 取消订单: orderId={}, user={}", id, authentication.getName());
        
        OrderDTO order = orderService.cancelOrder(id, authentication.getName());
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * 获取餐厅的所有订单 (仅 RESTAURANT_OWNER)
     * 
     * RBAC: @PreAuthorize("hasRole('RESTAURANT_OWNER')")
     * 权限验证: AuthorizationService.verifyRestaurantOwnership()
     * 
     * @param restaurantId 餐厅 ID
     * @param authentication 认证信息
     * @return 餐厅订单列表
     */
    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<OrderDTO>> getRestaurantOrders(
            @PathVariable Long restaurantId,
            Authentication authentication) {
        
        log.info("API - 获取餐厅订单: restaurantId={}, user={}", 
                restaurantId, authentication.getName());
        
        List<OrderDTO> orders = orderService.getRestaurantOrders(
                restaurantId, authentication.getName());
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * 餐厅老板标记订单为准备完成，可以取餐 (仅 RESTAURANT_OWNER)
     * 
     * RBAC: @PreAuthorize("hasRole('RESTAURANT_OWNER')")
     * 权限验证: AuthorizationService.verifyOrderRestaurantOwner()
     * 
     * @param id 订单 ID
     * @param authentication 认证信息
     * @return 更新后的订单
     */
    @PutMapping("/{id}/ready-for-pickup")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<OrderDTO> markOrderReadyForPickup(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("API - 标记订单为准备完成: orderId={}, user={}", id, authentication.getName());
        
        OrderDTO order = orderService.markOrderReadyForPickup(id, authentication.getName());
        
        return ResponseEntity.ok(order);
    }
}
