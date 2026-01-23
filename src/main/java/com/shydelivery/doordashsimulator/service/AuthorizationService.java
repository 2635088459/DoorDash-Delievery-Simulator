package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.entity.User.UserRole;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.MenuItemRepository;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.ReviewRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 授权服务 - 负责资源所有权验证
 * 
 * 功能：
 * - 验证餐厅所有权
 * - 验证订单访问权限
 * - 验证菜单项所有权
 * - 其他资源权限验证
 * 
 * @author DoorDash Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {
    
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final ReviewRepository reviewRepository;
    
    /**
     * 验证餐厅所有权
     * 
     * @param restaurantId 餐厅 ID
     * @param email 用户邮箱
     * @throws ResourceNotFoundException 餐厅不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户不是餐厅所有者
     */
    public void verifyRestaurantOwnership(Long restaurantId, String email) {
        log.debug("验证餐厅所有权: restaurantId={}, email={}", restaurantId, email);
        
        // 查找餐厅
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> {
                log.warn("餐厅不存在: id={}", restaurantId);
                return new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId);
            });
        
        // 查找用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证所有权
        if (!restaurant.getOwner().getId().equals(user.getId())) {
            log.warn("访问被拒绝: 用户 {} 尝试访问餐厅 {}, 但不是所有者", email, restaurantId);
            throw new AccessDeniedException("您没有权限访问此餐厅");
        }
        
        log.debug("餐厅所有权验证成功: 用户 {} 是餐厅 {} 的所有者", email, restaurantId);
    }
    
    /**
     * 获取用户并验证其为餐厅老板角色
     * 
     * @param email 用户邮箱
     * @return 用户实体
     * @throws UsernameNotFoundException 用户不存在
     */
    public User getUserAndVerifyRestaurantOwner(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + email));
        
        // 这里角色验证已经由 @PreAuthorize 完成，此方法主要用于获取用户实体
        log.debug("获取餐厅老板用户: email={}, role={}", email, user.getRole());
        
        return user;
    }
    
    /**
     * 检查用户是否拥有指定餐厅（不抛出异常，返回 boolean）
     * 
     * @param restaurantId 餐厅 ID
     * @param email 用户邮箱
     * @return true 如果用户拥有该餐厅，否则 false
     */
    public boolean isRestaurantOwner(Long restaurantId, String email) {
        try {
            verifyRestaurantOwnership(restaurantId, email);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ============ 订单权限验证 ============
    
    /**
     * 验证用户是否为订单的客户（所有者）
     * 
     * @param orderId 订单 ID
     * @param email 用户邮箱
     * @throws ResourceNotFoundException 订单不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户不是订单客户
     */
    public void verifyOrderCustomer(Long orderId, String email) {
        log.debug("验证订单客户权限: orderId={}, email={}", orderId, email);
        
        // 查找订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("订单不存在: id={}", orderId);
                return new ResourceNotFoundException("订单不存在，ID: " + orderId);
            });
        
        // 查找用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证是否为订单客户
        if (!order.getCustomer().getId().equals(user.getId())) {
            log.warn("访问被拒绝: 用户 {} 尝试访问订单 {}, 但不是订单客户", email, orderId);
            throw new AccessDeniedException("您没有权限访问此订单");
        }
        
        log.debug("订单客户权限验证成功: 用户 {} 是订单 {} 的客户", email, orderId);
    }
    
    /**
     * 验证用户是否为订单所属餐厅的所有者
     * 
     * @param orderId 订单 ID
     * @param email 用户邮箱（餐厅老板）
     * @throws ResourceNotFoundException 订单不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户不是餐厅所有者
     */
    public void verifyOrderRestaurantOwner(Long orderId, String email) {
        log.debug("验证订单餐厅所有者权限: orderId={}, email={}", orderId, email);
        
        // 查找订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("订单不存在: id={}", orderId);
                return new ResourceNotFoundException("订单不存在，ID: " + orderId);
            });
        
        // 查找用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证是否为餐厅所有者
        if (!order.getRestaurant().getOwner().getId().equals(user.getId())) {
            log.warn("访问被拒绝: 用户 {} 尝试访问订单 {}, 但不是餐厅所有者", email, orderId);
            throw new AccessDeniedException("您没有权限访问此订单");
        }
        
        log.debug("订单餐厅所有者权限验证成功: 用户 {} 是订单 {} 所属餐厅的所有者", email, orderId);
    }
    
    /**
     * 验证用户是否可以访问订单（客户或餐厅所有者）
     * 
     * @param orderId 订单 ID
     * @param email 用户邮箱
     * @throws ResourceNotFoundException 订单不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户无权访问订单
     */
    public void verifyOrderAccess(Long orderId, String email) {
        log.debug("验证订单访问权限: orderId={}, email={}", orderId, email);
        
        try {
            // 尝试验证是订单客户
            verifyOrderCustomer(orderId, email);
            log.debug("用户 {} 是订单 {} 的客户", email, orderId);
        } catch (AccessDeniedException e1) {
            try {
                // 尝试验证是餐厅所有者
                verifyOrderRestaurantOwner(orderId, email);
                log.debug("用户 {} 是订单 {} 所属餐厅的所有者", email, orderId);
            } catch (AccessDeniedException e2) {
                // 两者都不是，拒绝访问
                log.warn("访问被拒绝: 用户 {} 无权访问订单 {}", email, orderId);
                throw new AccessDeniedException("您没有权限访问此订单");
            }
        }
    }
    
    /**
     * 获取用户并验证其为 CUSTOMER 角色
     * 
     * @param email 用户邮箱
     * @return 用户实体
     * @throws UsernameNotFoundException 用户不存在
     */
    public User getUserAndVerifyCustomer(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + email));
        
        // 角色验证由 @PreAuthorize 完成
        log.debug("获取客户用户: email={}, role={}", email, user.getRole());
        
        return user;
    }
    
    // ============ 菜单项权限验证 ============
    
    /**
     * 验证菜单项所有权（通过餐厅所有权）
     * 
     * @param menuItemId 菜单项 ID
     * @param email 用户邮箱（餐厅老板）
     * @throws ResourceNotFoundException 菜单项不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户不是餐厅所有者
     */
    public void verifyMenuItemOwnership(Long menuItemId, String email) {
        log.debug("验证菜单项所有权: menuItemId={}, email={}", menuItemId, email);
        
        // 查找菜单项
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
            .orElseThrow(() -> {
                log.warn("菜单项不存在: id={}", menuItemId);
                return new ResourceNotFoundException("菜单项不存在，ID: " + menuItemId);
            });
        
        // 通过餐厅所有权验证
        verifyRestaurantOwnership(menuItem.getRestaurant().getId(), email);
        
        log.debug("菜单项所有权验证成功: 用户 {} 拥有菜单项 {} 所属的餐厅", email, menuItemId);
    }
    
    /**
     * 验证配送员是否被分配到该订单
     * 
     * @param orderId 订单 ID
     * @param email 配送员邮箱
     * @throws ResourceNotFoundException 订单不存在
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 配送员未被分配到该订单
     */
    public void verifyDriverAssignment(Long orderId, String email) {
        log.debug("验证配送员分配: orderId={}, email={}", orderId, email);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("订单不存在: id={}", orderId);
                return new ResourceNotFoundException("订单不存在，ID: " + orderId);
            });
        
        User driver = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证订单是否分配给该配送员
        if (order.getDriver() == null || !order.getDriver().getId().equals(driver.getId())) {
            log.warn("访问被拒绝: 配送员 {} 未被分配到订单 {}", email, orderId);
            throw new AccessDeniedException("您未被分配到此订单");
        }
        
        log.debug("配送员分配验证成功: 配送员 {} 被分配到订单 {}", email, orderId);
    }
    
    /**
     * 获取用户并验证是 DRIVER 角色
     * 
     * @param email 用户邮箱
     * @return User 实体
     * @throws UsernameNotFoundException 用户不存在
     * @throws AccessDeniedException 用户不是 DRIVER
     */
    public User getUserAndVerifyDriver(String email) {
        log.debug("验证 DRIVER 角色: email={}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        if (!user.getRole().equals(UserRole.DRIVER)) {
            log.warn("访问被拒绝: 用户 {} 不是 DRIVER 角色，实际角色: {}", email, user.getRole());
            throw new AccessDeniedException("只有配送员可以访问此功能");
        }
        
        log.debug("DRIVER 角色验证成功: email={}", email);
        return user;
    }
    
    // ========================================
    // Review 评价相关权限验证
    // ========================================
    
    /**
     * 验证评价所有权
     * 确保只有评价的创建者才能修改/删除评价
     * 
     * @param reviewId 评价 ID
     * @param email 用户邮箱
     */
    public void verifyReviewOwnership(Long reviewId, String email) {
        log.debug("验证评价所有权: reviewId={}, email={}", reviewId, email);
        
        // 获取评价
        com.shydelivery.doordashsimulator.entity.Review review = 
            reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("评价不存在: reviewId={}", reviewId);
                    return new ResourceNotFoundException("评价不存在，ID: " + reviewId);
                });
        
        // 获取当前用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证评价是否属于该用户
        if (!review.getCustomer().getId().equals(user.getId())) {
            log.warn("访问被拒绝: 评价 {} 不属于用户 {}", reviewId, email);
            throw new AccessDeniedException("您没有权限修改此评价");
        }
        
        log.debug("评价所有权验证成功: reviewId={}, customerId={}", reviewId, user.getId());
    }
    
    /**
     * 验证评价的订单所有权
     * 确保用户只能评价自己的订单
     * 
     * @param orderId 订单 ID
     * @param email 用户邮箱
     * @return 订单对象
     */
    public Order verifyOrderOwnershipForReview(Long orderId, String email) {
        log.debug("验证订单所有权（用于评价）: orderId={}, email={}", orderId, email);
        
        // 获取订单
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("订单不存在: orderId={}", orderId);
                return new ResourceNotFoundException("订单不存在，ID: " + orderId);
            });
        
        // 获取当前用户
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("用户不存在: email={}", email);
                return new UsernameNotFoundException("用户不存在: " + email);
            });
        
        // 验证订单是否属于该用户
        if (!order.getCustomer().getId().equals(user.getId())) {
            log.warn("访问被拒绝: 订单 {} 不属于用户 {}", orderId, email);
            throw new AccessDeniedException("您只能评价自己的订单");
        }
        
        log.debug("订单所有权验证成功（用于评价）: orderId={}, customerId={}", orderId, user.getId());
        return order;
    }
}

