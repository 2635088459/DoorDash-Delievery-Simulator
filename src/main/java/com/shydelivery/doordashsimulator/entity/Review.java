package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Review entity - Stores customer reviews and ratings
 * Each order can have one review
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_restaurant_id", columnList = "restaurant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order being reviewed - One review per order
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /**
     * Customer who wrote the review - Many reviews by one customer
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Restaurant being reviewed - Many reviews for one restaurant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /**
     * Rating for food quality (1-5 stars)
     */
    @Column(name = "food_rating", nullable = false)
    private Integer foodRating;

    /**
     * Rating for delivery service (1-5 stars)
     */
    @Column(name = "delivery_rating", nullable = false)
    private Integer deliveryRating;

    /**
     * Overall rating calculated from food and delivery ratings
     */
    @Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal overallRating;

    /**
     * Written review comment
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate overall rating from food and delivery ratings
     */
    public void calculateOverallRating() {
        if (this.foodRating != null && this.deliveryRating != null) {
            double average = (this.foodRating + this.deliveryRating) / 2.0;
            this.overallRating = BigDecimal.valueOf(average);
        }
    }

    /**
     * Check if review is positive (rating >= 4.0)
     */
    public boolean isPositive() {
        return this.overallRating != null && 
               this.overallRating.compareTo(BigDecimal.valueOf(4.0)) >= 0;
    }

    /**
     * Check if review is negative (rating < 3.0)
     */
    public boolean isNegative() {
        return this.overallRating != null && 
               this.overallRating.compareTo(BigDecimal.valueOf(3.0)) < 0;
    }
}
