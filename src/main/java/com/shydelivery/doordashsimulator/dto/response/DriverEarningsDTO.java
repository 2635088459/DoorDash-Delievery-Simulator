package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 配送员收益统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverEarningsDTO {
    
    /**
     * 今日收益
     */
    private BigDecimal todayEarnings;
    
    /**
     * 今日完成订单数
     */
    private Integer todayDeliveries;
    
    /**
     * 本周收益
     */
    private BigDecimal weekEarnings;
    
    /**
     * 本周完成订单数
     */
    private Integer weekDeliveries;
    
    /**
     * 总收益
     */
    private BigDecimal totalEarnings;
    
    /**
     * 可用余额
     */
    private BigDecimal availableBalance;
    
    /**
     * 总完成订单数
     */
    private Integer totalDeliveries;
    
    /**
     * 平均每单收益
     */
    private BigDecimal averageEarningsPerDelivery;
}
