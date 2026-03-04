package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSampleOrderDTO {

    private Long orderId;

    private String orderNumber;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private BigDecimal deliveryFee;

    private BigDecimal tipAmount;

    private String customerName;

    private LocalDateTime createdAt;

    private LocalDateTime actualDelivery;
}
