package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * DeliveryInfo entity - Stores delivery tracking information for orders
 * Each order has one delivery info record
 */
@Entity
@Table(name = "delivery_info", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_driver_id", columnList = "driver_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated order - One delivery info per order
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /**
     * Assigned driver - Many delivery info records belong to one driver
     * Nullable because driver may not be assigned immediately
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    /**
     * When the driver picked up the order from restaurant
     */
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    /**
     * When the order was delivered to customer
     */
    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    /**
     * Current delivery status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    /**
     * Delivery notes and updates (e.g., "Driver at restaurant", "On the way")
     */
    @Column(name = "tracking_notes", columnDefinition = "TEXT")
    private String trackingNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Delivery status enumeration
     */
    public enum DeliveryStatus {
        PENDING,         // Waiting for driver assignment
        ASSIGNED,        // Driver assigned to order
        PICKED_UP,       // Driver picked up order from restaurant
        IN_TRANSIT,      // Driver is delivering the order
        DELIVERED,       // Order successfully delivered
        FAILED           // Delivery failed
    }

    /**
     * Assign a driver to this delivery
     */
    public void assignDriver(Driver driver) {
        this.driver = driver;
        this.status = DeliveryStatus.ASSIGNED;
    }

    /**
     * Mark order as picked up
     */
    public void markPickedUp() {
        this.pickupTime = LocalDateTime.now();
        this.status = DeliveryStatus.PICKED_UP;
    }

    /**
     * Mark order as in transit
     */
    public void markInTransit() {
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    /**
     * Mark order as delivered
     */
    public void markDelivered() {
        this.deliveryTime = LocalDateTime.now();
        this.status = DeliveryStatus.DELIVERED;
    }

    /**
     * Add tracking note
     */
    public void addTrackingNote(String note) {
        if (this.trackingNotes == null) {
            this.trackingNotes = note;
        } else {
            this.trackingNotes += "\n" + note;
        }
    }

    /**
     * Check if delivery is completed
     */
    public boolean isCompleted() {
        return this.status == DeliveryStatus.DELIVERED || 
               this.status == DeliveryStatus.FAILED;
    }
}
