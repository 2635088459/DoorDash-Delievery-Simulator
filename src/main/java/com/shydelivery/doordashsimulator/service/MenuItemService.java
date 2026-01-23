package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreateMenuItemRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateMenuItemRequest;
import com.shydelivery.doordashsimulator.dto.response.MenuItemDTO;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.MenuItemRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MenuItem Service - 菜单项业务逻辑
 * 
 * RBAC 权限模型:
 * - PUBLIC: 查看餐厅的可用菜单项
 * - RESTAURANT_OWNER: 管理自己餐厅的菜单项(CRUD)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemService {
    
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final AuthorizationService authorizationService;
    
    /**
     * 获取餐厅的所有可用菜单项（公开接口）
     * 
     * @param restaurantId 餐厅 ID
     * @return 可用菜单项列表
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getAvailableMenuItems(Long restaurantId) {
        log.info("获取餐厅可用菜单: restaurantId={}", restaurantId);
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId));
        
        return menuItemRepository.findByRestaurantAndIsAvailableTrue(restaurant)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取餐厅的所有菜单项（RESTAURANT_OWNER 角色）
     * 
     * @param restaurantId 餐厅 ID
     * @param ownerEmail 餐厅所有者邮箱
     * @return 所有菜单项列表
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> getAllMenuItems(Long restaurantId, String ownerEmail) {
        log.info("获取餐厅所有菜单: restaurantId={}, owner={}", restaurantId, ownerEmail);
        
        // 验证餐厅所有权
        authorizationService.verifyRestaurantOwnership(restaurantId, ownerEmail);
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId));
        
        return menuItemRepository.findByRestaurant(restaurant)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据 ID 获取菜单项详情（公开接口）
     * 
     * @param id 菜单项 ID
     * @return 菜单项 DTO
     */
    @Transactional(readOnly = true)
    public MenuItemDTO getMenuItemById(Long id) {
        log.info("获取菜单项详情: id={}", id);
        
        MenuItem menuItem = menuItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("菜单项不存在，ID: " + id));
        
        return convertToDTO(menuItem);
    }
    
    /**
     * 创建菜单项（RESTAURANT_OWNER 角色）
     * 
     * @param request 创建请求
     * @param ownerEmail 餐厅所有者邮箱
     * @return 创建的菜单项 DTO
     */
    @Transactional
    public MenuItemDTO createMenuItem(CreateMenuItemRequest request, String ownerEmail) {
        log.info("创建菜单项: restaurant={}, name={}, owner={}", 
                request.getRestaurantId(), request.getName(), ownerEmail);
        
        // 验证餐厅所有权
        authorizationService.verifyRestaurantOwnership(request.getRestaurantId(), ownerEmail);
        
        // 获取餐厅
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + request.getRestaurantId()));
        
        // 创建菜单项
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setCategory(request.getCategory());
        menuItem.setPrice(request.getPrice());
        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);
        menuItem.setIsVegetarian(request.getIsVegetarian() != null ? request.getIsVegetarian() : false);
        menuItem.setIsVegan(request.getIsVegan() != null ? request.getIsVegan() : false);
        menuItem.setSpiceLevel(request.getSpicyLevel() != null ? request.getSpicyLevel() : 0);
        
        // 保存
        MenuItem saved = menuItemRepository.save(menuItem);
        log.info("菜单项创建成功: id={}, name={}", saved.getId(), saved.getName());
        
        return convertToDTO(saved);
    }
    
    /**
     * 更新菜单项（RESTAURANT_OWNER 角色）
     * 
     * @param id 菜单项 ID
     * @param request 更新请求
     * @param ownerEmail 餐厅所有者邮箱
     * @return 更新后的菜单项 DTO
     */
    @Transactional
    public MenuItemDTO updateMenuItem(Long id, UpdateMenuItemRequest request, String ownerEmail) {
        log.info("更新菜单项: id={}, owner={}", id, ownerEmail);
        
        // 验证菜单项所有权
        authorizationService.verifyMenuItemOwnership(id, ownerEmail);
        
        // 获取菜单项
        MenuItem menuItem = menuItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("菜单项不存在，ID: " + id));
        
        // 更新字段
        if (request.getName() != null) {
            menuItem.setName(request.getName());
        }
        if (request.getDescription() != null) {
            menuItem.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            menuItem.setCategory(request.getCategory());
        }
        if (request.getPrice() != null) {
            menuItem.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            menuItem.setImageUrl(request.getImageUrl());
        }
        if (request.getIsAvailable() != null) {
            menuItem.setIsAvailable(request.getIsAvailable());
        }
        if (request.getIsVegetarian() != null) {
            menuItem.setIsVegetarian(request.getIsVegetarian());
        }
        if (request.getIsVegan() != null) {
            menuItem.setIsVegan(request.getIsVegan());
        }
        if (request.getSpicyLevel() != null) {
            menuItem.setSpiceLevel(request.getSpicyLevel());
        }
        
        // 保存更新
        MenuItem updated = menuItemRepository.save(menuItem);
        log.info("菜单项更新成功: id={}", updated.getId());
        
        return convertToDTO(updated);
    }
    
    /**
     * 删除菜单项（RESTAURANT_OWNER 角色）
     * 
     * @param id 菜单项 ID
     * @param ownerEmail 餐厅所有者邮箱
     */
    @Transactional
    public void deleteMenuItem(Long id, String ownerEmail) {
        log.info("删除菜单项: id={}, owner={}", id, ownerEmail);
        
        // 验证菜单项所有权
        authorizationService.verifyMenuItemOwnership(id, ownerEmail);
        
        // 删除菜单项
        menuItemRepository.deleteById(id);
        log.info("菜单项删除成功: id={}", id);
    }
    
    /**
     * 将 MenuItem 实体转换为 DTO
     */
    private MenuItemDTO convertToDTO(MenuItem menuItem) {
        return MenuItemDTO.builder()
                .id(menuItem.getId())
                .restaurantId(menuItem.getRestaurant().getId())
                .restaurantName(menuItem.getRestaurant().getName())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .category(menuItem.getCategory())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .isAvailable(menuItem.getIsAvailable())
                .isVegetarian(menuItem.getIsVegetarian())
                .isVegan(menuItem.getIsVegan())
                .spicyLevel(menuItem.getSpiceLevel())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }
}
