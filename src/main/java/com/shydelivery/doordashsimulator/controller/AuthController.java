package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.UserDTO;
import com.shydelivery.doordashsimulator.dto.request.LoginRequest;
import com.shydelivery.doordashsimulator.dto.request.RegisterRequest;
import com.shydelivery.doordashsimulator.dto.response.AuthResponse;
import com.shydelivery.doordashsimulator.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * 提供的接口：
 * 1. POST /api/auth/register - 用户注册
 * 2. POST /api/auth/login - 用户登录
 * 3. POST /api/auth/refresh - 刷新 Token
 * 4. POST /api/auth/logout - 用户登出
 * 5. GET /api/auth/me - 获取当前用户信息
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 认证响应（包含 Tokens 和用户信息）
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());
        
        AuthResponse response = authService.register(request);
        
        log.info("Register successful for user: {}", response.getUser().getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户登录
     * 
     * @param request 登录请求（邮箱 + 密码）
     * @return 认证响应（包含 Tokens 和用户信息）
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        
        AuthResponse response = authService.login(request);
        
        log.info("Login successful for user: {}", response.getUser().getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 刷新 Token
     * 
     * @param refreshToken Refresh Token（从请求参数获取）
     * @return 新的认证响应
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("Token refresh request received");
        
        AuthResponse response = authService.refreshToken(refreshToken);
        
        log.info("Token refreshed successfully for user: {}", response.getUser().getEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户登出
     * 
     * @param authHeader Authorization Header（格式：Bearer {accessToken}）
     * @return 无返回内容
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("Logout request received");
        
        // 提取 Access Token（去掉 "Bearer " 前缀）
        String accessToken = authHeader.substring(7);
        
        authService.logout(accessToken);
        
        log.info("Logout successful");
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * 从 SecurityContext 中获取当前认证的用户邮箱
     * 然后从数据库查询完整的用户信息
     * 
     * @return 当前用户 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.info("Get current user request received");
        
        // 从 Spring Security 的 SecurityContext 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User not authenticated");
            return ResponseEntity.status(401).build();
        }
        
        // Principal 是用户邮箱（在 JwtAuthenticationFilter 中设置的）
        String email = (String) authentication.getPrincipal();
        log.info("Current user email: {}", email);
        
        // 从数据库查询用户信息
        UserDTO userDTO = authService.getUserByEmail(email);
        
        return ResponseEntity.ok(userDTO);
    }
    
    /**
     * 测试接口 - 验证 JWT Token 是否有效
     * 
     * @return 简单的成功消息
     */
    @GetMapping("/test")
    public ResponseEntity<String> testAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            String email = (String) authentication.getPrincipal();
            String authorities = authentication.getAuthorities().toString();
            
            return ResponseEntity.ok(
                    "Authentication successful! User: " + email + ", Authorities: " + authorities
            );
        }
        
        return ResponseEntity.status(401).body("Not authenticated");
    }
}
