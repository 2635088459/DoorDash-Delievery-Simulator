package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.Ticket.TicketCategory;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketPriority;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotNull(message = "类别不能为空")
    private TicketCategory category;

    @Builder.Default
    private TicketPriority priority = TicketPriority.NORMAL;

    @Builder.Default
    private TicketSource source = TicketSource.MANUAL;

    private Long restaurantId;

    private Long orderId;

    private Long driverId;

    private String assignedTo;

    private String assignedRole;

    private String evidenceJson;
}
