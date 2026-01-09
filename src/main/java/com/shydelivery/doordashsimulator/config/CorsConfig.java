package com.shydelivery.doordashsimulator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS Configuration
 * Allows frontend from different domains to access the API
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Allowed origins (development environment)
                .allowedOrigins(
                        "http://localhost:3000",  // React default port
                        "http://localhost:4200",  // Angular default port
                        "http://localhost:8081"   // Other frontend ports
                )
                // Allowed HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // Allowed request headers
                .allowedHeaders("*")
                // Allow credentials (cookies, etc.)
                .allowCredentials(true)
                // Preflight request cache time (seconds)
                .maxAge(3600);
    }

}
