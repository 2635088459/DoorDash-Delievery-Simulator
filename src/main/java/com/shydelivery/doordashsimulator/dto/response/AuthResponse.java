package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 DTO
 * 包含 JWT Tokens 和用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * ID Token - 包含用户身份信息（用于后续 API 请求）
     */
    private String idToken;
    
    /**
     * Access Token - 用于访问 AWS 资源
     */
    private String accessToken;
    
    /**
     * Refresh Token - 用于刷新其他 Token
     */
    private String refreshToken;
    
    /**
     * Token 过期时间（秒）
     */
    private Integer expiresIn;
    
    /**
     * Token 类型（固定为 "Bearer"）
     */
    private String tokenType;
    
    /**
     * 用户信息
     */
    private UserDTO user;
}
