package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏菜品响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteMenuItemDTO {
    
    /**
     * 收藏记录ID
     */
    private Long favoriteId;
    
    /**
     * 菜品ID
     */
    private Long menuItemId;
    
    /**
     * 菜品名称
     */
    private String menuItemName;
    
    /**
     * 菜品描述
     */
    private String description;
    
    /**
     * 价格
     */
    private BigDecimal price;
    
    /**
     * 菜品分类
     */
    private String category;
    
    /**
     * 菜品图片
     */
    private String imageUrl;
    
    /**
     * 是否可用
     */
    private Boolean isAvailable;
    
    /**
     * 所属餐厅ID
     */
    private Long restaurantId;
    
    /**
     * 所属餐厅名称
     */
    private String restaurantName;
    
    /**
     * 收藏备注
     */
    private String note;
    
    /**
     * 收藏时间
     */
    private LocalDateTime favoritedAt;
}
