package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing menu item
 * 
 * RBAC: Only RESTAURANT_OWNER role can update menu items (with ownership verification)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMenuItemRequest {
    
    /**
     * Name of the menu item
     */
    private String name;
    
    /**
     * Description of the menu item
     */
    private String description;
    
    /**
     * Category
     */
    private String category;
    
    /**
     * Price of the menu item
     */
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;
    
    /**
     * URL to the image of the menu item
     */
    private String imageUrl;
    
    /**
     * Whether this item is available for ordering
     */
    private Boolean isAvailable;
    
    /**
     * Whether this item is suitable for vegetarians
     */
    private Boolean isVegetarian;
    
    /**
     * Whether this item is suitable for vegans
     */
    private Boolean isVegan;
    
    /**
     * Spiciness level (0-5)
     */
    private Integer spicyLevel;
}
