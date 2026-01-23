package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新配送员状态请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverStatusRequest {
    
    /**
     * 配送员状态
     */
    @NotBlank(message = "状态不能为空")
    private String status;  // ONLINE, OFFLINE, BUSY, INACTIVE
    
    /**
     * 当前纬度
     */
    @DecimalMin(value = "-90.0", message = "纬度必须在 -90 到 90 之间")
    @DecimalMax(value = "90.0", message = "纬度必须在 -90 到 90 之间")
    private BigDecimal latitude;
    
    /**
     * 当前经度
     */
    @DecimalMin(value = "-180.0", message = "经度必须在 -180 到 180 之间")
    @DecimalMax(value = "180.0", message = "经度必须在 -180 到 180 之间")
    private BigDecimal longitude;
}
