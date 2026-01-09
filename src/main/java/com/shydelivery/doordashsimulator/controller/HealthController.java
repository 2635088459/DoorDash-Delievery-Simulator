package com.shydelivery.doordashsimulator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Used to test if the application is running properly
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * Health check endpoint
     * Access: http://localhost:8080/api/health
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "DoorDash Simulator");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Application is running successfully!");
        return response;
    }

    /**
     * Welcome page
     * Access: http://localhost:8080/api/health/welcome
     */
    @GetMapping("/welcome")
    public Map<String, String> welcome() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to DoorDash Simulator API");
        response.put("version", "1.0.0");
        response.put("swagger", "Visit /api/swagger-ui.html for API documentation");
        return response;
    }

}
