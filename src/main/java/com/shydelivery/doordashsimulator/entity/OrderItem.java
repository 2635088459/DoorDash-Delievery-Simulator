package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * OrderItem entity - Stores individual items within an order
 * This is a junction table linking orders and menu items
 * Records quantity and price snapshot at order time
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_menu_item_id", columnList = "menu_item_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order this item belongs to - Many order items belong to one order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Menu item being ordered - Many order items reference one menu item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /**
     * Quantity of this menu item ordered
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price per unit at the time of order
     * Captured to preserve historical pricing even if menu item price changes later
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Total price for this line item (quantity × unit_price)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Special requests or modifications for this item
     * Examples: "No onions", "Extra spicy", "Well done"
     */
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /**
     * Calculate subtotal from quantity and unit price
     */
    public void calculateSubtotal() {
        if (this.quantity != null && this.unitPrice != null) {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    /**
     * Set unit price from menu item
     * Captures the current price to preserve historical data
     * Note: Relies on Lombok @Data annotation to generate getPrice() method
     */
    public void setUnitPriceFromMenuItem(MenuItem menuItem) {
        if (menuItem != null) {
            // Lombok @Data generates getPrice() at compile time
            // This will work in Docker Maven build
            this.unitPrice = menuItem.getPrice();
        }
    }

    /**
     * Convenience method to set quantity and calculate subtotal
     */
    public void setQuantityAndCalculate(Integer quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }
}
