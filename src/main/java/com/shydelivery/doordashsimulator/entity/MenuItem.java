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
 * MenuItem entity - Stores menu items for each restaurant
 * Each menu item belongs to one restaurant
 */
@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_is_available", columnList = "is_available")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Restaurant this menu item belongs to - Many menu items belong to one restaurant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Category: Appetizer, Main Course, Dessert, Beverage, etc.
     */
    @Column(nullable = false, length = 100)
    private String category;

    /**
     * Price of the menu item
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * URL to the image of the menu item
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Whether this item is currently available for ordering
     */
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    /**
     * Whether this item is suitable for vegetarians
     */
    @Column(name = "is_vegetarian", nullable = false)
    private Boolean isVegetarian = false;

    /**
     * Whether this item is suitable for vegans
     */
    @Column(name = "is_vegan", nullable = false)
    private Boolean isVegan = false;

    /**
     * Spice level: 0 (not spicy) to 5 (very spicy)
     */
    @Column(name = "spice_level", nullable = false)
    private Integer spiceLevel = 0;

    /**
     * Estimated preparation time in minutes
     */
    @Column(name = "preparation_time", nullable = false)
    private Integer preparationTime = 15;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
