package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreateReviewRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateReviewRequest;
import com.shydelivery.doordashsimulator.dto.response.RestaurantRatingDTO;
import com.shydelivery.doordashsimulator.dto.response.ReviewDTO;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Review;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.ReviewRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ReviewService - 评价业务逻辑服务
 * 
 * 功能：
 * - 创建评价（仅限已完成订单）
 * - 更新评价（仅限评价创建者）
 * - 删除评价
 * - 查询评价
 * - 计算餐厅评分统计
 * 
 * 业务规则：
 * - 只有订单完成后才能评价
 * - 每个订单只能评价一次
 * - 评分范围 1-5 星
 * - 只有评价创建者才能修改/删除评价
 * 
 * @author DoorDash Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthorizationService authorizationService;
    
    /**
     * 创建评价 (CUSTOMER 角色)
     * 
     * @param request 创建评价请求
     * @param customerEmail 客户邮箱
     * @return 创建的评价 DTO
     */
    @Transactional
    public ReviewDTO createReview(CreateReviewRequest request, String customerEmail) {
        log.info("创建评价: orderId={}, customer={}", request.getOrderId(), customerEmail);
        
        // 验证订单所有权
        Order order = authorizationService.verifyOrderOwnershipForReview(
            request.getOrderId(), customerEmail);
        
        // 验证订单状态必须是已完成
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("只有已完成的订单才能评价");
        }
        
        // 检查订单是否已经评价过
        if (reviewRepository.existsByOrderId(request.getOrderId())) {
            throw new IllegalStateException("该订单已经评价过了");
        }
        
        // 获取客户用户对象
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + customerEmail));
        
        // 创建评价实体
        Review review = Review.builder()
            .order(order)
            .customer(customer)
            .restaurant(order.getRestaurant())
            .foodRating(request.getFoodRating())
            .deliveryRating(request.getDeliveryRating())
            .comment(request.getComment())
            .build();
        
        // 计算总体评分
        review.calculateOverallRating();
        
        // 保存评价
        Review saved = reviewRepository.save(review);
        log.info("评价创建成功: reviewId={}, orderId={}", saved.getId(), request.getOrderId());
        
        return convertToDTO(saved);
    }
    
    /**
     * 更新评价 (CUSTOMER 角色, 仅限评价创建者)
     * 
     * @param reviewId 评价 ID
     * @param request 更新评价请求
     * @param customerEmail 客户邮箱
     * @return 更新后的评价 DTO
     */
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UpdateReviewRequest request, String customerEmail) {
        log.info("更新评价: reviewId={}, customer={}", reviewId, customerEmail);
        
        // 验证评价所有权
        authorizationService.verifyReviewOwnership(reviewId, customerEmail);
        
        // 获取评价
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("评价不存在，ID: " + reviewId));
        
        // 更新字段
        if (request.getFoodRating() != null) {
            review.setFoodRating(request.getFoodRating());
        }
        if (request.getDeliveryRating() != null) {
            review.setDeliveryRating(request.getDeliveryRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        
        // 重新计算总体评分
        review.calculateOverallRating();
        
        // 保存
        Review updated = reviewRepository.save(review);
        log.info("评价更新成功: reviewId={}", reviewId);
        
        return convertToDTO(updated);
    }
    
    /**
     * 删除评价 (CUSTOMER 角色, 仅限评价创建者)
     * 
     * @param reviewId 评价 ID
     * @param customerEmail 客户邮箱
     */
    @Transactional
    public void deleteReview(Long reviewId, String customerEmail) {
        log.info("删除评价: reviewId={}, customer={}", reviewId, customerEmail);
        
        // 验证评价所有权
        authorizationService.verifyReviewOwnership(reviewId, customerEmail);
        
        // 删除评价
        reviewRepository.deleteById(reviewId);
        log.info("评价删除成功: reviewId={}", reviewId);
    }
    
    /**
     * 根据ID获取评价 (公开)
     * 
     * @param reviewId 评价 ID
     * @return 评价 DTO
     */
    @Transactional(readOnly = true)
    public ReviewDTO getReviewById(Long reviewId) {
        log.info("获取评价: reviewId={}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("评价不存在，ID: " + reviewId));
        
        return convertToDTO(review);
    }
    
    /**
     * 根据订单ID获取评价 (公开)
     * 
     * @param orderId 订单 ID
     * @return 评价 DTO
     */
    @Transactional(readOnly = true)
    public ReviewDTO getReviewByOrderId(Long orderId) {
        log.info("获取订单评价: orderId={}", orderId);
        
        Review review = reviewRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("该订单暂无评价"));
        
        return convertToDTO(review);
    }
    
    /**
     * 获取餐厅的所有评价 (公开)
     * 
     * @param restaurantId 餐厅 ID
     * @return 评价列表
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewsByRestaurant(Long restaurantId) {
        log.info("获取餐厅评价: restaurantId={}", restaurantId);
        
        // 验证餐厅存在
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId);
        }
        
        List<Review> reviews = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        log.info("找到 {} 条餐厅评价", reviews.size());
        
        return reviews.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取客户的所有评价 (CUSTOMER 角色)
     * 
     * @param customerEmail 客户邮箱
     * @return 评价列表
     */
    @Transactional(readOnly = true)
    public List<ReviewDTO> getMyReviews(String customerEmail) {
        log.info("获取我的评价: customer={}", customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在: " + customerEmail));
        
        List<Review> reviews = reviewRepository.findByCustomer(customer);
        log.info("找到 {} 条评价", reviews.size());
        
        return reviews.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取餐厅的评分统计 (公开)
     * 
     * @param restaurantId 餐厅 ID
     * @return 评分统计 DTO
     */
    @Transactional(readOnly = true)
    public RestaurantRatingDTO getRestaurantRating(Long restaurantId) {
        log.info("获取餐厅评分统计: restaurantId={}", restaurantId);
        
        // 验证餐厅存在
        com.shydelivery.doordashsimulator.entity.Restaurant restaurant = 
            restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId));
        
        // 获取统计数据
        BigDecimal averageRating = reviewRepository.calculateAverageRatingForRestaurant(restaurantId);
        Double averageFoodRating = reviewRepository.calculateAverageFoodRatingForRestaurant(restaurantId);
        Double averageDeliveryRating = reviewRepository.calculateAverageDeliveryRatingForRestaurant(restaurantId);
        Long totalReviews = reviewRepository.countReviewsByRestaurant(restaurantId);
        
        // 统计正面和负面评价
        List<Review> allReviews = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
        long positiveCount = allReviews.stream().filter(Review::isPositive).count();
        long negativeCount = allReviews.stream().filter(Review::isNegative).count();
        
        return RestaurantRatingDTO.builder()
            .restaurantId(restaurantId)
            .restaurantName(restaurant.getName())
            .averageRating(averageRating != null ? averageRating : BigDecimal.ZERO)
            .averageFoodRating(averageFoodRating != null ? averageFoodRating : 0.0)
            .averageDeliveryRating(averageDeliveryRating != null ? averageDeliveryRating : 0.0)
            .totalReviews(totalReviews)
            .positiveReviews(positiveCount)
            .negativeReviews(negativeCount)
            .build();
    }
    
    /**
     * 将 Review 实体转换为 DTO
     * 
     * @param review 评价实体
     * @return 评价 DTO
     */
    private ReviewDTO convertToDTO(Review review) {
        String customerName = review.getCustomer().getFirstName() + " " + 
                            review.getCustomer().getLastName();
        
        return ReviewDTO.builder()
            .id(review.getId())
            .orderId(review.getOrder().getId())
            .orderNumber(review.getOrder().getOrderNumber())
            .customerId(review.getCustomer().getId())
            .customerName(customerName)
            .restaurantId(review.getRestaurant().getId())
            .restaurantName(review.getRestaurant().getName())
            .foodRating(review.getFoodRating())
            .deliveryRating(review.getDeliveryRating())
            .overallRating(review.getOverallRating())
            .comment(review.getComment())
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getCreatedAt()) // Review entity doesn't have updatedAt
            .isPositive(review.isPositive())
            .isNegative(review.isNegative())
            .build();
    }
}
