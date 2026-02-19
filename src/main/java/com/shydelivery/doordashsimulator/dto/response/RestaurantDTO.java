package com.shydelivery.doordashsimulator.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 餐厅响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
    
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String description;
    private String cuisineType;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String phoneNumber;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String weeklyScheduleJson;
    private String restDaysJson;
    private Boolean isActive;
    private BigDecimal rating;
    private Integer totalReviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
