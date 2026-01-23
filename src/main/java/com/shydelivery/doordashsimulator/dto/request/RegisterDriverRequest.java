package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配送员注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDriverRequest {
    
    /**
     * 车辆类型
     */
    @NotBlank(message = "车辆类型不能为空")
    private String vehicleType;  // CAR, MOTORCYCLE, BICYCLE, SCOOTER, WALKING
    
    /**
     * 驾驶证号码
     */
    @NotBlank(message = "驾驶证号码不能为空")
    private String licenseNumber;
    
    /**
     * 车辆型号
     */
    private String vehicleModel;
    
    /**
     * 车牌号
     */
    @NotBlank(message = "车牌号不能为空")
    private String vehiclePlate;
    
    /**
     * 身份证号码
     */
    private String idCardNumber;
    
    /**
     * 配送员简介
     */
    private String bio;
}
