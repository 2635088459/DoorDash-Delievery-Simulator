package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateReviewRequest - 更新评价请求 DTO
 * 
 * 客户修改已有评价时使用
 * 
 * 验证规则:
 * - 食物评分和配送评分如果提供，必须在 1-5 之间
 * - 评论可选
 * 
 * @author DoorDash Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReviewRequest {
    
    /**
     * 食物评分（1-5星，可选）
     */
    @Min(value = 1, message = "食物评分最低为1星")
    @Max(value = 5, message = "食物评分最高为5星")
    private Integer foodRating;
    
    /**
     * 配送评分（1-5星，可选）
     */
    @Min(value = 1, message = "配送评分最低为1星")
    @Max(value = 5, message = "配送评分最高为5星")
    private Integer deliveryRating;
    
    /**
     * 评论内容（可选，最多1000字符）
     */
    @Size(max = 1000, message = "评论内容不能超过1000字符")
    private String comment;
}
