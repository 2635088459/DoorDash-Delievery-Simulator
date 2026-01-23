package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.RegisterDriverRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateDriverLocationRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateDriverStatusRequest;
import com.shydelivery.doordashsimulator.dto.response.DriverDTO;
import com.shydelivery.doordashsimulator.dto.response.DriverEarningsDTO;
import com.shydelivery.doordashsimulator.entity.Driver;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.BusinessException;
import com.shydelivery.doordashsimulator.repository.DriverRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Driver Service
 * 配送员业务逻辑服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DriverService {
    
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    
    /**
     * 注册为配送员
     */
    @Transactional
    public DriverDTO registerDriver(RegisterDriverRequest request, String userEmail) {
        log.info("注册配送员: email={}, vehicleType={}", userEmail, request.getVehicleType());
        
        // 1. 查找用户
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 2. 检查是否已经是配送员
        if (driverRepository.existsByUser(user)) {
            throw new BusinessException("该用户已经是配送员");
        }
        
        // 3. 检查驾驶证是否已被使用
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessException("该驾驶证号已被注册");
        }
        
        // 4. 创建配送员
        Driver driver = Driver.builder()
            .user(user)
            .vehicleType(Driver.VehicleType.valueOf(request.getVehicleType()))
            .licenseNumber(request.getLicenseNumber())
            .vehiclePlate(request.getVehiclePlate())
            .isAvailable(false)  // 默认离线状态
            .rating(new BigDecimal("5.00"))
            .totalDeliveries(0)
            .build();
        
        Driver saved = driverRepository.save(driver);
        
        log.info("配送员注册成功: driverId={}, userId={}", saved.getId(), user.getId());
        
        return convertToDTO(saved);
    }
    
    /**
     * 获取配送员信息
     */
    public DriverDTO getDriverInfo(String userEmail) {
        Driver driver = findDriverByUserEmail(userEmail);
        return convertToDTO(driver);
    }
    
    /**
     * 更新配送员状态
     */
    @Transactional
    public DriverDTO updateDriverStatus(UpdateDriverStatusRequest request, String userEmail) {
        log.info("更新配送员状态: email={}, status={}", userEmail, request.getStatus());
        
        Driver driver = findDriverByUserEmail(userEmail);
        
        // 更新状态
        boolean goingOnline = "ONLINE".equals(request.getStatus()) && !driver.getIsAvailable();
        boolean goingOffline = "OFFLINE".equals(request.getStatus()) && driver.getIsAvailable();
        
        if (goingOnline) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new BusinessException("上线时必须提供位置信息");
            }
            driver.goOnline();
            driver.updateLocation(request.getLatitude(), request.getLongitude());
            log.info("配送员已上线: driverId={}", driver.getId());
        } else if (goingOffline) {
            driver.goOffline();
            log.info("配送员已下线: driverId={}", driver.getId());
        }
        
        // 更新位置（如果提供）
        if (request.getLatitude() != null && request.getLongitude() != null) {
            driver.updateLocation(request.getLatitude(), request.getLongitude());
        }
        
        Driver updated = driverRepository.save(driver);
        
        return convertToDTO(updated);
    }
    
    /**
     * 更新配送员位置
     */
    @Transactional
    public DriverDTO updateLocation(UpdateDriverLocationRequest request, String userEmail) {
        Driver driver = findDriverByUserEmail(userEmail);
        
        driver.updateLocation(request.getLatitude(), request.getLongitude());
        
        Driver updated = driverRepository.save(driver);
        
        log.debug("配送员位置已更新: driverId={}, lat={}, lon={}", 
            driver.getId(), request.getLatitude(), request.getLongitude());
        
        return convertToDTO(updated);
    }
    
    /**
     * 获取配送员收益统计
     */
    public DriverEarningsDTO getEarnings(String userEmail) {
        Driver driver = findDriverByUserEmail(userEmail);
        
        BigDecimal averageEarnings = BigDecimal.ZERO;
        if (driver.getTotalDeliveries() > 0) {
            averageEarnings = driver.getTotalEarnings()
                .divide(new BigDecimal(driver.getTotalDeliveries()), 2, RoundingMode.HALF_UP);
        }
        
        return DriverEarningsDTO.builder()
            .todayEarnings(BigDecimal.ZERO)  // TODO: 从配送记录计算
            .todayDeliveries(0)
            .weekEarnings(BigDecimal.ZERO)   // TODO: 从配送记录计算
            .weekDeliveries(0)
            .totalEarnings(driver.getTotalEarnings())
            .availableBalance(BigDecimal.ZERO)  // 使用 driver 实体中的字段
            .totalDeliveries(driver.getTotalDeliveries())
            .averageEarningsPerDelivery(averageEarnings)
            .build();
    }
    
    /**
     * 获取所有在线配送员
     */
    public List<DriverDTO> getOnlineDrivers() {
        List<Driver> drivers = driverRepository.findByIsAvailableTrue();
        return drivers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 查找附近的在线配送员
     */
    public List<DriverDTO> findNearbyDrivers(BigDecimal latitude, BigDecimal longitude, Double radiusKm) {
        // 获取所有在线且有位置的配送员
        List<Driver> availableDrivers = driverRepository.findAvailableDriversWithLocation();
        
        // 过滤出指定半径内的配送员
        return availableDrivers.stream()
            .filter(driver -> {
                double distance = calculateDistance(
                    latitude, longitude,
                    driver.getCurrentLatitude(), driver.getCurrentLongitude()
                );
                return distance <= radiusKm;
            })
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * 根据用户邮箱查找配送员
     */
    private Driver findDriverByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        return driverRepository.findByUser(user)
            .orElseThrow(() -> new BusinessException("该用户不是配送员"));
    }
    
    /**
     * 计算两点之间的距离（Haversine公式）
     * Phase 2: 改为 public，供 WebSocket 使用
     */
    public double calculateDistance(
        BigDecimal lat1, BigDecimal lon1,
        BigDecimal lat2, BigDecimal lon2) {
        
        final double EARTH_RADIUS_KM = 6371.0;
        
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) * 
                   Math.cos(Math.toRadians(lat2.doubleValue())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * 转换为 DTO
     */
    private DriverDTO convertToDTO(Driver driver) {
        User user = driver.getUser();
        
        return DriverDTO.builder()
            .id(driver.getId())
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .phoneNumber(user.getPhoneNumber())
            .vehicleType(driver.getVehicleType().name())
            .licenseNumber(driver.getLicenseNumber())
            .vehiclePlate(driver.getVehiclePlate())
            .status(driver.getIsAvailable() ? "ONLINE" : "OFFLINE")
            .currentLatitude(driver.getCurrentLatitude())
            .currentLongitude(driver.getCurrentLongitude())
            .rating(driver.getRating())
            .totalDeliveries(driver.getTotalDeliveries())
            .completedDeliveries(driver.getTotalDeliveries())  // TODO: 从实体获取
            .totalEarnings(BigDecimal.ZERO)  // TODO: 从实体获取
            .availableBalance(BigDecimal.ZERO)
            .isActive(true)
            .createdAt(driver.getCreatedAt())
            .updatedAt(driver.getUpdatedAt())
            .build();
    }
}
