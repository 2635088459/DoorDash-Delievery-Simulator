package com.shydelivery.doordashsimulator.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Order.PaymentMethod;
import com.shydelivery.doordashsimulator.entity.Order.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Order entity
 * 
 * Contains all order information visible to authorized users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    
    private Long id;
    
    /**
     * Customer information
     */
    private Long customerId;
    private String customerName;
    private String customerEmail;
    
    /**
     * Restaurant information
     */
    private Long restaurantId;
    private String restaurantName;
    
    /**
     * Delivery address information
     */
    private Long deliveryAddressId;
    private String deliveryAddressStreet;
    private String deliveryAddressCity;
    private String deliveryAddressState;
    private String deliveryAddressZipCode;
    
    /**
     * Order details
     */
    private String orderNumber;
    private OrderStatus status;
    
    /**
     * Financial information
     */
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    
    /**
     * Payment information
     */
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Long paymentId;  // 关联的支付记录ID
    private String paymentTransactionId;  // 支付交易ID
    
    /**
     * Delivery information
     */
    private String specialInstructions;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;
    
    /**
     * Phase 2: 配送距离和动态定价信息
     */
    private BigDecimal deliveryDistanceKm;
    private String weatherCondition;
    private Boolean badWeatherSurcharge;
    private Boolean peakHourSurcharge;
    
    /**
     * Order items
     */
    private List<OrderItemDTO> items;
    
    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
