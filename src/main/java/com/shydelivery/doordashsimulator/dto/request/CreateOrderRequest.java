package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.Order.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new order
 * 
 * RBAC: Only CUSTOMER role can create orders
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    /**
     * ID of the restaurant to order from
     */
    @NotNull(message = "餐厅 ID 不能为空")
    private Long restaurantId;
    
    /**
     * ID of the delivery address (optional, can use inline address instead)
     */
    private Long deliveryAddressId;
    
    /**
     * Inline delivery address (used if deliveryAddressId is not provided)
     */
    @Valid
    private DeliveryAddressRequest deliveryAddress;
    
    /**
     * List of items in the order
     */
    @NotEmpty(message = "订单项不能为空")
    @Valid
    private List<OrderItemRequest> items;
    
    /**
     * Payment method
     */
    @NotNull(message = "支付方式不能为空")
    private PaymentMethod paymentMethod;
    
    /**
     * Special instructions for delivery (optional)
     */
    private String specialInstructions;
    
    /**
     * Nested class for delivery address
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddressRequest {
        
        /**
         * Street address
         */
        @NotNull(message = "街道地址不能为空")
        private String streetAddress;
        
        /**
         * City
         */
        @NotNull(message = "城市不能为空")
        private String city;
        
        /**
         * State/Province
         */
        @NotNull(message = "州/省不能为空")
        private String state;
        
        /**
         * Zip/Postal code
         */
        @NotNull(message = "邮政编码不能为空")
        private String zipCode;
        
        /**
         * Delivery instructions (optional)
         */
        private String deliveryInstructions;
    }
    
    /**
     * Nested class for order items
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        
        /**
         * Menu item ID
         */
        @NotNull(message = "菜品 ID 不能为空")
        private Long menuItemId;
        
        /**
         * Quantity to order
         */
        @NotNull(message = "数量不能为空")
        private Integer quantity;
        
        /**
         * Special instructions for this item (optional)
         */
        private String specialInstructions;
    }
}
