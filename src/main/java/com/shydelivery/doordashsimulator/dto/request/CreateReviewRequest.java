package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateReviewRequest - 创建评价请求 DTO
 * 
 * 客户提交评价时使用
 * 
 * 验证规则:
 * - 订单ID必须提供
 * - 食物评分和配送评分必须在 1-5 之间
 * - 评论可选，但如果提供则长度限制
 * 
 * @author DoorDash Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {
    
    /**
     * 订单ID（必填）
     */
    @NotNull(message = "订单ID不能为空")
    @Positive(message = "订单ID必须为正数")
    private Long orderId;
    
    /**
     * 食物评分（1-5星，必填）
     */
    @NotNull(message = "食物评分不能为空")
    @Min(value = 1, message = "食物评分最低为1星")
    @Max(value = 5, message = "食物评分最高为5星")
    private Integer foodRating;
    
    /**
     * 配送评分（1-5星，必填）
     */
    @NotNull(message = "配送评分不能为空")
    @Min(value = 1, message = "配送评分最低为1星")
    @Max(value = 5, message = "配送评分最高为5星")
    private Integer deliveryRating;
    
    /**
     * 评论内容（可选，最多1000字符）
     */
    @Size(max = 1000, message = "评论内容不能超过1000字符")
    private String comment;
}
