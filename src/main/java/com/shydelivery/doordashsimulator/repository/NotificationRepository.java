package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Notification;
import com.shydelivery.doordashsimulator.entity.Notification.NotificationType;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification Repository - Phase 2
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查找用户的所有通知（按创建时间倒序）
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 查找用户的未读通知
     */
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    /**
     * 查找用户特定类型的通知
     */
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    /**
     * 统计用户的未读通知数量
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    Long countUnreadByUser(@Param("user") User user);

    /**
     * 查找指定订单的所有通知
     */
    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 查找指定配送的所有通知
     */
    List<Notification> findByDeliveryIdOrderByCreatedAtDesc(Long deliveryId);

    /**
     * 批量标记用户的所有通知为已读
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);

    /**
     * 删除过期的已读通知（30天前）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.readAt < :expiryDate")
    int deleteExpiredReadNotifications(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * 查找最近 N 条通知
     */
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);
}
