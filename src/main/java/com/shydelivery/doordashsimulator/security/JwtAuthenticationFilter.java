package com.shydelivery.doordashsimulator.security;

import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器（本地版本）
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        try {
            // 1. 从请求中提取 JWT Token
            String token = extractTokenFromRequest(request);
            
            if (token != null && !token.isEmpty()) {
                log.debug("Found JWT token in request");
                
                // 2. 验证 Token
                if (tokenProvider.validateToken(token)) {
                    // 3. 提取用户邮箱
                    String email = tokenProvider.getEmailFromToken(token);
                    
                    log.debug("Token validated for user: {}", email);
                    
                    // 4. 从数据库获取用户信息
                    User user = userRepository.findByEmail(email).orElse(null);
                    
                    List<GrantedAuthority> authorities;
                    if (user != null && user.getRole() != null) {
                        // 使用数据库中的角色
                        authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                        );
                        log.debug("User {} has role: {} from database", email, user.getRole());
                    } else {
                        // 默认权限
                        authorities = Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_USER")
                        );
                        log.warn("User not found for email: {}, using default ROLE_USER", email);
                    }
                    
                    // 5. 创建 Authentication 对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,      // Principal (用户标识)
                                    null,       // Credentials (凭证，不需要)
                                    authorities // Authorities (权限列表)
                            );
                    
                    // 6. 设置请求详情
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 7. 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("SecurityContext updated for user: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("Failed to authenticate user: {}", e.getMessage(), e);
            // 清空 SecurityContext
            SecurityContextHolder.clearContext();
        }
        
        // 继续过滤链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求中提取 Bearer Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * 判断是否应该跳过某些请求（例如公开接口）
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 这些路径不需要 JWT 验证
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/users") ||
               path.equals("/api/error") ||
               path.startsWith("/api/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/api/ws");
    }
}
