package com.shydelivery.doordashsimulator.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 菜品搜索请求 DTO
 * 支持多条件搜索和过滤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemSearchRequest {
    
    /**
     * 搜索关键词（搜索菜品名称和描述）
     */
    private String keyword;
    
    /**
     * 餐厅ID（在特定餐厅内搜索）
     */
    private Long restaurantId;
    
    /**
     * 菜品分类
     */
    private String category;
    
    /**
     * 最低价格
     */
    private BigDecimal minPrice;
    
    /**
     * 最高价格
     */
    private BigDecimal maxPrice;
    
    /**
     * 是否只显示可用的菜品
     */
    private Boolean availableOnly;
    
    /**
     * 是否只显示热门菜品
     */
    private Boolean popularOnly;
    
    /**
     * 排序字段
     * 可选值：price（价格）、name（名称）、popularity（人气）
     */
    private String sortBy;
    
    /**
     * 排序方向
     * 可选值：asc（升序）、desc（降序）
     */
    private String sortDirection;
    
    /**
     * 页码（从0开始）
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 获取排序字段，如果未指定则返回默认值 "name"
     */
    public String getSortBy() {
        return sortBy != null ? sortBy : "name";
    }
    
    /**
     * 获取排序方向，如果未指定则返回默认值 "asc"
     */
    public String getSortDirection() {
        return sortDirection != null ? sortDirection : "asc";
    }
    
    /**
     * 获取页码，如果未指定则返回默认值 0
     */
    public Integer getPage() {
        return page != null ? page : 0;
    }
    
    /**
     * 获取每页大小，如果未指定则返回默认值 20
     */
    public Integer getSize() {
        return size != null && size > 0 && size <= 100 ? size : 20;
    }
}
