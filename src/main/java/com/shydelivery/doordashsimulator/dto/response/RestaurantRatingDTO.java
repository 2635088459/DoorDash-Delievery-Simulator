package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * RestaurantRatingDTO - 餐厅评分统计 DTO
 * 
 * 返回餐厅的评分统计信息
 * 
 * @author DoorDash Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRatingDTO {
    
    /**
     * 餐厅ID
     */
    private Long restaurantId;
    
    /**
     * 餐厅名称
     */
    private String restaurantName;
    
    /**
     * 平均总体评分
     */
    private BigDecimal averageRating;
    
    /**
     * 平均食物评分
     */
    private Double averageFoodRating;
    
    /**
     * 平均配送评分
     */
    private Double averageDeliveryRating;
    
    /**
     * 评价总数
     */
    private Long totalReviews;
    
    /**
     * 正面评价数量 (>= 4.0)
     */
    private Long positiveReviews;
    
    /**
     * 负面评价数量 (< 3.0)
     */
    private Long negativeReviews;
}
