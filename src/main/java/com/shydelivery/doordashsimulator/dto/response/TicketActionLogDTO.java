package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.TicketActionLog.ActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketActionLogDTO {

    private Long id;

    private Long ticketId;

    private String actionType;

    private ActionStatus status;

    private String operator;

    private String note;

    private String resultMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
