package com.shydelivery.doordashsimulator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新评价回复请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不能超过1000个字符")
    private String replyContent;
}
