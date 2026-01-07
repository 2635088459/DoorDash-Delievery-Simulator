package com.shydelivery.doordashsimulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置
 * 允许前端从不同域名访问API
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许的源（开发环境）
                .allowedOrigins(
                        "http://localhost:3000",  // React 默认端口
                        "http://localhost:4200",  // Angular 默认端口
                        "http://localhost:8081"   // 其他前端端口
                )
                // 允许的HTTP方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许携带认证信息（cookies等）
                .allowCredentials(true)
                // 预检请求的缓存时间（秒）
                .maxAge(3600);
    }

}
