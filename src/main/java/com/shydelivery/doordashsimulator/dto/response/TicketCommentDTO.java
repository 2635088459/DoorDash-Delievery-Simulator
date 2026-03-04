package com.shydelivery.doordashsimulator.dto.response;

import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCommentDTO {

    private Long id;

    private Long ticketId;

    private String author;

    private String authorRole;

    private CommentType type;

    private String content;

    private String evidenceJson;

    private LocalDateTime createdAt;
}
