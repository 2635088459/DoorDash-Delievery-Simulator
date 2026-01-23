package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.FavoriteRestaurant;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏餐厅数据访问层
 */
@Repository
public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant, Long> {
    
    /**
     * 查找用户对特定餐厅的收藏
     */
    Optional<FavoriteRestaurant> findByUserAndRestaurant(User user, Restaurant restaurant);
    
    /**
     * 查找用户的所有收藏餐厅
     */
    List<FavoriteRestaurant> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 查找用户收藏的所有餐厅（只返回餐厅）
     */
    @Query("SELECT fr.restaurant FROM FavoriteRestaurant fr WHERE fr.user = :user ORDER BY fr.createdAt DESC")
    List<Restaurant> findRestaurantsByUser(@Param("user") User user);
    
    /**
     * 检查用户是否收藏了某个餐厅
     */
    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);
    
    /**
     * 统计用户收藏的餐厅数量
     */
    long countByUser(User user);
    
    /**
     * 统计餐厅被收藏的次数
     */
    long countByRestaurant(Restaurant restaurant);
    
    /**
     * 根据用户ID和餐厅ID删除收藏
     */
    @Query("DELETE FROM FavoriteRestaurant fr WHERE fr.user.id = :userId AND fr.restaurant.id = :restaurantId")
    void deleteByUserIdAndRestaurantId(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);
}
