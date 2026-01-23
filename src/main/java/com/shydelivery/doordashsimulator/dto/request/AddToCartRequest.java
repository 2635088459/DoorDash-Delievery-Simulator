package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加商品到购物车请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    
    /**
     * 菜品ID
     */
    @NotNull(message = "菜品ID不能为空")
    private Long menuItemId;
    
    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须至少为1")
    private Integer quantity;
    
    /**
     * 特殊要求（可选）
     */
    private String specialInstructions;
}
