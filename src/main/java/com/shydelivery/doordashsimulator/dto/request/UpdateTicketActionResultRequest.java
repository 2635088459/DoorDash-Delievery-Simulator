package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.TicketActionLog.ActionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketActionResultRequest {

    @NotNull(message = "结果状态不能为空")
    private ActionStatus status;

    private String resultMessage;
}
