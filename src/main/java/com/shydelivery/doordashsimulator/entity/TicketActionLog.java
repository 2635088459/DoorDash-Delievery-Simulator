package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_action_logs", indexes = {
    @Index(name = "idx_ticket_action_ticket", columnList = "ticket_id"),
    @Index(name = "idx_ticket_action_status", columnList = "status"),
    @Index(name = "idx_ticket_action_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionStatus status = ActionStatus.PENDING;

    @Column(name = "operator", length = 200)
    private String operator;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum ActionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}
