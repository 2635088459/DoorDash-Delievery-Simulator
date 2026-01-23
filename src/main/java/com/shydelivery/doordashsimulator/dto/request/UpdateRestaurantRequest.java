package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新餐厅请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestaurantRequest {
    
    @Size(max = 200, message = "餐厅名称不能超过200个字符")
    private String name;
    
    @Size(max = 1000, message = "描述不能超过1000个字符")
    private String description;
    
    @Size(max = 100, message = "菜系类型不能超过100个字符")
    private String cuisineType;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "电话号码格式不正确")
    private String phoneNumber;
    
    private Boolean isActive;
}
