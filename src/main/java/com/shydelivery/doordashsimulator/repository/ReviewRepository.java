package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.Review;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ReviewRepository - 评价数据访问层
 * 
 * 提供评价的 CRUD 操作和查询方法
 * 
 * @author DoorDash Team
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * 根据订单ID查找评价
     * 
     * @param orderId 订单ID
     * @return 评价（Optional）
     */
    Optional<Review> findByOrderId(Long orderId);
    
    /**
     * 根据客户查找所有评价
     * 
     * @param customer 客户用户对象
     * @return 客户的所有评价列表
     */
    List<Review> findByCustomer(User customer);
    
    /**
     * 根据餐厅查找所有评价
     * 
     * @param restaurant 餐厅对象
     * @return 餐厅的所有评价列表
     */
    List<Review> findByRestaurant(Restaurant restaurant);
    
    /**
     * 根据餐厅ID查找所有评价（按创建时间降序）
     * 
     * @param restaurantId 餐厅ID
     * @return 餐厅的所有评价列表
     */
    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    
    /**
     * 查找餐厅的正面评价（评分 >= 4.0）
     * 
     * @param restaurantId 餐厅ID
     * @return 正面评价列表
     */
    @Query("SELECT r FROM Review r WHERE r.restaurant.id = :restaurantId AND r.overallRating >= 4.0 ORDER BY r.createdAt DESC")
    List<Review> findPositiveReviewsByRestaurant(@Param("restaurantId") Long restaurantId);
    
    /**
     * 计算餐厅的平均总体评分
     * 
     * @param restaurantId 餐厅ID
     * @return 平均评分
     */
    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    BigDecimal calculateAverageRatingForRestaurant(@Param("restaurantId") Long restaurantId);
    
    /**
     * 计算餐厅的平均食物评分
     * 
     * @param restaurantId 餐厅ID
     * @return 平均食物评分
     */
    @Query("SELECT AVG(r.foodRating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double calculateAverageFoodRatingForRestaurant(@Param("restaurantId") Long restaurantId);
    
    /**
     * 计算餐厅的平均配送评分
     * 
     * @param restaurantId 餐厅ID
     * @return 平均配送评分
     */
    @Query("SELECT AVG(r.deliveryRating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double calculateAverageDeliveryRatingForRestaurant(@Param("restaurantId") Long restaurantId);
    
    /**
     * 统计餐厅的评价总数
     * 
     * @param restaurantId 餐厅ID
     * @return 评价总数
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Long countReviewsByRestaurant(@Param("restaurantId") Long restaurantId);
    
    /**
     * 检查订单是否已有评价
     * 
     * @param orderId 订单ID
     * @return 是否存在评价
     */
    boolean existsByOrderId(Long orderId);
}
