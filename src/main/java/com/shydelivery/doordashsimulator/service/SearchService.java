package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.MenuItemSearchRequest;
import com.shydelivery.doordashsimulator.dto.request.RestaurantSearchRequest;
import com.shydelivery.doordashsimulator.dto.response.MenuItemDTO;
import com.shydelivery.doordashsimulator.dto.response.RestaurantDTO;
import com.shydelivery.doordashsimulator.dto.response.SearchResultDTO;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.repository.MenuItemRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 提供餐厅和菜品的搜索、过滤、排序功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {
    
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final ReviewRepository reviewRepository;
    
    /**
     * 综合搜索（同时搜索餐厅和菜品）
     */
    @Transactional(readOnly = true)
    public SearchResultDTO globalSearch(String keyword, Integer page, Integer size) {
        log.info("Global search with keyword: {}", keyword);
        
        page = page != null ? page : 0;
        size = size != null && size > 0 && size <= 100 ? size : 20;
        
        // 搜索餐厅
        RestaurantSearchRequest restaurantRequest = RestaurantSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        Page<Restaurant> restaurantPage = searchRestaurants(restaurantRequest);
        
        // 搜索菜品
        MenuItemSearchRequest menuItemRequest = MenuItemSearchRequest.builder()
                .keyword(keyword)
                .availableOnly(true)
                .page(page)
                .size(size)
                .build();
        Page<MenuItem> menuItemPage = searchMenuItems(menuItemRequest);
        
        // 转换为 DTO
        List<RestaurantDTO> restaurantDTOs = restaurantPage.getContent().stream()
                .map(this::convertToRestaurantDTO)
                .collect(Collectors.toList());
        
        List<MenuItemDTO> menuItemDTOs = menuItemPage.getContent().stream()
                .map(this::convertToMenuItemDTO)
                .collect(Collectors.toList());
        
        return SearchResultDTO.builder()
                .restaurants(restaurantDTOs)
                .menuItems(menuItemDTOs)
                .totalRestaurants(restaurantPage.getTotalElements())
                .totalMenuItems(menuItemPage.getTotalElements())
                .currentPage(page)
                .pageSize(size)
                .keyword(keyword)
                .build();
    }
    
    /**
     * 搜索餐厅（支持多条件过滤和排序）
     */
    @Transactional(readOnly = true)
    public Page<Restaurant> searchRestaurants(RestaurantSearchRequest request) {
        log.info("Searching restaurants with request: {}", request);
        
        Pageable pageable = createPageable(
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDirection()
        );
        
        // 构建查询条件
        String keyword = request.getKeyword() != null ? request.getKeyword().toLowerCase() : null;
        String cuisineType = request.getCuisineType();
        Double minRating = request.getMinRating();
        Double maxRating = request.getMaxRating();
        BigDecimal minDeliveryFee = request.getMinDeliveryFee();
        BigDecimal maxDeliveryFee = request.getMaxDeliveryFee();
        BigDecimal minOrderAmount = request.getMinOrderAmount();
        BigDecimal maxOrderAmount = request.getMaxOrderAmount();
        Boolean openOnly = request.getOpenOnly();
        
        // 调用 Repository 的搜索方法
        return restaurantRepository.searchRestaurants(
                keyword,
                cuisineType,
                minRating,
                maxRating,
                minDeliveryFee,
                maxDeliveryFee,
                minOrderAmount,
                maxOrderAmount,
                openOnly,
                pageable
        );
    }
    
    /**
     * 搜索菜品（支持多条件过滤和排序）
     */
    @Transactional(readOnly = true)
    public Page<MenuItem> searchMenuItems(MenuItemSearchRequest request) {
        log.info("Searching menu items with request: {}", request);
        
        Pageable pageable = createPageable(
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortDirection()
        );
        
        // 构建查询条件
        String keyword = request.getKeyword() != null ? request.getKeyword().toLowerCase() : null;
        Long restaurantId = request.getRestaurantId();
        String category = request.getCategory();
        BigDecimal minPrice = request.getMinPrice();
        BigDecimal maxPrice = request.getMaxPrice();
        Boolean availableOnly = request.getAvailableOnly();
        
        // 调用 Repository 的搜索方法
        return menuItemRepository.searchMenuItems(
                keyword,
                restaurantId,
                category,
                minPrice,
                maxPrice,
                availableOnly,
                pageable
        );
    }
    
    /**
     * 按评分搜索餐厅
     */
    @Transactional(readOnly = true)
    public List<RestaurantDTO> searchByRating(Double minRating, Integer limit) {
        log.info("Searching restaurants with min rating: {}", minRating);
        
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 10, Sort.by("id").descending());
        
        List<Restaurant> restaurants = restaurantRepository.searchRestaurants(
                null, null, minRating, null, null, null, null, null, null, pageable
        ).getContent();
        
        return restaurants.stream()
                .map(this::convertToRestaurantDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 按价格区间搜索菜品
     */
    @Transactional(readOnly = true)
    public List<MenuItemDTO> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Integer limit) {
        log.info("Searching menu items with price range: {} - {}", minPrice, maxPrice);
        
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 10, Sort.by("price").ascending());
        
        List<MenuItem> menuItems = menuItemRepository.searchMenuItems(
                null, null, null, minPrice, maxPrice, true, pageable
        ).getContent();
        
        return menuItems.stream()
                .map(this::convertToMenuItemDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取热门餐厅（按评分和评价数量）
     */
    @Transactional(readOnly = true)
    public List<RestaurantDTO> getPopularRestaurants(Integer limit) {
        log.info("Getting popular restaurants, limit: {}", limit);
        
        Pageable pageable = PageRequest.of(0, limit != null ? limit : 10);
        List<Restaurant> restaurants = restaurantRepository.findAll(pageable).getContent();
        
        // 按评价数量和评分排序
        restaurants.sort((r1, r2) -> {
            Long count1 = reviewRepository.countReviewsByRestaurant(r1.getId());
            Long count2 = reviewRepository.countReviewsByRestaurant(r2.getId());
            int countCompare = count2.compareTo(count1);
            if (countCompare != 0) return countCompare;
            
            BigDecimal rating1 = reviewRepository.calculateAverageRatingForRestaurant(r1.getId());
            BigDecimal rating2 = reviewRepository.calculateAverageRatingForRestaurant(r2.getId());
            rating1 = rating1 != null ? rating1 : BigDecimal.ZERO;
            rating2 = rating2 != null ? rating2 : BigDecimal.ZERO;
            return rating2.compareTo(rating1);
        });
        
        return restaurants.stream()
                .limit(limit != null ? limit : 10)
                .map(this::convertToRestaurantDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建分页和排序对象
     */
    private Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
        page = page != null ? page : 0;
        size = size != null && size > 0 && size <= 100 ? size : 20;
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Sort sort = Sort.by(direction, sortBy != null ? sortBy : "id");
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * 转换餐厅实体为 DTO
     */
    private RestaurantDTO convertToRestaurantDTO(Restaurant restaurant) {
        // 计算平均评分（ReviewRepository 返回 BigDecimal）
        BigDecimal avgRating = reviewRepository.calculateAverageRatingForRestaurant(restaurant.getId());
        if (avgRating == null) {
            avgRating = BigDecimal.ZERO;
        }
        Long reviewCount = reviewRepository.countReviewsByRestaurant(restaurant.getId());
        
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
                .rating(avgRating)
                .totalReviews(reviewCount != null ? reviewCount.intValue() : 0)
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }
    
    /**
     * 转换菜品实体为 DTO
     */
    private MenuItemDTO convertToMenuItemDTO(MenuItem menuItem) {
        return MenuItemDTO.builder()
                .id(menuItem.getId())
                .restaurantId(menuItem.getRestaurant().getId())
                .restaurantName(menuItem.getRestaurant().getName())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .category(menuItem.getCategory())
                .imageUrl(menuItem.getImageUrl())
                .isAvailable(menuItem.getIsAvailable())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }
}
