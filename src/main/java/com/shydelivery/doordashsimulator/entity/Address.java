package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Address entity - Stores delivery addresses for users
 * Each user can have multiple addresses
 */
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_is_default", columnList = "is_default")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who owns this address - Many addresses belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    /**
     * GPS coordinates for delivery location
     * Latitude: -90 to 90
     */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * GPS coordinates for delivery location
     * Longitude: -180 to 180
     */
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * Whether this is the user's default delivery address
     * Only one address per user should be default
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Returns a formatted full address string
     */
    public String getFullAddress() {
        return String.format("%s, %s, %s %s", 
            streetAddress, city, state, zipCode);
    }

    /**
     * Checks if GPS coordinates are available
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
