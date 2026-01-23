package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户请求 DTO
 * 只允许更新部分字段（不包括邮箱、密码、角色）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    @Size(min = 1, max = 50, message = "名字长度不能超过50个字符")
    private String firstName;
    
    @Size(min = 1, max = 50, message = "姓氏长度不能超过50个字符")
    private String lastName;
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "电话号码格式不正确（10-15位数字）")
    private String phoneNumber;
}
