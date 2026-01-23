package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreateRestaurantRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateRestaurantRequest;
import com.shydelivery.doordashsimulator.dto.response.RestaurantDTO;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Restaurant Service - 餐厅业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {
    
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    
    /**
     * 获取所有活跃餐厅（公开接口）
     */
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getAllActiveRestaurants() {
        log.info("获取所有活跃餐厅");
        return restaurantRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据 ID 获取餐厅详情（公开接口）
     */
    @Transactional(readOnly = true)
    public RestaurantDTO getRestaurantById(Long id) {
        log.info("获取餐厅详情: id={}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + id));
        return convertToDTO(restaurant);
    }
    
    /**
     * 创建餐厅（仅餐厅老板）
     */
    @Transactional
    public RestaurantDTO createRestaurant(CreateRestaurantRequest request, String ownerEmail) {
        log.info("创建餐厅: name={}, owner={}", request.getName(), ownerEmail);
        
        // 获取餐厅老板用户
        User owner = authorizationService.getUserAndVerifyRestaurantOwner(ownerEmail);
        
        // 创建餐厅实体
        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setCuisineType(request.getCuisineType());
        restaurant.setStreetAddress(request.getStreetAddress());
        restaurant.setCity(request.getCity());
        restaurant.setState(request.getState());
        restaurant.setZipCode(request.getZipCode());
        restaurant.setPhoneNumber(request.getPhoneNumber());
        restaurant.setIsActive(true);
        restaurant.setRating(BigDecimal.ZERO);
        
        // 设置默认营业时间（可以后续通过更新接口修改）
        restaurant.setOpeningTime(java.time.LocalTime.of(9, 0));  // 默认 9:00 AM
        restaurant.setClosingTime(java.time.LocalTime.of(22, 0)); // 默认 10:00 PM
        restaurant.setDeliveryFee(BigDecimal.valueOf(5.00));      // 默认配送费 $5
        restaurant.setMinimumOrder(BigDecimal.valueOf(10.00));    // 默认最低消费 $10
        restaurant.setLatitude(BigDecimal.valueOf(37.7749));      // 默认坐标 (San Francisco)
        restaurant.setLongitude(BigDecimal.valueOf(-122.4194));
        
        // 保存到数据库
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("餐厅创建成功: id={}, name={}", saved.getId(), saved.getName());
        
        return convertToDTO(saved);
    }
    
    /**
     * 更新餐厅信息（仅餐厅所有者）
     */
    @Transactional
    public RestaurantDTO updateRestaurant(Long id, UpdateRestaurantRequest request, String ownerEmail) {
        log.info("更新餐厅: id={}, owner={}", id, ownerEmail);
        
        // 验证所有权
        authorizationService.verifyRestaurantOwnership(id, ownerEmail);
        
        // 获取餐厅
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + id));
        
        // 更新字段
        if (request.getName() != null) {
            restaurant.setName(request.getName());
        }
        if (request.getDescription() != null) {
            restaurant.setDescription(request.getDescription());
        }
        if (request.getCuisineType() != null) {
            restaurant.setCuisineType(request.getCuisineType());
        }
        if (request.getPhoneNumber() != null) {
            restaurant.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            restaurant.setIsActive(request.getIsActive());
        }
        
        // 保存更新
        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("餐厅更新成功: id={}", updated.getId());
        
        return convertToDTO(updated);
    }
    
    /**
     * 删除餐厅（仅餐厅所有者）
     */
    @Transactional
    public void deleteRestaurant(Long id, String ownerEmail) {
        log.info("删除餐厅: id={}, owner={}", id, ownerEmail);
        
        // 验证所有权
        authorizationService.verifyRestaurantOwnership(id, ownerEmail);
        
        // 删除餐厅
        restaurantRepository.deleteById(id);
        log.info("餐厅删除成功: id={}", id);
    }
    
    /**
     * 获取当前用户的所有餐厅（仅餐厅老板）
     */
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getMyRestaurants(String ownerEmail) {
        log.info("获取我的餐厅列表: owner={}", ownerEmail);
        
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + ownerEmail));
        
        return restaurantRepository.findByOwner(owner)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将 Restaurant 实体转换为 DTO
     */
    private RestaurantDTO convertToDTO(Restaurant restaurant) {
        return RestaurantDTO.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwner().getId())
                .ownerName(restaurant.getOwner().getFirstName() + " " + restaurant.getOwner().getLastName())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .cuisineType(restaurant.getCuisineType())
                .streetAddress(restaurant.getStreetAddress())
                .city(restaurant.getCity())
                .state(restaurant.getState())
                .zipCode(restaurant.getZipCode())
                .phoneNumber(restaurant.getPhoneNumber())
                .isActive(restaurant.getIsActive())
                .rating(restaurant.getRating())
                .totalReviews(0) // TODO: 从 reviews 表计算
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }
}
