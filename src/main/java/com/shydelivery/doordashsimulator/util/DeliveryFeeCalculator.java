package com.shydelivery.doordashsimulator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 配送费计算器
 * 
 * 计算规则:
 * 1. 基础费用: $3.00
 * 2. 距离费用: $1.50/km
 * 3. 高峰期加价: 1.5倍（11:00-13:00, 17:00-20:00）
 * 4. 最低配送费: $2.00
 * 5. 恶劣天气加价: 1.2倍（可选）
 * 
 * Phase 2 新增: 动态定价算法
 */
@Slf4j
@Component
public class DeliveryFeeCalculator {
    
    private static final BigDecimal BASE_FEE = new BigDecimal("3.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("1.50");
    private static final BigDecimal MINIMUM_FEE = new BigDecimal("2.00");
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal WEATHER_MULTIPLIER = new BigDecimal("1.2");
    
    /**
     * 计算配送费（标准版）
     * 
     * @param distanceKm 配送距离（公里）
     * @param orderTime 下单时间
     * @return 配送费
     */
    public BigDecimal calculateDeliveryFee(double distanceKm, LocalDateTime orderTime) {
        return calculateDeliveryFee(distanceKm, orderTime, false);
    }
    
    /**
     * 计算配送费（完整版）
     * 
     * @param distanceKm 配送距离（公里）
     * @param orderTime 下单时间
     * @param badWeather 是否恶劣天气
     * @return 配送费
     */
    public BigDecimal calculateDeliveryFee(
            double distanceKm, 
            LocalDateTime orderTime, 
            boolean badWeather) {
        
        log.debug("计算配送费: distance={}km, time={}, badWeather={}", 
                distanceKm, orderTime, badWeather);
        
        BigDecimal fee = BASE_FEE;
        
        // 1. 距离费用
        BigDecimal distanceFee = PER_KM_RATE.multiply(new BigDecimal(distanceKm));
        fee = fee.add(distanceFee);
        
        log.debug("基础费用: ${}, 距离费用: ${}, 小计: ${}", 
                BASE_FEE, distanceFee, fee);
        
        // 2. 高峰期加价
        if (isPeakHour(orderTime)) {
            BigDecimal originalFee = fee;
            fee = fee.multiply(PEAK_MULTIPLIER);
            log.info("高峰期加价: ${} → ${} ({}x)", 
                    originalFee, fee, PEAK_MULTIPLIER);
        }
        
        // 3. 恶劣天气加价
        if (badWeather) {
            BigDecimal originalFee = fee;
            fee = fee.multiply(WEATHER_MULTIPLIER);
            log.info("恶劣天气加价: ${} → ${} ({}x)", 
                    originalFee, fee, WEATHER_MULTIPLIER);
        }
        
        // 4. 最低配送费
        if (fee.compareTo(MINIMUM_FEE) < 0) {
            log.debug("应用最低配送费: ${} → ${}", fee, MINIMUM_FEE);
            fee = MINIMUM_FEE;
        }
        
        BigDecimal finalFee = fee.setScale(2, RoundingMode.HALF_UP);
        log.info("最终配送费: ${}", finalFee);
        
        return finalFee;
    }
    
    /**
     * 判断是否高峰期
     * 高峰期定义:
     * - 午餐时间: 11:00 - 13:00
     * - 晚餐时间: 17:00 - 20:00
     * 
     * Phase 2: 改为 public，供 OrderService 使用
     */
    public boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        
        // 午餐高峰期
        boolean isLunchPeak = hour >= 11 && hour < 13;
        
        // 晚餐高峰期
        boolean isDinnerPeak = hour >= 17 && hour < 20;
        
        return isLunchPeak || isDinnerPeak;
    }
    
    /**
     * 计算配送员收益
     * 配送员收益 = 配送费 × 80%（平台抽成20%）
     * 
     * @param deliveryFee 配送费
     * @return 配送员收益
     */
    public BigDecimal calculateDriverEarnings(BigDecimal deliveryFee) {
        BigDecimal driverPercentage = new BigDecimal("0.80");
        BigDecimal earnings = deliveryFee.multiply(driverPercentage);
        
        log.debug("配送员收益: ${} × 80% = ${}", deliveryFee, earnings);
        
        return earnings.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算平台抽成
     * 
     * @param deliveryFee 配送费
     * @return 平台抽成
     */
    public BigDecimal calculatePlatformFee(BigDecimal deliveryFee) {
        BigDecimal platformPercentage = new BigDecimal("0.20");
        BigDecimal platformFee = deliveryFee.multiply(platformPercentage);
        
        return platformFee.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 估算配送时间（分钟）
     * 
     * @param distanceKm 配送距离（公里）
     * @return 预计配送时间（分钟）
     */
    public int estimateDeliveryTime(double distanceKm) {
        // 假设配送员平均速度: 20 km/h
        double averageSpeedKmPerHour = 20.0;
        
        // 计算行驶时间（小时）
        double travelTimeHours = distanceKm / averageSpeedKmPerHour;
        
        // 转换为分钟，并加上准备时间（10分钟）
        int preparationMinutes = 10;
        int travelMinutes = (int) Math.ceil(travelTimeHours * 60);
        
        int totalMinutes = preparationMinutes + travelMinutes;
        
        log.debug("预计配送时间: {}km / {}km/h = {}分钟 + {}分钟准备 = {}分钟", 
                distanceKm, averageSpeedKmPerHour, travelMinutes, preparationMinutes, totalMinutes);
        
        return totalMinutes;
    }
    
    /**
     * 计算预计送达时间
     * 
     * @param orderTime 下单时间
     * @param distanceKm 配送距离（公里）
     * @return 预计送达时间
     */
    public LocalDateTime estimateDeliveryTime(LocalDateTime orderTime, double distanceKm) {
        int estimatedMinutes = estimateDeliveryTime(distanceKm);
        return orderTime.plusMinutes(estimatedMinutes);
    }
}
