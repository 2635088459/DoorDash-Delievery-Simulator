package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.User.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建用户请求 DTO
 * 用于用户注册
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;
    
    @NotBlank(message = "名字不能为空")
    @Size(min = 1, max = 50, message = "名字长度不能超过50个字符")
    private String firstName;
    
    @NotBlank(message = "姓氏不能为空")
    @Size(min = 1, max = 50, message = "姓氏长度不能超过50个字符")
    private String lastName;
    
    @NotBlank(message = "电话号码不能为空")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "电话号码格式不正确（E.164格式，如 +16263805884）")
    private String phoneNumber;
    
    @NotNull(message = "用户角色不能为空")
    private UserRole role;
}
