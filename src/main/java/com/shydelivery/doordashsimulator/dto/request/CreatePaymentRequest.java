package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建支付请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    /**
     * 支付方式
     */
    @NotNull(message = "支付方式不能为空")
    private PaymentMethod paymentMethod;
    
    /**
     * 卡号最后四位（信用卡/借记卡）
     */
    @Size(min = 4, max = 4, message = "卡号最后四位必须是4位数字")
    private String cardLastFour;
    
    /**
     * 支付提供商
     */
    @Size(max = 50, message = "支付提供商名称不能超过50个字符")
    private String paymentProvider;
    
    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    private String notes;
}
