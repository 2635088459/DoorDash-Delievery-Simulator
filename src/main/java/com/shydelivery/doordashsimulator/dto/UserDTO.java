package com.shydelivery.doordashsimulator.dto;

import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.entity.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应 DTO
 * 返回给前端的用户信息（不包含密码等敏感信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 从 User Entity 转换为 UserDTO
     * 
     * @param user User 实体对象
     * @return UserDTO
     */
    public static UserDTO from(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    /**
     * 获取用户全名
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
