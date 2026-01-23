package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ReviewDTO - 评价响应 DTO
 * 
 * 返回评价信息给客户端
 * 
 * 包含内容:
 * - 评价基本信息
 * - 食物和配送评分
 * - 总体评分
 * - 客户和餐厅信息
 * - 时间戳
 * 
 * @author DoorDash Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    
    /**
     * 评价ID
     */
    private Long id;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 客户ID
     */
    private Long customerId;
    
    /**
     * 客户姓名
     */
    private String customerName;
    
    /**
     * 餐厅ID
     */
    private Long restaurantId;
    
    /**
     * 餐厅名称
     */
    private String restaurantName;
    
    /**
     * 食物评分 (1-5星)
     */
    private Integer foodRating;
    
    /**
     * 配送评分 (1-5星)
     */
    private Integer deliveryRating;
    
    /**
     * 总体评分 (计算得出)
     */
    private BigDecimal overallRating;
    
    /**
     * 评论内容
     */
    private String comment;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否为正面评价 (>= 4.0)
     */
    private Boolean isPositive;
    
    /**
     * 是否为负面评价 (< 3.0)
     */
    private Boolean isNegative;
}
