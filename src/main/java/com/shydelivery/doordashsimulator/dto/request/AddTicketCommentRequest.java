package com.shydelivery.doordashsimulator.dto.request;

import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTicketCommentRequest {

    @NotBlank(message = "评论不能为空")
    private String content;

    @Builder.Default
    private CommentType type = CommentType.USER_NOTE;

    private String evidenceJson;
}
