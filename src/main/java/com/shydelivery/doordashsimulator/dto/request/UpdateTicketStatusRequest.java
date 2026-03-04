package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {

    @NotNull(message = "状态不能为空")
    private TicketStatus status;

    private String assignedTo;

    private String assignedRole;
}
