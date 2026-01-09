package com.shydelivery.doordashsimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Basic Configuration
 * Temporarily disables authentication for easier development and testing
 * 
 * Can be extended later with:
 * - JWT authentication
 * - User login
 * - Authorization control
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (for development, should be enabled in production)
                .csrf(csrf -> csrf.disable())
                // Allow all requests (no authentication required for now)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

}
