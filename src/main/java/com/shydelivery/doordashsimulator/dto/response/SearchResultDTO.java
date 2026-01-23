package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果响应 DTO
 * 包含餐厅和菜品的搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    
    /**
     * 餐厅搜索结果
     */
    private List<RestaurantDTO> restaurants;
    
    /**
     * 菜品搜索结果
     */
    private List<MenuItemDTO> menuItems;
    
    /**
     * 餐厅总数
     */
    private Long totalRestaurants;
    
    /**
     * 菜品总数
     */
    private Long totalMenuItems;
    
    /**
     * 当前页码
     */
    private Integer currentPage;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 搜索关键词
     */
    private String keyword;
}
