package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.CreateMenuItemRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateMenuItemRequest;
import com.shydelivery.doordashsimulator.dto.response.MenuItemDTO;
import com.shydelivery.doordashsimulator.service.MenuItemService;
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
 * MenuItem Controller - 菜单项管理 REST API
 * 
 * RBAC 三层防御:
 * 1. JWT 过滤器 - 验证身份
 * 2. @PreAuthorize - 验证角色
 * 3. AuthorizationService - 验证资源所有权
 * 
 * 端点设计:
 * - GET /menu-items/restaurant/{restaurantId}/available - 公开：查看可用菜单
 * - GET /menu-items/restaurant/{restaurantId} - OWNER：查看所有菜单（包括不可用）
 * - GET /menu-items/{id} - 公开：查看菜单项详情
 * - POST /menu-items - RESTAURANT_OWNER：创建菜单项
 * - PUT /menu-items/{id} - RESTAURANT_OWNER：更新菜单项
 * - DELETE /menu-items/{id} - RESTAURANT_OWNER：删除菜单项
 */
@Slf4j
@RestController
@RequestMapping("/menu-items")
@RequiredArgsConstructor
public class MenuItemController {
    
    private final MenuItemService menuItemService;
    
    /**
     * 获取餐厅的可用菜单项（公开接口）
     * 
     * 权限: PUBLIC
     * 使用场景: 顾客浏览餐厅菜单
     * 
     * @param restaurantId 餐厅 ID
     * @return 可用菜单项列表
     */
    @GetMapping("/restaurant/{restaurantId}/available")
    public ResponseEntity<List<MenuItemDTO>> getAvailableMenuItems(@PathVariable Long restaurantId) {
        log.info("获取可用菜单: restaurantId={}", restaurantId);
        List<MenuItemDTO> menuItems = menuItemService.getAvailableMenuItems(restaurantId);
        return ResponseEntity.ok(menuItems);
    }
    
    /**
     * 获取餐厅的所有菜单项（RESTAURANT_OWNER 角色）
     * 
     * 权限: RESTAURANT_OWNER
     * 使用场景: 餐厅老板管理菜单
     * 
     * @param restaurantId 餐厅 ID
     * @param authentication 当前认证用户
     * @return 所有菜单项列表
     */
    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<MenuItemDTO>> getAllMenuItems(
            @PathVariable Long restaurantId,
            Authentication authentication) {
        log.info("获取所有菜单: restaurantId={}, user={}", restaurantId, authentication.getName());
        List<MenuItemDTO> menuItems = menuItemService.getAllMenuItems(restaurantId, authentication.getName());
        return ResponseEntity.ok(menuItems);
    }
    
    /**
     * 根据 ID 获取菜单项详情（公开接口）
     * 
     * 权限: PUBLIC
     * 使用场景: 查看菜单项详细信息
     * 
     * @param id 菜单项 ID
     * @return 菜单项详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemDTO> getMenuItemById(@PathVariable Long id) {
        log.info("获取菜单项详情: id={}", id);
        MenuItemDTO menuItem = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(menuItem);
    }
    
    /**
     * 创建菜单项（RESTAURANT_OWNER 角色）
     * 
     * 权限: RESTAURANT_OWNER
     * 验证: 
     * - 第二层: @PreAuthorize 验证 RESTAURANT_OWNER 角色
     * - 第三层: AuthorizationService 验证餐厅所有权
     * 
     * @param request 创建请求
     * @param authentication 当前认证用户
     * @return 创建的菜单项
     */
    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemDTO> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication) {
        log.info("创建菜单项: restaurant={}, name={}, user={}", 
                request.getRestaurantId(), request.getName(), authentication.getName());
        MenuItemDTO menuItem = menuItemService.createMenuItem(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItem);
    }
    
    /**
     * 更新菜单项（RESTAURANT_OWNER 角色）
     * 
     * 权限: RESTAURANT_OWNER
     * 验证:
     * - 第二层: @PreAuthorize 验证 RESTAURANT_OWNER 角色
     * - 第三层: AuthorizationService 验证菜单项所有权
     * 
     * @param id 菜单项 ID
     * @param request 更新请求
     * @param authentication 当前认证用户
     * @return 更新后的菜单项
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<MenuItemDTO> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMenuItemRequest request,
            Authentication authentication) {
        log.info("更新菜单项: id={}, user={}", id, authentication.getName());
        MenuItemDTO menuItem = menuItemService.updateMenuItem(id, request, authentication.getName());
        return ResponseEntity.ok(menuItem);
    }
    
    /**
     * 删除菜单项（RESTAURANT_OWNER 角色）
     * 
     * 权限: RESTAURANT_OWNER
     * 验证:
     * - 第二层: @PreAuthorize 验证 RESTAURANT_OWNER 角色
     * - 第三层: AuthorizationService 验证菜单项所有权
     * 
     * @param id 菜单项 ID
     * @param authentication 当前认证用户
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("删除菜单项: id={}, user={}", id, authentication.getName());
        menuItemService.deleteMenuItem(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
