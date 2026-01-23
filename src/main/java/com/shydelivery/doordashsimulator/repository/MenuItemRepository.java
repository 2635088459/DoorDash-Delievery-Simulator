package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * MenuItem Repository - Data access for MenuItem entities
 * 
 * RBAC Context:
 * - Public can view available menu items for any restaurant
 * - Restaurant owners can manage (CRUD) their own restaurant's menu items
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    
    /**
     * Find all menu items for a specific restaurant
     * Used by: Public and RESTAURANT_OWNER to view menu
     */
    List<MenuItem> findByRestaurant(Restaurant restaurant);
    
    /**
     * Find all available menu items for a specific restaurant
     * Used by: Public to view only available items
     */
    List<MenuItem> findByRestaurantAndIsAvailableTrue(Restaurant restaurant);
    
    /**
     * Find menu items by restaurant ID
     * Used by: RESTAURANT_OWNER role queries
     */
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId ORDER BY m.category, m.name")
    List<MenuItem> findByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Find available menu items by restaurant ID
     * Used by: Public queries for ordering
     */
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.isAvailable = true ORDER BY m.category, m.name")
    List<MenuItem> findAvailableByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Find menu items by category
     * Used by: Filtering menu items by category
     */
    List<MenuItem> findByRestaurantAndCategory(Restaurant restaurant, String category);
    
    /**
     * Find vegetarian menu items
     * Used by: Dietary preference filtering
     */
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.isVegetarian = true AND m.isAvailable = true")
    List<MenuItem> findVegetarianByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Find vegan menu items
     * Used by: Dietary preference filtering
     */
    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.isVegan = true AND m.isAvailable = true")
    List<MenuItem> findVeganByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    /**
     * Check if a menu item belongs to a specific restaurant
     * Used by: Ownership verification
     */
    boolean existsByIdAndRestaurant(Long id, Restaurant restaurant);
    
    /**
     * 综合搜索菜品
     * 支持多条件过滤和排序
     */
    @Query("SELECT m FROM MenuItem m WHERE " +
            "(:keyword IS NULL OR LOWER(m.name) LIKE CONCAT('%', :keyword, '%') OR LOWER(m.description) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:restaurantId IS NULL OR m.restaurant.id = :restaurantId) AND " +
            "(:category IS NULL OR m.category = :category) AND " +
            "(:minPrice IS NULL OR m.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR m.price <= :maxPrice) AND " +
            "(:availableOnly IS NULL OR :availableOnly = false OR m.isAvailable = true)")
    Page<MenuItem> searchMenuItems(
            @Param("keyword") String keyword,
            @Param("restaurantId") Long restaurantId,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("availableOnly") Boolean availableOnly,
            Pageable pageable
    );
}
