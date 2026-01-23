package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户收藏的餐厅
 * 记录用户对餐厅的收藏关系
 */
@Entity
@Table(name = "favorite_restaurants", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "restaurant_id"}),
       indexes = {
           @Index(name = "idx_favorite_rest_user", columnList = "user_id"),
           @Index(name = "idx_favorite_rest_restaurant", columnList = "restaurant_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteRestaurant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 收藏的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 收藏的餐厅
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
    
    /**
     * 收藏时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 备注（可选，用户可以添加收藏原因）
     */
    @Column(name = "note", length = 500)
    private String note;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteRestaurant)) return false;
        FavoriteRestaurant that = (FavoriteRestaurant) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
