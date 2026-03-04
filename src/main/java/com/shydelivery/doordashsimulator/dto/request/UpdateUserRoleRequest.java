package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.User.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "角色不能为空")
    private UserRole role;
}
