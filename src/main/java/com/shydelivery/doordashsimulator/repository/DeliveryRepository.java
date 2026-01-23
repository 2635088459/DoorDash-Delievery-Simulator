package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Delivery;
import com.shydelivery.doordashsimulator.entity.DeliveryStatus;
import com.shydelivery.doordashsimulator.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Delivery Repository
 * 配送记录数据访问层
 */
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    
    /**
     * 根据订单ID查找配送记录
     */
    Optional<Delivery> findByOrderId(Long orderId);
    
    /**
     * 根据配送员查找所有配送记录
     */
    List<Delivery> findByDriver(Driver driver);
    
    /**
     * 根据配送员ID查找所有配送记录
     */
    List<Delivery> findByDriverId(Long driverId);
    
    /**
     * 根据配送员ID和状态查找配送记录
     */
    List<Delivery> findByDriverIdAndStatus(Long driverId, DeliveryStatus status);
    
    /**
     * 根据配送员和状态查找配送记录
     */
    List<Delivery> findByDriverAndStatus(Driver driver, DeliveryStatus status);
    
    /**
     * 分页查询配送员的配送历史
     */
    Page<Delivery> findByDriverOrderByCreatedAtDesc(Driver driver, Pageable pageable);
    
    /**
     * 查找配送员在指定时间范围内的配送记录
     */
    @Query("SELECT d FROM Delivery d WHERE d.driver = :driver " +
           "AND d.createdAt BETWEEN :startDate AND :endDate")
    List<Delivery> findByDriverAndDateRange(
        @Param("driver") Driver driver,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 查找配送员当前正在进行的配送
     */
    @Query("SELECT d FROM Delivery d WHERE d.driver.id = :driverId " +
           "AND d.status IN ('ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')")
    Optional<Delivery> findActiveDeliveryByDriver(@Param("driverId") Long driverId);
    
    /**
     * 查找所有未分配的配送
     */
    List<Delivery> findByStatus(DeliveryStatus status);
    
    /**
     * 统计配送员今日完成的配送数量
     */
    @Query(value = "SELECT COUNT(*) FROM deliveries d WHERE d.driver_id = :driverId " +
           "AND d.status = 'DELIVERED' AND DATE(d.delivered_at) = CURRENT_DATE", 
           nativeQuery = true)
    Long countTodayDeliveriesByDriver(@Param("driverId") Long driverId);
    
    /**
     * 查找配送员今日所有配送记录
     */
    @Query(value = "SELECT * FROM deliveries d WHERE d.driver_id = :driverId " +
           "AND DATE(d.created_at) = CURRENT_DATE", 
           nativeQuery = true)
    List<Delivery> findTodayDeliveriesByDriver(@Param("driverId") Long driverId);
    
    /**
     * 查找配送员本周所有配送记录
     */
    @Query(value = "SELECT * FROM deliveries d WHERE d.driver_id = :driverId " +
           "AND EXTRACT(WEEK FROM d.created_at) = EXTRACT(WEEK FROM CURRENT_DATE) " +
           "AND EXTRACT(YEAR FROM d.created_at) = EXTRACT(YEAR FROM CURRENT_DATE)", 
           nativeQuery = true)
    List<Delivery> findWeekDeliveriesByDriver(@Param("driverId") Long driverId);
}
