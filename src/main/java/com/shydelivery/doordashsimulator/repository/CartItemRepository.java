package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Cart;
import com.shydelivery.doordashsimulator.entity.CartItem;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CartItem Repository - 购物车商品项数据访问接口
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    /**
     * 查找购物车中的特定菜品
     */
    Optional<CartItem> findByCartAndMenuItem(Cart cart, MenuItem menuItem);
    
    /**
     * 查找购物车的所有商品
     */
    List<CartItem> findByCartOrderByCreatedAtDesc(Cart cart);
    
    /**
     * 查找购物车中的特定菜品（通过ID）
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.menuItem.id = :menuItemId")
    Optional<CartItem> findByCartIdAndMenuItemId(@Param("cartId") Long cartId, @Param("menuItemId") Long menuItemId);
    
    /**
     * 删除购物车的所有商品
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteByCartId(@Param("cartId") Long cartId);
    
    /**
     * 统计购物车中的商品数量
     */
    long countByCart(Cart cart);
    
    /**
     * 查找包含特定菜品的所有购物车商品
     */
    List<CartItem> findByMenuItem(MenuItem menuItem);
}
