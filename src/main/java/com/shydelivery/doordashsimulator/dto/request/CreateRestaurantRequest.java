package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建餐厅请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantRequest {
    
    @NotBlank(message = "餐厅名称不能为空")
    @Size(max = 200, message = "餐厅名称不能超过200个字符")
    private String name;
    
    @Size(max = 1000, message = "描述不能超过1000个字符")
    private String description;
    
    @NotBlank(message = "菜系类型不能为空")
    @Size(max = 100, message = "菜系类型不能超过100个字符")
    private String cuisineType;
    
    @NotBlank(message = "街道地址不能为空")
    @Size(max = 255, message = "街道地址不能超过255个字符")
    private String streetAddress;
    
    @NotBlank(message = "城市不能为空")
    @Size(max = 100, message = "城市不能超过100个字符")
    private String city;
    
    @NotBlank(message = "州/省不能为空")
    @Size(max = 50, message = "州/省不能超过50个字符")
    private String state;
    
    @NotBlank(message = "邮编不能为空")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "邮编格式不正确")
    private String zipCode;
    
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "电话号码格式不正确")
    private String phoneNumber;
}
