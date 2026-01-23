package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车商品项实体
 * 表示购物车中的一个菜品及其数量
 */
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_id", columnList = "cart_id"),
    @Index(name = "idx_menu_item_id", columnList = "menu_item_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属购物车
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * 菜品
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /**
     * 数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * 添加时的价格（用于检测价格变化）
     */
    @Column(name = "price_at_add", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtAdd;

    /**
     * 特殊要求（可选）
     */
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 计算该商品项的小计
     */
    public BigDecimal getSubtotal() {
        return priceAtAdd.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 检查价格是否发生变化
     */
    public boolean hasPriceChanged() {
        return menuItem != null && 
               !priceAtAdd.equals(menuItem.getPrice());
    }

    /**
     * 获取当前价格
     */
    public BigDecimal getCurrentPrice() {
        return menuItem != null ? menuItem.getPrice() : priceAtAdd;
    }

    /**
     * 检查菜品是否仍然可用
     */
    public boolean isMenuItemAvailable() {
        return menuItem != null && menuItem.getIsAvailable();
    }

    /**
     * 增加数量
     */
    public void increaseQuantity(int amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }

    /**
     * 减少数量
     */
    public void decreaseQuantity(int amount) {
        if (amount > 0 && this.quantity > amount) {
            this.quantity -= amount;
        }
    }

    /**
     * 设置数量
     */
    public void setQuantity(int quantity) {
        if (quantity > 0) {
            this.quantity = quantity;
        }
    }
}
