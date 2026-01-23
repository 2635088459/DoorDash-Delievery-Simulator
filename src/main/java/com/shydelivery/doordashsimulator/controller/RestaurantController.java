package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.CreateRestaurantRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateRestaurantRequest;
import com.shydelivery.doordashsimulator.dto.response.RestaurantDTO;
import com.shydelivery.doordashsimulator.service.RestaurantService;
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
 * Restaurant Controller - 餐厅管理API
 * 
 * 权限说明：
 * - GET /api/restaurants - 公开访问，所有人可浏览
 * - GET /api/restaurants/{id} - 公开访问，查看餐厅详情
 * - POST /api/restaurants - 需要 RESTAURANT_OWNER 角色
 * - PUT /api/restaurants/{id} - 需要 RESTAURANT_OWNER 角色 + 所有者验证
 * - DELETE /api/restaurants/{id} - 需要 RESTAURANT_OWNER 角色 + 所有者验证
 * - GET /api/restaurants/my - 需要 RESTAURANT_OWNER 角色
 * 
 * @author DoorDash Team
 */
@Slf4j
@RestController
@RequestMapping("/restaurants")  // 注意：context-path 已经是 /api，所以这里不需要 /api 前缀
@RequiredArgsConstructor
public class RestaurantController {
    
    private final RestaurantService restaurantService;
    
    /**
     * 获取所有活跃餐厅
     * ✅ 公开接口 - 所有人可访问
     * 
     * @return 餐厅列表
     */
    @GetMapping
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurants() {
        log.info("📋 获取所有餐厅列表");
        List<RestaurantDTO> restaurants = restaurantService.getAllActiveRestaurants();
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 获取餐厅详情
     * ✅ 公开接口 - 所有人可访问
     * 
     * @param id 餐厅ID
     * @return 餐厅详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurant(@PathVariable Long id) {
        log.info("🔍 获取餐厅详情: id={}", id);
        RestaurantDTO restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurant);
    }
    
    /**
     * 创建餐厅
     * 🔒 权限要求：RESTAURANT_OWNER 角色
     * 
     * @param request 创建请求
     * @param principal 当前登录用户
     * @return 创建的餐厅
     */
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping
    public ResponseEntity<RestaurantDTO> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request,
            Principal principal) {
        
        String ownerEmail = principal.getName();
        log.info("✨ 创建餐厅: name={}, owner={}", request.getName(), ownerEmail);
        
        RestaurantDTO created = restaurantService.createRestaurant(request, ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * 获取我的餐厅列表
     * 🔒 权限要求：RESTAURANT_OWNER 角色
     * 
     * @param principal 当前登录用户
     * @return 用户拥有的所有餐厅
     */
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @GetMapping("/my")
    public ResponseEntity<List<RestaurantDTO>> getMyRestaurants(Principal principal) {
        String ownerEmail = principal.getName();
        log.info("🏪 获取我的餐厅: owner={}", ownerEmail);
        
        List<RestaurantDTO> restaurants = restaurantService.getMyRestaurants(ownerEmail);
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 更新餐厅信息
     * 🔒 权限要求：RESTAURANT_OWNER 角色 + 所有者验证
     * 
     * @param id 餐厅ID
     * @param request 更新请求
     * @param principal 当前登录用户
     * @return 更新后的餐厅
     */
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantDTO> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRestaurantRequest request,
            Principal principal) {
        
        String ownerEmail = principal.getName();
        log.info("✏️ 更新餐厅: id={}, owner={}", id, ownerEmail);
        
        // 权限验证在 Service 层进行
        RestaurantDTO updated = restaurantService.updateRestaurant(id, request, ownerEmail);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 删除餐厅
     * 🔒 权限要求：RESTAURANT_OWNER 角色 + 所有者验证
     * 
     * @param id 餐厅ID
     * @param principal 当前登录用户
     * @return 删除成功消息
     */
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestaurant(
            @PathVariable Long id,
            Principal principal) {
        
        String ownerEmail = principal.getName();
        log.info("🗑️ 删除餐厅: id={}, owner={}", id, ownerEmail);
        
        // 权限验证在 Service 层进行
        restaurantService.deleteRestaurant(id, ownerEmail);
        
        return ResponseEntity.ok().body(new DeleteResponse(
            "餐厅删除成功",
            "餐厅ID " + id + " 已被删除"
        ));
    }
    
    /**
     * 删除响应内部类
     */
    private record DeleteResponse(String message, String detail) {}
}
