package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车商品项响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    
    private Long id;
    private Long menuItemId;
    private String menuItemName;
    private String menuItemDescription;
    private BigDecimal priceAtAdd;
    private BigDecimal currentPrice;
    private Boolean priceChanged;
    private Integer quantity;
    private BigDecimal subtotal;
    private String specialInstructions;
    private Boolean isAvailable;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
