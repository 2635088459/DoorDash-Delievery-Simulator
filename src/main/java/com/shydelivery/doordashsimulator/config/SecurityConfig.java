package com.shydelivery.doordashsimulator.config;

import com.shydelivery.doordashsimulator.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置
 * 
 * 配置内容：
 * 1. JWT 认证过滤器
 * 2. 公开接口（不需要认证）
 * 3. CORS 跨域配置
 * 4. Session 管理（无状态）
 * 5. 密码加密器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize 等方法级别的安全注解
@Slf4j
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 配置 Security Filter Chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security with JWT authentication...");
        
        http
                // 禁用 CSRF (因为使用 JWT，不需要 CSRF 保护)
                .csrf(AbstractHttpConfigurer::disable)
                
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开接口（不需要认证）
                        .requestMatchers("/auth/**").permitAll()              // 认证相关接口
                        .requestMatchers("/users").permitAll()                // 用户注册接口
                        .requestMatchers("/error").permitAll()                // 错误页面
                        .requestMatchers("/actuator/health").permitAll()      // 健康检查
                        .requestMatchers("/swagger-ui/**").permitAll()        // Swagger UI
                        .requestMatchers("/api-docs/**").permitAll()          // API 文档
                        .requestMatchers("/v3/api-docs/**").permitAll()       // OpenAPI 文档
                        
                        // WebSocket 端点（公开访问）
                        .requestMatchers("/ws/**").permitAll()                // WebSocket 连接
                        
                        // 餐厅公开接口（浏览和查看）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/restaurants", "/restaurants/**").permitAll()
                        
                        // 菜单项公开接口（浏览和查看）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/menu-items/**").permitAll()
                        
                        // 评价公开接口（查看评价和评分统计）
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/reviews/**").permitAll()
                        
                        // 搜索公开接口（所有搜索功能）
                        .requestMatchers("/search/**").permitAll()
                        
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                
                // 配置 Session 管理（无状态）
                // 因为使用 JWT，不需要服务器端 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 添加 JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前）
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        
        log.info("Spring Security configured successfully with JWT");
        return http.build();
    }
    
    /**
     * CORS 跨域配置
     * 允许前端应用跨域访问 API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的来源（开发环境允许所有，生产环境应该限制）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 允许的 Headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带凭证（Cookies）
        configuration.setAllowCredentials(true);
        
        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);
        
        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 密码加密器
     * 使用 BCrypt 算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
