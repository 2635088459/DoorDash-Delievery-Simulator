package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new menu item
 * 
 * RBAC: Only RESTAURANT_OWNER role can create menu items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuItemRequest {
    
    /**
     * ID of the restaurant this menu item belongs to
     */
    @NotNull(message = "餐厅 ID 不能为空")
    private Long restaurantId;
    
    /**
     * Name of the menu item
     */
    @NotBlank(message = "菜品名称不能为空")
    private String name;
    
    /**
     * Description of the menu item
     */
    private String description;
    
    /**
     * Category (e.g., "Appetizer", "Main Course", "Dessert", "Beverage")
     */
    @NotBlank(message = "分类不能为空")
    private String category;
    
    /**
     * Price of the menu item
     */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;
    
    /**
     * URL to the image of the menu item (optional)
     */
    private String imageUrl;
    
    /**
     * Whether this item is available for ordering
     */
    private Boolean isAvailable = true;
    
    /**
     * Whether this item is suitable for vegetarians
     */
    private Boolean isVegetarian = false;
    
    /**
     * Whether this item is suitable for vegans
     */
    private Boolean isVegan = false;
    
    /**
     * Spiciness level (0-5, optional)
     */
    private Integer spicyLevel = 0;
}
