package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Restaurant entity - Stores restaurant information
 * Each restaurant is owned by a user with RESTAURANT_OWNER role
 */
@Entity
@Table(name = "restaurants", indexes = {
    @Index(name = "idx_owner_id", columnList = "owner_id"),
    @Index(name = "idx_cuisine_type", columnList = "cuisine_type"),
    @Index(name = "idx_is_active", columnList = "is_active"),
    @Index(name = "idx_rating", columnList = "rating")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Restaurant owner - Many restaurants can belong to one user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cuisine_type", nullable = false, length = 100)
    private String cuisineType;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String email;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Average rating from customer reviews (0.00 - 5.00)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    /**
     * Base delivery fee charged by this restaurant
     */
    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    /**
     * Minimum order amount required for delivery
     */
    @Column(name = "minimum_order", precision = 10, scale = 2)
    private BigDecimal minimumOrder = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
