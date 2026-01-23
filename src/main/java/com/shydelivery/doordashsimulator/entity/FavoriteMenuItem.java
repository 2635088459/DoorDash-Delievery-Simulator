package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户收藏的菜品
 * 记录用户对菜品的收藏关系
 */
@Entity
@Table(name = "favorite_menu_items", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "menu_item_id"}),
       indexes = {
           @Index(name = "idx_favorite_menu_user", columnList = "user_id"),
           @Index(name = "idx_favorite_menu_item", columnList = "menu_item_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteMenuItem {
    
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
     * 收藏的菜品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;
    
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
        if (!(o instanceof FavoriteMenuItem)) return false;
        FavoriteMenuItem that = (FavoriteMenuItem) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
