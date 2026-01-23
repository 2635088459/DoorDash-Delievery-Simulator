package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDTO {
    
    private Long id;
    
    // 用户信息
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    
    // 车辆信息
    private String vehicleType;
    private String licenseNumber;
    private String vehicleModel;
    private String vehiclePlate;
    
    // 状态信息
    private String status;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private LocalDateTime lastLocationUpdate;
    
    // 统计信息
    private BigDecimal rating;
    private Integer totalDeliveries;
    private Integer completedDeliveries;
    private Integer cancelledDeliveries;
    private Integer totalOnlineMinutes;
    private Double completionRate;
    
    // 收益信息
    private BigDecimal totalEarnings;
    private BigDecimal availableBalance;
    private BigDecimal todayEarnings;
    private BigDecimal weekEarnings;
    
    // 认证信息
    private Boolean isVerified;
    private LocalDateTime verificationDate;
    private String backgroundCheckStatus;
    
    // 其他信息
    private String bio;
    private Boolean isActive;
    
    // 时间戳
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
