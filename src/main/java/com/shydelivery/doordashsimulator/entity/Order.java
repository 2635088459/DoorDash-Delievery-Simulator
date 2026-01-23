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
 * Order entity - Stores customer orders
 * Represents a complete order from a customer to a restaurant
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Customer who placed the order - Many orders belong to one customer
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Restaurant where the order is placed - Many orders belong to one restaurant
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /**
     * Delivery address for this order - Many orders can use one address
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;
    
    /**
     * Driver assigned to deliver this order - Many orders can be delivered by one driver
     * Nullable until a driver accepts the order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    /**
     * Human-readable order number for customer reference
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Current status of the order
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    /**
     * Sum of all order items (before delivery fee and tax)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Delivery charge for this order
     */
    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    /**
     * Tax amount calculated on subtotal
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    /**
     * Final total amount (subtotal + delivery_fee + tax)
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Payment method used for this order
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    /**
     * Payment status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Special instructions for delivery (e.g., "Leave at door", "Ring doorbell")
     */
    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    /**
     * Estimated delivery time
     */
    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    /**
     * Actual delivery time (when order was delivered)
     */
    @Column(name = "actual_delivery")
    private LocalDateTime actualDelivery;
    
    /**
     * Time when driver picked up the order from restaurant
     */
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;
    
    /**
     * Phase 2: 配送距离（公里）
     * 从餐厅到配送地址的距离，用于动态定价
     */
    @Column(name = "delivery_distance_km", precision = 10, scale = 2)
    private BigDecimal deliveryDistanceKm;
    
    /**
     * Phase 2: 下单时的天气状况
     * 用于记录是否因恶劣天气加价
     */
    @Column(name = "weather_condition", length = 50)
    private String weatherCondition;
    
    /**
     * Phase 2: 是否因恶劣天气加价
     */
    @Column(name = "bad_weather_surcharge")
    private Boolean badWeatherSurcharge = false;
    
    /**
     * Phase 2: 是否在高峰期下单
     */
    @Column(name = "peak_hour_surcharge")
    private Boolean peakHourSurcharge = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Order status enumeration
     */
    public enum OrderStatus {
        PENDING,           // Order placed, waiting for restaurant confirmation
        CONFIRMED,         // Restaurant confirmed the order
        PREPARING,         // Food is being prepared
        READY_FOR_PICKUP,  // Food ready, waiting for driver
        PICKED_UP,         // Driver picked up the order
        IN_TRANSIT,        // Order is being delivered
        DELIVERED,         // Order successfully delivered
        CANCELLED          // Order was cancelled
    }

    /**
     * Payment method enumeration
     */
    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        CASH,
        PAYPAL,
        APPLE_PAY,
        GOOGLE_PAY
    }

    /**
     * Payment status enumeration
     */
    public enum PaymentStatus {
        PENDING,    // Payment not yet processed
        COMPLETED,  // Payment successful
        FAILED,     // Payment failed
        REFUNDED    // Payment was refunded
    }

    /**
     * Calculate total amount from subtotal, delivery fee, and tax
     */
    public void calculateTotalAmount() {
        this.totalAmount = this.subtotal
            .add(this.deliveryFee)
            .add(this.tax);
    }

    /**
     * Check if order is in a cancellable state
     */
    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING || 
               this.status == OrderStatus.CONFIRMED;
    }

    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.DELIVERED || 
               this.status == OrderStatus.CANCELLED;
    }

    /**
     * Check if order is in progress
     */
    public boolean isInProgress() {
        return this.status == OrderStatus.PREPARING || 
               this.status == OrderStatus.READY_FOR_PICKUP ||
               this.status == OrderStatus.PICKED_UP ||
               this.status == OrderStatus.IN_TRANSIT;
    }
}
