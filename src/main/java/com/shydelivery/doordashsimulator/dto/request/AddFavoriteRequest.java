package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加收藏请求
 * 用于收藏餐厅或菜品时的可选备注
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteRequest {
    
    /**
     * 收藏备注（可选）
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
