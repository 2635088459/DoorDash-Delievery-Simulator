package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.AddFavoriteRequest;
import com.shydelivery.doordashsimulator.dto.response.FavoriteMenuItemDTO;
import com.shydelivery.doordashsimulator.dto.response.FavoriteRestaurantDTO;
import com.shydelivery.doordashsimulator.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 收藏控制器
 * 提供餐厅和菜品收藏管理接口
 */
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    
    // ==================== 餐厅收藏 ====================
    
    /**
     * 收藏餐厅
     */
    @PostMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteRestaurantDTO> addFavoriteRestaurant(
            @PathVariable Long restaurantId,
            @Valid @RequestBody(required = false) AddFavoriteRequest request,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Adding restaurant {} to favorites - customer: {}", restaurantId, customerEmail);
        
        FavoriteRestaurantDTO favorite = favoriteService.addFavoriteRestaurant(restaurantId, request, customerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }
    
    /**
     * 取消收藏餐厅
     */
    @DeleteMapping("/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeFavoriteRestaurant(
            @PathVariable Long restaurantId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Removing restaurant {} from favorites - customer: {}", restaurantId, customerEmail);
        
        favoriteService.removeFavoriteRestaurant(restaurantId, customerEmail);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取用户收藏的所有餐厅
     */
    @GetMapping("/restaurants")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FavoriteRestaurantDTO>> getFavoriteRestaurants(Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting favorite restaurants - customer: {}", customerEmail);
        
        List<FavoriteRestaurantDTO> favorites = favoriteService.getFavoriteRestaurants(customerEmail);
        return ResponseEntity.ok(favorites);
    }
    
    /**
     * 检查餐厅是否已收藏
     */
    @GetMapping("/restaurants/{restaurantId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteStatusResponse> checkRestaurantFavoriteStatus(
            @PathVariable Long restaurantId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        boolean isFavorited = favoriteService.isRestaurantFavorited(restaurantId, customerEmail);
        
        return ResponseEntity.ok(new FavoriteStatusResponse(isFavorited));
    }
    
    // ==================== 菜品收藏 ====================
    
    /**
     * 收藏菜品
     */
    @PostMapping("/menu-items/{menuItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteMenuItemDTO> addFavoriteMenuItem(
            @PathVariable Long menuItemId,
            @Valid @RequestBody(required = false) AddFavoriteRequest request,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Adding menu item {} to favorites - customer: {}", menuItemId, customerEmail);
        
        FavoriteMenuItemDTO favorite = favoriteService.addFavoriteMenuItem(menuItemId, request, customerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }
    
    /**
     * 取消收藏菜品
     */
    @DeleteMapping("/menu-items/{menuItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> removeFavoriteMenuItem(
            @PathVariable Long menuItemId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Removing menu item {} from favorites - customer: {}", menuItemId, customerEmail);
        
        favoriteService.removeFavoriteMenuItem(menuItemId, customerEmail);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取用户收藏的所有菜品
     */
    @GetMapping("/menu-items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FavoriteMenuItemDTO>> getFavoriteMenuItems(Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting favorite menu items - customer: {}", customerEmail);
        
        List<FavoriteMenuItemDTO> favorites = favoriteService.getFavoriteMenuItems(customerEmail);
        return ResponseEntity.ok(favorites);
    }
    
    /**
     * 获取用户在特定餐厅收藏的菜品
     */
    @GetMapping("/menu-items/restaurant/{restaurantId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FavoriteMenuItemDTO>> getFavoriteMenuItemsByRestaurant(
            @PathVariable Long restaurantId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting favorite menu items for restaurant {} - customer: {}", restaurantId, customerEmail);
        
        List<FavoriteMenuItemDTO> favorites = favoriteService.getFavoriteMenuItemsByRestaurant(restaurantId, customerEmail);
        return ResponseEntity.ok(favorites);
    }
    
    /**
     * 检查菜品是否已收藏
     */
    @GetMapping("/menu-items/{menuItemId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteStatusResponse> checkMenuItemFavoriteStatus(
            @PathVariable Long menuItemId,
            Principal principal) {
        
        String customerEmail = principal.getName();
        boolean isFavorited = favoriteService.isMenuItemFavorited(menuItemId, customerEmail);
        
        return ResponseEntity.ok(new FavoriteStatusResponse(isFavorited));
    }
    
    // ==================== 统计信息 ====================
    
    /**
     * 获取用户收藏统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FavoriteService.FavoriteStatsDTO> getFavoriteStats(Principal principal) {
        
        String customerEmail = principal.getName();
        log.info("Getting favorite stats - customer: {}", customerEmail);
        
        FavoriteService.FavoriteStatsDTO stats = favoriteService.getFavoriteStats(customerEmail);
        return ResponseEntity.ok(stats);
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 收藏状态响应
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class FavoriteStatusResponse {
        private boolean isFavorited;
    }
}
