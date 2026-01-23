package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Cart;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cart Repository - 购物车数据访问接口
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    /**
     * 查找用户在特定餐厅的购物车
     */
    Optional<Cart> findByCustomerAndRestaurantAndIsActiveTrue(User customer, Restaurant restaurant);
    
    /**
     * 查找用户的所有有效购物车
     */
    List<Cart> findByCustomerAndIsActiveTrueOrderByUpdatedAtDesc(User customer);
    
    /**
     * 根据用户ID查找所有有效购物车
     */
    @Query("SELECT c FROM Cart c WHERE c.customer.id = :customerId AND c.isActive = true ORDER BY c.updatedAt DESC")
    List<Cart> findActiveCartsByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * 查找用户在特定餐厅的购物车（包括不活跃的）
     */
    Optional<Cart> findByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);
    
    /**
     * 删除过期的购物车
     */
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.expiresAt < :now")
    int deleteExpiredCarts(@Param("now") LocalDateTime now);
    
    /**
     * 查找所有过期的购物车
     */
    @Query("SELECT c FROM Cart c WHERE c.expiresAt < :now AND c.isActive = true")
    List<Cart> findExpiredCarts(@Param("now") LocalDateTime now);
    
    /**
     * 设置购物车为不活跃
     */
    @Modifying
    @Query("UPDATE Cart c SET c.isActive = false WHERE c.id = :cartId")
    int deactivateCart(@Param("cartId") Long cartId);
    
    /**
     * 清空用户的所有购物车
     */
    @Modifying
    @Query("UPDATE Cart c SET c.isActive = false WHERE c.customer.id = :customerId")
    int deactivateAllCartsByCustomer(@Param("customerId") Long customerId);
    
    /**
     * 统计用户的有效购物车数量
     */
    long countByCustomerIdAndIsActiveTrue(Long customerId);
}
