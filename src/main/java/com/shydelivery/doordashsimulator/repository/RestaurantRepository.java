package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Restaurant Repository - 餐厅数据访问接口
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    
    /**
     * 根据所有者查找餐厅列表
     */
    List<Restaurant> findByOwner(User owner);
    
    /**
     * 根据所有者ID查找餐厅列表
     */
    List<Restaurant> findByOwnerId(Long ownerId);
    
    /**
     * 根据菜系类型查找餐厅
     */
    List<Restaurant> findByCuisineType(String cuisineType);
    
    /**
     * 查找活跃的餐厅
     */
    List<Restaurant> findByIsActiveTrue();
    
    /**
     * 根据评分查找餐厅（评分大于等于指定值）
     */
    List<Restaurant> findByRatingGreaterThanEqual(Double rating);
    
    /**
     * 综合搜索餐厅
     * 支持多条件过滤和排序
     */
    @Query("SELECT r FROM Restaurant r WHERE " +
            "(:keyword IS NULL OR LOWER(r.name) LIKE CONCAT('%', :keyword, '%') OR LOWER(r.description) LIKE CONCAT('%', :keyword, '%')) AND " +
            "(:cuisineType IS NULL OR r.cuisineType = :cuisineType) AND " +
            "(:minRating IS NULL OR r.rating >= :minRating) AND " +
            "(:maxRating IS NULL OR r.rating <= :maxRating) AND " +
            "(:minDeliveryFee IS NULL OR r.deliveryFee >= :minDeliveryFee) AND " +
            "(:maxDeliveryFee IS NULL OR r.deliveryFee <= :maxDeliveryFee) AND " +
            "(:minOrderAmount IS NULL OR r.minimumOrder >= :minOrderAmount) AND " +
            "(:maxOrderAmount IS NULL OR r.minimumOrder <= :maxOrderAmount) AND " +
            "(:openOnly IS NULL OR :openOnly = false OR r.isActive = true)")
    Page<Restaurant> searchRestaurants(
            @Param("keyword") String keyword,
            @Param("cuisineType") String cuisineType,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("minDeliveryFee") BigDecimal minDeliveryFee,
            @Param("maxDeliveryFee") BigDecimal maxDeliveryFee,
            @Param("minOrderAmount") BigDecimal minOrderAmount,
            @Param("maxOrderAmount") BigDecimal maxOrderAmount,
            @Param("openOnly") Boolean openOnly,
            Pageable pageable
    );
}
