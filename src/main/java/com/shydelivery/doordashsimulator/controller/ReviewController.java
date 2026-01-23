package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.CreateReviewRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateReviewRequest;
import com.shydelivery.doordashsimulator.dto.response.RestaurantRatingDTO;
import com.shydelivery.doordashsimulator.dto.response.ReviewDTO;
import com.shydelivery.doordashsimulator.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReviewController - 评价管理 REST API
 * 
 * RBAC 三层防御:
 * 1. JWT 过滤器 - 验证身份
 * 2. @PreAuthorize - 验证角色
 * 3. AuthorizationService - 验证资源所有权
 * 
 * 端点设计:
 * - POST /reviews - CUSTOMER：创建评价
 * - PUT /reviews/{id} - CUSTOMER：更新评价（仅限创建者）
 * - DELETE /reviews/{id} - CUSTOMER：删除评价（仅限创建者）
 * - GET /reviews/{id} - 公开：获取评价详情
 * - GET /reviews/order/{orderId} - 公开：获取订单评价
 * - GET /reviews/restaurant/{restaurantId} - 公开：获取餐厅评价列表
 * - GET /reviews/restaurant/{restaurantId}/rating - 公开：获取餐厅评分统计
 * - GET /reviews/my - CUSTOMER：获取我的评价
 * 
 * @author DoorDash Team
 */
@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    /**
     * 创建评价 (仅 CUSTOMER, 仅限已完成订单)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 权限验证: AuthorizationService.verifyOrderOwnershipForReview()
     * 
     * @param request 创建评价请求
     * @param authentication 认证信息
     * @return 创建的评价
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        
        log.info("API - 创建评价: orderId={}, user={}", request.getOrderId(), authentication.getName());
        
        ReviewDTO review = reviewService.createReview(request, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
    
    /**
     * 更新评价 (仅 CUSTOMER, 仅限评价创建者)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 权限验证: AuthorizationService.verifyReviewOwnership()
     * 
     * @param id 评价 ID
     * @param request 更新评价请求
     * @param authentication 认证信息
     * @return 更新后的评价
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {
        
        log.info("API - 更新评价: reviewId={}, user={}", id, authentication.getName());
        
        ReviewDTO review = reviewService.updateReview(id, request, authentication.getName());
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * 删除评价 (仅 CUSTOMER, 仅限评价创建者)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 权限验证: AuthorizationService.verifyReviewOwnership()
     * 
     * @param id 评价 ID
     * @param authentication 认证信息
     * @return 无内容
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("API - 删除评价: reviewId={}, user={}", id, authentication.getName());
        
        reviewService.deleteReview(id, authentication.getName());
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取评价详情 (公开)
     * 
     * @param id 评价 ID
     * @return 评价详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        log.info("API - 获取评价详情: reviewId={}", id);
        
        ReviewDTO review = reviewService.getReviewById(id);
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * 获取订单的评价 (公开)
     * 
     * @param orderId 订单 ID
     * @return 订单评价
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReviewDTO> getReviewByOrderId(@PathVariable Long orderId) {
        log.info("API - 获取订单评价: orderId={}", orderId);
        
        ReviewDTO review = reviewService.getReviewByOrderId(orderId);
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * 获取餐厅的所有评价 (公开)
     * 
     * @param restaurantId 餐厅 ID
     * @return 评价列表
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByRestaurant(@PathVariable Long restaurantId) {
        log.info("API - 获取餐厅评价列表: restaurantId={}", restaurantId);
        
        List<ReviewDTO> reviews = reviewService.getReviewsByRestaurant(restaurantId);
        
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * 获取餐厅的评分统计 (公开)
     * 
     * @param restaurantId 餐厅 ID
     * @return 评分统计
     */
    @GetMapping("/restaurant/{restaurantId}/rating")
    public ResponseEntity<RestaurantRatingDTO> getRestaurantRating(@PathVariable Long restaurantId) {
        log.info("API - 获取餐厅评分统计: restaurantId={}", restaurantId);
        
        RestaurantRatingDTO rating = reviewService.getRestaurantRating(restaurantId);
        
        return ResponseEntity.ok(rating);
    }
    
    /**
     * 获取我的所有评价 (仅 CUSTOMER)
     * 
     * RBAC: @PreAuthorize("hasRole('CUSTOMER')")
     * 
     * @param authentication 认证信息
     * @return 我的评价列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(Authentication authentication) {
        log.info("API - 获取我的评价: user={}", authentication.getName());
        
        List<ReviewDTO> reviews = reviewService.getMyReviews(authentication.getName());
        
        return ResponseEntity.ok(reviews);
    }
}
