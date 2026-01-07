package com.shydelivery.doordashsimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 基础配置
 * 暂时禁用认证，便于开发测试
 * 
 * 后续可以添加：
 * - JWT 认证
 * - 用户登录
 * - 权限控制
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（开发阶段，生产环境需要启用）
                .csrf(csrf -> csrf.disable())
                // 允许所有请求（暂时不需要认证）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

}
