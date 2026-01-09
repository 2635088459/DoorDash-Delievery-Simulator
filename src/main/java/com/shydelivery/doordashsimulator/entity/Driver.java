package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Driver entity - Stores delivery driver information
 * Each driver has a one-to-one relationship with a User account (role: DRIVER)
 */
@Entity
@Table(name = "drivers", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_is_available", columnList = "is_available")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated user account - One driver has one user account
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Driver's license number
     */
    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    /**
     * Type of vehicle used for delivery
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 50)
    private VehicleType vehicleType;

    /**
     * Vehicle license plate number
     */
    @Column(name = "vehicle_plate", nullable = false, length = 20)
    private String vehiclePlate;

    /**
     * Whether the driver is currently available for deliveries
     */
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    /**
     * Current GPS latitude position
     */
    @Column(name = "current_latitude", precision = 10, scale = 8)
    private BigDecimal currentLatitude;

    /**
     * Current GPS longitude position
     */
    @Column(name = "current_longitude", precision = 11, scale = 8)
    private BigDecimal currentLongitude;

    /**
     * Average driver rating from customer reviews (0.00 - 5.00)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Total number of completed deliveries
     */
    @Column(name = "total_deliveries", nullable = false)
    private Integer totalDeliveries = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Vehicle type enumeration
     */
    public enum VehicleType {
        CAR,
        MOTORCYCLE,
        BICYCLE,
        SCOOTER,
        WALKING
    }

    /**
     * Update current location
     */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.currentLatitude = latitude;
        this.currentLongitude = longitude;
    }

    /**
     * Check if driver has current location
     */
    public boolean hasLocation() {
        return this.currentLatitude != null && this.currentLongitude != null;
    }

    /**
     * Mark driver as available
     */
    public void goOnline() {
        this.isAvailable = true;
    }

    /**
     * Mark driver as unavailable
     */
    public void goOffline() {
        this.isAvailable = false;
    }

    /**
     * Increment total deliveries count
     */
    public void incrementDeliveries() {
        this.totalDeliveries++;
    }
}
