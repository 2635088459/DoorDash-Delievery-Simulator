package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.response.DeliveryDTO;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.util.DeliveryFeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivery Service - 配送业务逻辑
 * 
 * Phase 2增强: 集成配送费计算器
 * 
 * RBAC 权限模型:
 * - DRIVER: 查看可配送订单, 接单, 更新配送状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {
    
    private final OrderRepository orderRepository;
    private final AuthorizationService authorizationService;
    private final DeliveryFeeCalculator feeCalculator;  // Phase 2: 新增
    
    /**
     * 获取可配送的订单列表 (DRIVER 角色)
     * 状态为 READY_FOR_PICKUP 且未分配配送员的订单
     * 
     * @param driverEmail 配送员邮箱
     * @return 可配送订单列表
     */
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAvailableOrders(String driverEmail) {
        log.info("获取可配送订单: driver={}", driverEmail);
        
        // 验证 DRIVER 角色
        authorizationService.getUserAndVerifyDriver(driverEmail);
        
        // 查询未分配配送员且状态为 READY_FOR_PICKUP 的订单
        List<Order> orders = orderRepository.findByStatusAndDriverIsNull(OrderStatus.READY_FOR_PICKUP);
        
        log.info("找到 {} 个可配送订单", orders.size());
        
        return orders.stream()
                .map(this::convertToDeliveryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取配送员的所有订单 (DRIVER 角色)
     * 包括已接单但未完成的订单
     * 
     * @param driverEmail 配送员邮箱
     * @return 配送员的订单列表
     */
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getMyDeliveries(String driverEmail) {
        log.info("获取我的配送订单: driver={}", driverEmail);
        
        // 验证 DRIVER 角色并获取用户
        User driver = authorizationService.getUserAndVerifyDriver(driverEmail);
        
        // 查询分配给该配送员的所有订单
        List<Order> orders = orderRepository.findByDriver(driver);
        
        log.info("配送员 {} 有 {} 个订单", driverEmail, orders.size());
        
        return orders.stream()
                .map(this::convertToDeliveryDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 接受配送订单 (DRIVER 角色)
     * 将订单分配给配送员
     * 
     * @param orderId 订单 ID
     * @param driverEmail 配送员邮箱
     * @return 更新后的配送信息
     */
    @Transactional
    public DeliveryDTO acceptOrder(Long orderId, String driverEmail) {
        log.info("配送员接单: orderId={}, driver={}", orderId, driverEmail);
        
        // 验证 DRIVER 角色并获取用户
        User driver = authorizationService.getUserAndVerifyDriver(driverEmail);
        
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证订单状态
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("订单状态不是 READY_FOR_PICKUP，无法接单");
        }
        
        // 验证订单未被其他配送员接单
        if (order.getDriver() != null) {
            throw new IllegalStateException("订单已被其他配送员接单");
        }
        
        // 分配配送员
        order.setDriver(driver);
        
        // 保存
        Order saved = orderRepository.save(order);
        log.info("配送员 {} 成功接单 {}", driverEmail, orderId);
        
        return convertToDeliveryDTO(saved);
    }
    
    /**
     * 更新配送状态为 PICKED_UP (DRIVER 角色)
     * 配送员从餐厅取餐
     * 
     * @param orderId 订单 ID
     * @param driverEmail 配送员邮箱
     * @return 更新后的配送信息
     */
    @Transactional
    public DeliveryDTO markAsPickedUp(Long orderId, String driverEmail) {
        log.info("标记订单已取餐: orderId={}, driver={}", orderId, driverEmail);
        
        // 验证配送员分配
        authorizationService.verifyDriverAssignment(orderId, driverEmail);
        
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证状态（必须是 READY_FOR_PICKUP）
        if (order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("只能对 READY_FOR_PICKUP 状态的订单标记取餐");
        }
        
        // 更新状态
        order.setStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(LocalDateTime.now());
        
        // 保存
        Order saved = orderRepository.save(order);
        log.info("订单 {} 已标记为已取餐", orderId);
        
        return convertToDeliveryDTO(saved);
    }
    
    /**
     * 更新配送状态为 IN_TRANSIT (DRIVER 角色)
     * 配送员正在配送中
     * 
     * @param orderId 订单 ID
     * @param driverEmail 配送员邮箱
     * @return 更新后的配送信息
     */
    @Transactional
    public DeliveryDTO markAsInTransit(Long orderId, String driverEmail) {
        log.info("标记订单配送中: orderId={}, driver={}", orderId, driverEmail);
        
        // 验证配送员分配
        authorizationService.verifyDriverAssignment(orderId, driverEmail);
        
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证状态（必须是 PICKED_UP）
        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new IllegalStateException("只能对 PICKED_UP 状态的订单标记配送中");
        }
        
        // 更新状态
        order.setStatus(OrderStatus.IN_TRANSIT);
        
        // 保存
        Order saved = orderRepository.save(order);
        log.info("订单 {} 已标记为配送中", orderId);
        
        return convertToDeliveryDTO(saved);
    }
    
    /**
     * 更新配送状态为 DELIVERED (DRIVER 角色)
     * 订单已送达
     * 
     * @param orderId 订单 ID
     * @param driverEmail 配送员邮箱
     * @return 更新后的配送信息
     */
    @Transactional
    public DeliveryDTO markAsDelivered(Long orderId, String driverEmail) {
        log.info("标记订单已送达: orderId={}, driver={}", orderId, driverEmail);
        
        // 验证配送员分配
        authorizationService.verifyDriverAssignment(orderId, driverEmail);
        
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证状态（必须是 IN_TRANSIT）
        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new IllegalStateException("只能对 IN_TRANSIT 状态的订单标记已送达");
        }
        
        // 更新状态
        order.setStatus(OrderStatus.DELIVERED);
        order.setActualDelivery(LocalDateTime.now());
        
        // 保存
        Order saved = orderRepository.save(order);
        log.info("订单 {} 已送达", orderId);
        
        return convertToDeliveryDTO(saved);
    }
    
    /**
     * 将 Order 实体转换为 DeliveryDTO
     */
    private DeliveryDTO convertToDeliveryDTO(Order order) {
        return DeliveryDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .restaurantAddress(order.getRestaurant().getStreetAddress() + ", " + 
                                  order.getRestaurant().getCity() + ", " + 
                                  order.getRestaurant().getState())
                .restaurantPhone(order.getRestaurant().getPhoneNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .customerPhone(order.getCustomer().getPhoneNumber())
                .deliveryStreet(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getStreetAddress() : null)
                .deliveryCity(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getCity() : null)
                .deliveryState(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getState() : null)
                .deliveryZipCode(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getZipCode() : null)
                .driverId(order.getDriver() != null ? order.getDriver().getId() : null)
                .driverName(order.getDriver() != null ? 
                           order.getDriver().getFirstName() + " " + order.getDriver().getLastName() : null)
                .driverEmail(order.getDriver() != null ? order.getDriver().getEmail() : null)
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .estimatedDelivery(order.getEstimatedDelivery())
                .actualDelivery(order.getActualDelivery())
                .pickedUpAt(order.getPickedUpAt())
                .specialInstructions(order.getSpecialInstructions())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
