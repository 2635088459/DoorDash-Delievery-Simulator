package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating order status
 * 
 * RBAC: 
 * - RESTAURANT_OWNER can update status: CONFIRMED, PREPARING, READY_FOR_PICKUP
 * - DRIVER can update status: PICKED_UP, IN_TRANSIT, DELIVERED
 * - CUSTOMER can update status: CANCELLED (only if status is PENDING)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    
    /**
     * New status for the order
     */
    @NotNull(message = "订单状态不能为空")
    private OrderStatus status;
    
    /**
     * Optional notes about the status change
     */
    private String notes;
}
