package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏餐厅响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRestaurantDTO {
    
    /**
     * 收藏记录ID
     */
    private Long favoriteId;
    
    /**
     * 餐厅ID
     */
    private Long restaurantId;
    
    /**
     * 餐厅名称
     */
    private String restaurantName;
    
    /**
     * 餐厅描述
     */
    private String description;
    
    /**
     * 餐厅地址
     */
    private String address;
    
    /**
     * 菜系类型
     */
    private String cuisineType;
    
    /**
     * 配送费
     */
    private BigDecimal deliveryFee;
    
    /**
     * 最低起送金额
     */
    private BigDecimal minimumOrder;
    
    /**
     * 餐厅图片
     */
    private String imageUrl;
    
    /**
     * 餐厅评分
     */
    private Double averageRating;
    
    /**
     * 评论数量
     */
    private Integer reviewCount;
    
    /**
     * 是否营业
     */
    private Boolean isActive;
    
    /**
     * 收藏备注
     */
    private String note;
    
    /**
     * 收藏时间
     */
    private LocalDateTime favoritedAt;
}
