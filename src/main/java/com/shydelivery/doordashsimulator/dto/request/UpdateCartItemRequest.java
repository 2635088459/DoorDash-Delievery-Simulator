package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新购物车商品请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    
    /**
     * 新的数量
     */
    @Min(value = 1, message = "数量必须至少为1")
    private Integer quantity;
    
    /**
     * 特殊要求（可选）
     */
    private String specialInstructions;
}
