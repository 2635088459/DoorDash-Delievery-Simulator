package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Ticket entity - anomaly/operations work orders
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_category", columnList = "category"),
    @Index(name = "idx_ticket_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_ticket_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketCategory category = TicketCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketSource source = TicketSource.SYSTEM;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "assigned_to", length = 200)
    private String assignedTo;

    @Column(name = "assigned_role", length = 50)
    private String assignedRole;

    @Column(name = "created_by", length = 200)
    private String createdBy;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TicketStatus {
        NEW,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum TicketPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum TicketCategory {
        RESTAURANT_CANCEL_SPIKE,
        DELIVERY_DELAY_SPIKE,
        DELIVERY_TIMEOUT_SPIKE,
        DRIVER_ISSUE,
        PAYMENT_ISSUE,
        PAYMENT_REFUND_SPIKE,
        OTHER
    }

    public enum TicketSource {
        SYSTEM,
        MANUAL
    }
}
