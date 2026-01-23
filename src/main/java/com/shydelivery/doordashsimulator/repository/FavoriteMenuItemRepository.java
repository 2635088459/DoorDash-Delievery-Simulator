package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.FavoriteMenuItem;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏菜品数据访问层
 */
@Repository
public interface FavoriteMenuItemRepository extends JpaRepository<FavoriteMenuItem, Long> {
    
    /**
     * 查找用户对特定菜品的收藏
     */
    Optional<FavoriteMenuItem> findByUserAndMenuItem(User user, MenuItem menuItem);
    
    /**
     * 查找用户的所有收藏菜品
     */
    List<FavoriteMenuItem> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 查找用户收藏的所有菜品（只返回菜品）
     */
    @Query("SELECT fm.menuItem FROM FavoriteMenuItem fm WHERE fm.user = :user ORDER BY fm.createdAt DESC")
    List<MenuItem> findMenuItemsByUser(@Param("user") User user);
    
    /**
     * 检查用户是否收藏了某个菜品
     */
    boolean existsByUserAndMenuItem(User user, MenuItem menuItem);
    
    /**
     * 统计用户收藏的菜品数量
     */
    long countByUser(User user);
    
    /**
     * 统计菜品被收藏的次数
     */
    long countByMenuItem(MenuItem menuItem);
    
    /**
     * 根据用户ID和菜品ID删除收藏
     */
    @Query("DELETE FROM FavoriteMenuItem fm WHERE fm.user.id = :userId AND fm.menuItem.id = :menuItemId")
    void deleteByUserIdAndMenuItemId(@Param("userId") Long userId, @Param("menuItemId") Long menuItemId);
    
    /**
     * 查找用户在特定餐厅收藏的所有菜品
     */
    @Query("SELECT fm FROM FavoriteMenuItem fm WHERE fm.user = :user AND fm.menuItem.restaurant.id = :restaurantId ORDER BY fm.createdAt DESC")
    List<FavoriteMenuItem> findByUserAndRestaurantId(@Param("user") User user, @Param("restaurantId") Long restaurantId);
}
