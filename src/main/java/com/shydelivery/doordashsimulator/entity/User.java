package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity - Stores all user accounts (customers, restaurant owners, delivery drivers)
 * This is the core entity that supports multiple user roles in the system
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone_number"),
    @Index(name = "idx_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * AWS Cognito User Sub (unique identifier from Cognito)
     * This links our local user to Cognito user pool
     */
    @Column(name = "cognito_sub", unique = true, length = 255)
    private String cognitoSub;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    /**
     * Password hash (BCrypt)
     * Note: For AWS Cognito users, this will be null
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /**
     * User role: CUSTOMER, RESTAURANT_OWNER, DRIVER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User role enumeration
     */
    public enum UserRole {
        CUSTOMER,
        RESTAURANT_OWNER,
        DRIVER
    }
}
