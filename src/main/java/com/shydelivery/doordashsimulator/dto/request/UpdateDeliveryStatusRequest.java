package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新配送状态请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryStatusRequest {
    
    /**
     * 配送状态
     */
    @NotBlank(message = "状态不能为空")
    private String status;  // ACCEPTED, PICKED_UP, IN_TRANSIT, DELIVERED
    
    /**
     * 备注信息
     */
    private String notes;
    
    /**
     * 送达证明URL（完成配送时）
     */
    private String deliveryProofUrl;
}
