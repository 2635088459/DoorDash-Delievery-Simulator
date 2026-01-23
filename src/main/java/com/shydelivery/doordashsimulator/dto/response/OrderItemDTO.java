package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for OrderItem entity
 * 
 * Represents a single item within an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    
    private Long id;
    
    /**
     * Menu item information
     */
    private Long menuItemId;
    private String menuItemName;
    private String menuItemDescription;
    
    /**
     * Order details
     */
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal; // quantity * unitPrice
    
    /**
     * Special instructions for this item
     */
    private String specialInstructions;
}
