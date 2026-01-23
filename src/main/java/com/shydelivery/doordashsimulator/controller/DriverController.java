package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.RegisterDriverRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateDriverLocationRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateDriverStatusRequest;
import com.shydelivery.doordashsimulator.dto.response.DriverDTO;
import com.shydelivery.doordashsimulator.dto.response.DriverEarningsDTO;
import com.shydelivery.doordashsimulator.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Driver Controller
 * 配送员管理 REST API
 * 
 * RBAC 权限:
 * - POST /drivers/register      - CUSTOMER (任何用户可以注册成为配送员)
 * - GET  /drivers/me            - DRIVER
 * - PUT  /drivers/status        - DRIVER
 * - PUT  /drivers/location      - DRIVER
 * - GET  /drivers/earnings      - DRIVER
 * - GET  /drivers/online        - ADMIN
 */
@Slf4j
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {
    
    private final DriverService driverService;
    
    /**
     * 注册为配送员
     * POST /api/drivers/register
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DriverDTO> registerDriver(
            @Valid @RequestBody RegisterDriverRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        log.info("API - 配送员注册: email={}", email);
        
        DriverDTO driver = driverService.registerDriver(request, email);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(driver);
    }
    
    /**
     * 获取配送员信息
     * GET /api/drivers/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")  // 任何用户都可以查看自己的配送员信息
    public ResponseEntity<DriverDTO> getMyInfo(Authentication authentication) {
        
        String email = authentication.getName();
        DriverDTO driver = driverService.getDriverInfo(email);
        
        return ResponseEntity.ok(driver);
    }
    
    /**
     * 更新配送员状态
     * PUT /api/drivers/status
     */
    @PutMapping("/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DriverDTO> updateStatus(
            @Valid @RequestBody UpdateDriverStatusRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        log.info("API - 更新配送员状态: email={}, status={}", email, request.getStatus());
        
        DriverDTO driver = driverService.updateDriverStatus(request, email);
        
        return ResponseEntity.ok(driver);
    }
    
    /**
     * 更新配送员位置
     * PUT /api/drivers/location
     */
    @PutMapping("/location")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DriverDTO> updateLocation(
            @Valid @RequestBody UpdateDriverLocationRequest request,
            Authentication authentication) {
        
        String email = authentication.getName();
        DriverDTO driver = driverService.updateLocation(request, email);
        
        return ResponseEntity.ok(driver);
    }
    
    /**
     * 获取配送员收益统计
     * GET /api/drivers/earnings
     */
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DriverEarningsDTO> getEarnings(Authentication authentication) {
        
        String email = authentication.getName();
        DriverEarningsDTO earnings = driverService.getEarnings(email);
        
        return ResponseEntity.ok(earnings);
    }
    
    /**
     * 获取所有在线配送员（管理员）
     * GET /api/drivers/online
     */
    @GetMapping("/online")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DriverDTO>> getOnlineDrivers() {
        
        List<DriverDTO> drivers = driverService.getOnlineDrivers();
        
        return ResponseEntity.ok(drivers);
    }
    
    /**
     * 查找附近的配送员
     * GET /api/drivers/nearby
     */
    @GetMapping("/nearby")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DriverDTO>> findNearbyDrivers(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        
        log.info("API - 查找附近配送员: lat={}, lon={}, radius={}km", latitude, longitude, radiusKm);
        
        List<DriverDTO> drivers = driverService.findNearbyDrivers(latitude, longitude, radiusKm);
        
        return ResponseEntity.ok(drivers);
    }
}
