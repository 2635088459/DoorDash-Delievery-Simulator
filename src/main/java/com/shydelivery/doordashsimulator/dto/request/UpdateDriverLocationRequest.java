package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新配送员位置请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverLocationRequest {
    
    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在 -90 到 90 之间")
    @DecimalMax(value = "90.0", message = "纬度必须在 -90 到 90 之间")
    private BigDecimal latitude;
    
    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在 -180 到 180 之间")
    @DecimalMax(value = "180.0", message = "经度必须在 -180 到 180 之间")
    private BigDecimal longitude;
    
    /**
     * 速度（km/h）
     */
    private BigDecimal speed;
    
    /**
     * 方向角度（0-360）
     */
    @DecimalMin(value = "0.0", message = "方向角度必须在 0 到 360 之间")
    @DecimalMax(value = "360.0", message = "方向角度必须在 0 到 360 之间")
    private BigDecimal heading;
}
