package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Delivery information
 * 
 * Represents delivery details for DRIVER role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDTO {
    
    /**
     * Order information
     */
    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    
    /**
     * Restaurant information
     */
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private String restaurantPhone;
    
    /**
     * Customer information
     */
    private Long customerId;
    private String customerName;
    private String customerPhone;
    
    /**
     * Delivery address
     */
    private String deliveryStreet;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryZipCode;
    
    /**
     * Driver information (if assigned)
     */
    private Long driverId;
    private String driverName;
    private String driverEmail;
    
    /**
     * Delivery details
     */
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;
    private LocalDateTime pickedUpAt;
    
    /**
     * Special instructions
     */
    private String specialInstructions;
    
    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
