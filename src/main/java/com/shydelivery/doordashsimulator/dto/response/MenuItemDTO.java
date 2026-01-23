package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for MenuItem entity
 * 
 * Contains all menu item information visible to users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {
    
    private Long id;
    
    /**
     * Restaurant information
     */
    private Long restaurantId;
    private String restaurantName;
    
    /**
     * Menu item details
     */
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    
    /**
     * Availability and dietary information
     */
    private Boolean isAvailable;
    private Boolean isVegetarian;
    private Boolean isVegan;
    private Integer spicyLevel;
    
    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
