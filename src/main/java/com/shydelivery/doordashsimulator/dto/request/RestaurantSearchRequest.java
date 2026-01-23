package com.shydelivery.doordashsimulator.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 餐厅搜索请求 DTO
 * 支持多条件搜索和过滤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSearchRequest {
    
    /**
     * 搜索关键词（搜索餐厅名称和描述）
     */
    private String keyword;
    
    /**
     * 餐厅类型
     */
    private String cuisineType;
    
    /**
     * 最低评分
     */
    private Double minRating;
    
    /**
     * 最高评分
     */
    private Double maxRating;
    
    /**
     * 最低配送费
     */
    private BigDecimal minDeliveryFee;
    
    /**
     * 最高配送费
     */
    private BigDecimal maxDeliveryFee;
    
    /**
     * 最小订单金额（起送价）
     */
    private BigDecimal minOrderAmount;
    
    /**
     * 最大订单金额（起送价）
     */
    private BigDecimal maxOrderAmount;
    
    /**
     * 是否只显示营业中的餐厅
     */
    private Boolean openOnly;
    
    /**
     * 排序字段
     * 可选值：rating（评分）、deliveryFee（配送费）、deliveryTime（配送时间）、name（名称）
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
     * 获取排序字段，如果未指定则返回默认值 "rating"
     */
    public String getSortBy() {
        return sortBy != null ? sortBy : "rating";
    }
    
    /**
     * 获取排序方向，如果未指定则返回默认值 "desc"
     */
    public String getSortDirection() {
        return sortDirection != null ? sortDirection : "desc";
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
