package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTicketActionRequest {

    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    private String note;

    private boolean markResolved;
}
