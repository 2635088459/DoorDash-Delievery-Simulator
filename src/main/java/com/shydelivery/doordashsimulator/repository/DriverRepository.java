package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Driver;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Driver Repository
 * 配送员数据访问层
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    /**
     * 根据用户查找配送员
     */
    Optional<Driver> findByUser(User user);
    
    /**
     * 根据用户ID查找配送员
     */
    Optional<Driver> findByUserId(Long userId);
    
    /**
     * 检查用户是否已经是配送员
     */
    boolean existsByUser(User user);
    
    /**
     * 检查驾驶证号是否已存在
     */
    boolean existsByLicenseNumber(String licenseNumber);
    
    /**
     * 查找所有可用（在线）的配送员
     */
    List<Driver> findByIsAvailableTrue();
    
    /**
     * 查找附近的可用配送员（简单实现 - 实际项目中应使用 PostGIS）
     * 
     * 注意：这是一个简化版本，只检查可用性
     * 真实项目中应该使用 PostGIS 的地理位置查询功能
     */
    @Query("SELECT d FROM Driver d WHERE d.isAvailable = true " +
           "AND d.currentLatitude IS NOT NULL " +
           "AND d.currentLongitude IS NOT NULL")
    List<Driver> findAvailableDriversWithLocation();
    
    /**
     * 查找评分最高的配送员（TOP N）
     */
    @Query("SELECT d FROM Driver d ORDER BY d.rating DESC")
    List<Driver> findTopRatedDrivers();
    
    /**
     * 查找完成配送最多的配送员（TOP N）
     */
    @Query("SELECT d FROM Driver d ORDER BY d.totalDeliveries DESC")
    List<Driver> findTopDriversByDeliveries();
    
    /**
     * 统计在线配送员数量
     */
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.isAvailable = true")
    Long countOnlineDrivers();
}
