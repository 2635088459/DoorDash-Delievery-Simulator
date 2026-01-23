package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.MenuItemSearchRequest;
import com.shydelivery.doordashsimulator.dto.request.RestaurantSearchRequest;
import com.shydelivery.doordashsimulator.dto.response.MenuItemDTO;
import com.shydelivery.doordashsimulator.dto.response.RestaurantDTO;
import com.shydelivery.doordashsimulator.dto.response.SearchResultDTO;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 搜索控制器
 * 提供餐厅和菜品的搜索、过滤功能
 * 所有搜索端点都是公开的（不需要认证）
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * 综合搜索（同时搜索餐厅和菜品）
     * GET /search?keyword=pizza&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<SearchResultDTO> globalSearch(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        log.info("API call: Global search - keyword: {}, page: {}, size: {}", keyword, page, size);
        
        SearchResultDTO result = searchService.globalSearch(keyword, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 搜索餐厅（支持多条件过滤）
     * POST /search/restaurants
     */
    @PostMapping("/restaurants")
    public ResponseEntity<Page<Restaurant>> searchRestaurants(
            @RequestBody RestaurantSearchRequest request) {
        log.info("API call: Search restaurants - request: {}", request);
        
        Page<Restaurant> restaurants = searchService.searchRestaurants(request);
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 搜索菜品（支持多条件过滤）
     * POST /search/menu-items
     */
    @PostMapping("/menu-items")
    public ResponseEntity<Page<MenuItem>> searchMenuItems(
            @RequestBody MenuItemSearchRequest request) {
        log.info("API call: Search menu items - request: {}", request);
        
        Page<MenuItem> menuItems = searchService.searchMenuItems(request);
        return ResponseEntity.ok(menuItems);
    }
    
    /**
     * 按评分搜索餐厅
     * GET /search/restaurants/by-rating?minRating=4.0&limit=10
     */
    @GetMapping("/restaurants/by-rating")
    public ResponseEntity<List<RestaurantDTO>> searchByRating(
            @RequestParam Double minRating,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("API call: Search restaurants by rating - min: {}, limit: {}", minRating, limit);
        
        List<RestaurantDTO> restaurants = searchService.searchByRating(minRating, limit);
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 按价格区间搜索菜品
     * GET /search/menu-items/by-price?minPrice=10.00&maxPrice=20.00&limit=10
     */
    @GetMapping("/menu-items/by-price")
    public ResponseEntity<List<MenuItemDTO>> searchByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("API call: Search menu items by price - range: {} - {}, limit: {}", minPrice, maxPrice, limit);
        
        List<MenuItemDTO> menuItems = searchService.searchByPriceRange(minPrice, maxPrice, limit);
        return ResponseEntity.ok(menuItems);
    }
    
    /**
     * 获取热门餐厅
     * GET /search/restaurants/popular?limit=10
     */
    @GetMapping("/restaurants/popular")
    public ResponseEntity<List<RestaurantDTO>> getPopularRestaurants(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("API call: Get popular restaurants - limit: {}", limit);
        
        List<RestaurantDTO> restaurants = searchService.getPopularRestaurants(limit);
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 快速搜索餐厅（仅按名称或描述）
     * GET /search/restaurants/quick?keyword=burger&page=0&size=10
     */
    @GetMapping("/restaurants/quick")
    public ResponseEntity<Page<Restaurant>> quickSearchRestaurants(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("API call: Quick search restaurants - keyword: {}", keyword);
        
        RestaurantSearchRequest request = RestaurantSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();
        
        Page<Restaurant> restaurants = searchService.searchRestaurants(request);
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 快速搜索菜品（仅按名称或描述）
     * GET /search/menu-items/quick?keyword=pizza&page=0&size=10
     */
    @GetMapping("/menu-items/quick")
    public ResponseEntity<Page<MenuItem>> quickSearchMenuItems(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("API call: Quick search menu items - keyword: {}", keyword);
        
        MenuItemSearchRequest request = MenuItemSearchRequest.builder()
                .keyword(keyword)
                .availableOnly(true)
                .page(page)
                .size(size)
                .build();
        
        Page<MenuItem> menuItems = searchService.searchMenuItems(request);
        return ResponseEntity.ok(menuItems);
    }
}
