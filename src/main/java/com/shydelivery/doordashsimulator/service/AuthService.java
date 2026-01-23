package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.UserDTO;
import com.shydelivery.doordashsimulator.dto.request.LoginRequest;
import com.shydelivery.doordashsimulator.dto.request.RegisterRequest;
import com.shydelivery.doordashsimulator.dto.response.AuthResponse;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.BusinessException;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import com.shydelivery.doordashsimulator.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务（本地版本 - 不依赖 AWS Cognito）
 */
@Service
@Slf4j
@Transactional
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    /**
     * 用户注册
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt: {}", request.getEmail());
        
        // 1. 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists: " + request.getEmail());
        }
        
        // 2. 检查手机号是否已存在
        if (userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new BusinessException("Phone number already exists: " + request.getPhone());
        }
        
        // 3. 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhone());
        user.setRole(request.getRole());
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());
        
        // 4. 生成 JWT tokens
        String accessToken = tokenProvider.generateToken(savedUser.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getEmail());
        
        // 5. 返回认证响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationTime())
                .user(convertToDTO(savedUser))
                .build();
    }
    
    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());
        
        // 1. 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getEmail());
                    return new BusinessException("Invalid email or password");
                });
        
        // 2. 检查密码
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Invalid credentials for user: {}", request.getEmail());
            throw new BusinessException("Invalid email or password");
        }
        
        // 3. 检查用户状态
        if (!user.getIsActive()) {
            log.warn("Inactive user attempted to login: {}", user.getEmail());
            throw new BusinessException("Account is disabled");
        }
        
        log.info("Login successful for user: {}", user.getEmail());
        
        // 4. 生成 JWT tokens
        String accessToken = tokenProvider.generateToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());
        
        // 5. 返回认证响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationTime())
                .user(convertToDTO(user))
                .build();
    }
    
    /**
     * 刷新Token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Token refresh request received");
        
        // 1. 验证 refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }
        
        // 2. 从 token 中提取用户邮箱
        String email = tokenProvider.getEmailFromToken(refreshToken);
        
        // 3. 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // 4. 检查用户状态
        if (!user.getIsActive()) {
            throw new BusinessException("Account is disabled");
        }
        
        // 5. 生成新的 tokens
        String newAccessToken = tokenProvider.generateToken(user.getEmail());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());
        
        log.info("Token refreshed successfully for user: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationTime())
                .user(convertToDTO(user))
                .build();
    }
    
    /**
     * 登出（本地版本 - 可选实现 token 黑名单）
     */
    public void logout(String accessToken) {
        log.info("Logout successful");
        // 本地版本：可以实现 token 黑名单机制
        // 这里简单实现，由客户端删除 token
    }
    
    /**
     * 根据邮箱获取用户
     */
    public UserDTO getUserByEmail(String email) {
        log.info("Get user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found: " + email));
        
        return convertToDTO(user);
    }
    
    /**
     * 转换 User 为 UserDTO
     */
    private UserDTO convertToDTO(User user) {
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
}
