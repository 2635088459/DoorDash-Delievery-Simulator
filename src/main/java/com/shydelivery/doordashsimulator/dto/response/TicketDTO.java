package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.Ticket.TicketCategory;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketPriority;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketSource;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    private Long id;

    private String title;

    private String description;

    private TicketStatus status;

    private TicketPriority priority;

    private TicketCategory category;

    private TicketSource source;

    private Long restaurantId;

    private Long orderId;

    private Long driverId;

    private String assignedTo;

    private String assignedRole;

    private String createdBy;

    private String evidenceJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long slaHours;

    private LocalDateTime slaDeadline;

    private Boolean slaOverdue;

    private Long slaOverdueMinutes;

    private List<TicketCommentDTO> comments;
}
