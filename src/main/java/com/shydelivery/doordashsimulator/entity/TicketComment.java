package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TicketComment entity - comments and agent notes
 */
@Entity
@Table(name = "ticket_comments", indexes = {
    @Index(name = "idx_ticket_comment_ticket", columnList = "ticket_id"),
    @Index(name = "idx_ticket_comment_type", columnList = "comment_type"),
    @Index(name = "idx_ticket_comment_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(name = "author_role", length = 50)
    private String authorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false, length = 30)
    private CommentType type = CommentType.USER_NOTE;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum CommentType {
        AGENT_NOTE,
        USER_NOTE,
        SYSTEM_NOTE
    }
}
