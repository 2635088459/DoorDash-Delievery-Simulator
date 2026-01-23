package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.AddFavoriteRequest;
import com.shydelivery.doordashsimulator.dto.response.FavoriteMenuItemDTO;
import com.shydelivery.doordashsimulator.dto.response.FavoriteRestaurantDTO;
import com.shydelivery.doordashsimulator.entity.*;
import com.shydelivery.doordashsimulator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务
 * 提供餐厅和菜品收藏功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteService {
    
    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final FavoriteMenuItemRepository favoriteMenuItemRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    
    // ==================== 餐厅收藏 ====================
    
    /**
     * 收藏餐厅
     */
    @Transactional
    public FavoriteRestaurantDTO addFavoriteRestaurant(Long restaurantId, AddFavoriteRequest request, String customerEmail) {
        log.info("Adding restaurant {} to favorites for user {}", restaurantId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("餐厅不存在，ID: " + restaurantId));
        
        // 检查是否已收藏
        if (favoriteRestaurantRepository.existsByUserAndRestaurant(customer, restaurant)) {
            throw new IllegalStateException("您已收藏过该餐厅");
        }
        
        FavoriteRestaurant favorite = FavoriteRestaurant.builder()
                .user(customer)
                .restaurant(restaurant)
                .note(request != null ? request.getNote() : null)
                .build();
        
        favorite = favoriteRestaurantRepository.save(favorite);
        log.info("Restaurant {} added to favorites successfully", restaurantId);
        
        return convertRestaurantToDTO(favorite);
    }
    
    /**
     * 取消收藏餐厅
     */
    @Transactional
    public void removeFavoriteRestaurant(Long restaurantId, String customerEmail) {
        log.info("Removing restaurant {} from favorites for user {}", restaurantId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("餐厅不存在，ID: " + restaurantId));
        
        FavoriteRestaurant favorite = favoriteRestaurantRepository.findByUserAndRestaurant(customer, restaurant)
                .orElseThrow(() -> new IllegalArgumentException("您未收藏该餐厅"));
        
        favoriteRestaurantRepository.delete(favorite);
        log.info("Restaurant {} removed from favorites successfully", restaurantId);
    }
    
    /**
     * 获取用户收藏的所有餐厅
     */
    @Transactional(readOnly = true)
    public List<FavoriteRestaurantDTO> getFavoriteRestaurants(String customerEmail) {
        log.info("Getting favorite restaurants for user {}", customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        List<FavoriteRestaurant> favorites = favoriteRestaurantRepository.findByUserOrderByCreatedAtDesc(customer);
        
        return favorites.stream()
                .map(this::convertRestaurantToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查用户是否收藏了某个餐厅
     */
    @Transactional(readOnly = true)
    public boolean isRestaurantFavorited(Long restaurantId, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElse(null);
        
        if (customer == null) {
            return false;
        }
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElse(null);
        
        if (restaurant == null) {
            return false;
        }
        
        return favoriteRestaurantRepository.existsByUserAndRestaurant(customer, restaurant);
    }
    
    // ==================== 菜品收藏 ====================
    
    /**
     * 收藏菜品
     */
    @Transactional
    public FavoriteMenuItemDTO addFavoriteMenuItem(Long menuItemId, AddFavoriteRequest request, String customerEmail) {
        log.info("Adding menu item {} to favorites for user {}", menuItemId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + menuItemId));
        
        // 检查是否已收藏
        if (favoriteMenuItemRepository.existsByUserAndMenuItem(customer, menuItem)) {
            throw new IllegalStateException("您已收藏过该菜品");
        }
        
        FavoriteMenuItem favorite = FavoriteMenuItem.builder()
                .user(customer)
                .menuItem(menuItem)
                .note(request != null ? request.getNote() : null)
                .build();
        
        favorite = favoriteMenuItemRepository.save(favorite);
        log.info("Menu item {} added to favorites successfully", menuItemId);
        
        return convertMenuItemToDTO(favorite);
    }
    
    /**
     * 取消收藏菜品
     */
    @Transactional
    public void removeFavoriteMenuItem(Long menuItemId, String customerEmail) {
        log.info("Removing menu item {} from favorites for user {}", menuItemId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("菜品不存在，ID: " + menuItemId));
        
        FavoriteMenuItem favorite = favoriteMenuItemRepository.findByUserAndMenuItem(customer, menuItem)
                .orElseThrow(() -> new IllegalArgumentException("您未收藏该菜品"));
        
        favoriteMenuItemRepository.delete(favorite);
        log.info("Menu item {} removed from favorites successfully", menuItemId);
    }
    
    /**
     * 获取用户收藏的所有菜品
     */
    @Transactional(readOnly = true)
    public List<FavoriteMenuItemDTO> getFavoriteMenuItems(String customerEmail) {
        log.info("Getting favorite menu items for user {}", customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        List<FavoriteMenuItem> favorites = favoriteMenuItemRepository.findByUserOrderByCreatedAtDesc(customer);
        
        return favorites.stream()
                .map(this::convertMenuItemToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户在特定餐厅收藏的菜品
     */
    @Transactional(readOnly = true)
    public List<FavoriteMenuItemDTO> getFavoriteMenuItemsByRestaurant(Long restaurantId, String customerEmail) {
        log.info("Getting favorite menu items for restaurant {} and user {}", restaurantId, customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        List<FavoriteMenuItem> favorites = favoriteMenuItemRepository.findByUserAndRestaurantId(customer, restaurantId);
        
        return favorites.stream()
                .map(this::convertMenuItemToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查用户是否收藏了某个菜品
     */
    @Transactional(readOnly = true)
    public boolean isMenuItemFavorited(Long menuItemId, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElse(null);
        
        if (customer == null) {
            return false;
        }
        
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElse(null);
        
        if (menuItem == null) {
            return false;
        }
        
        return favoriteMenuItemRepository.existsByUserAndMenuItem(customer, menuItem);
    }
    
    // ==================== 统计信息 ====================
    
    /**
     * 获取用户收藏统计
     */
    @Transactional(readOnly = true)
    public FavoriteStatsDTO getFavoriteStats(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        long restaurantCount = favoriteRestaurantRepository.countByUser(customer);
        long menuItemCount = favoriteMenuItemRepository.countByUser(customer);
        
        return FavoriteStatsDTO.builder()
                .restaurantCount(restaurantCount)
                .menuItemCount(menuItemCount)
                .totalCount(restaurantCount + menuItemCount)
                .build();
    }
    
    // ==================== DTO 转换 ====================
    
    private FavoriteRestaurantDTO convertRestaurantToDTO(FavoriteRestaurant favorite) {
        Restaurant restaurant = favorite.getRestaurant();
        
        // 构建完整地址
        String fullAddress = String.format("%s, %s, %s %s", 
                restaurant.getStreetAddress(),
                restaurant.getCity(),
                restaurant.getState(),
                restaurant.getZipCode());
        
        return FavoriteRestaurantDTO.builder()
                .favoriteId(favorite.getId())
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .description(restaurant.getDescription())
                .address(fullAddress)
                .cuisineType(restaurant.getCuisineType())
                .deliveryFee(restaurant.getDeliveryFee())
                .minimumOrder(restaurant.getMinimumOrder())
                .imageUrl(null) // Restaurant 实体没有 imageUrl 字段
                .averageRating(restaurant.getRating() != null ? restaurant.getRating().doubleValue() : 0.0)
                .reviewCount(0) // 需要从 Review 表查询，暂时设为0
                .isActive(restaurant.getIsActive())
                .note(favorite.getNote())
                .favoritedAt(favorite.getCreatedAt())
                .build();
    }
    
    private FavoriteMenuItemDTO convertMenuItemToDTO(FavoriteMenuItem favorite) {
        MenuItem menuItem = favorite.getMenuItem();
        
        return FavoriteMenuItemDTO.builder()
                .favoriteId(favorite.getId())
                .menuItemId(menuItem.getId())
                .menuItemName(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .category(menuItem.getCategory())
                .imageUrl(menuItem.getImageUrl())
                .isAvailable(menuItem.getIsAvailable())
                .restaurantId(menuItem.getRestaurant().getId())
                .restaurantName(menuItem.getRestaurant().getName())
                .note(favorite.getNote())
                .favoritedAt(favorite.getCreatedAt())
                .build();
    }
    
    /**
     * 收藏统计 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FavoriteStatsDTO {
        private Long restaurantCount;
        private Long menuItemCount;
        private Long totalCount;
    }
}
