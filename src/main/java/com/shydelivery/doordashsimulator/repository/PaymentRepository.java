package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Payment;
import com.shydelivery.doordashsimulator.entity.PaymentStatus;
import com.shydelivery.doordashsimulator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付数据访问层
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 根据订单查找支付
     */
    Optional<Payment> findByOrder(Order order);
    
    /**
     * 根据订单ID查找支付
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findByOrderId(@Param("orderId") Long orderId);
    
    /**
     * 根据客户查找所有支付记录（按创建时间降序）
     */
    List<Payment> findByCustomerOrderByCreatedAtDesc(User customer);
    
    /**
     * 根据客户ID查找所有支付记录
     */
    @Query("SELECT p FROM Payment p WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);
    
    /**
     * 根据支付状态查找
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * 根据交易ID查找
     */
    Optional<Payment> findByTransactionId(String transactionId);
    
    /**
     * 根据客户和状态查找（按创建时间降序）
     */
    List<Payment> findByCustomerAndStatusOrderByCreatedAtDesc(User customer, PaymentStatus status);
    
    /**
     * 查找指定时间范围内已完成的支付
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :startDate AND :endDate ORDER BY p.paidAt DESC")
    List<Payment> findCompletedPaymentsBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 查找指定时间范围内的退款记录
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN ('REFUNDED', 'PARTIALLY_REFUNDED') AND p.refundedAt BETWEEN :startDate AND :endDate ORDER BY p.refundedAt DESC")
    List<Payment> findRefundsBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 统计客户的总支付金额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.customer.id = :customerId AND p.status = 'COMPLETED'")
    Double getTotalAmountByCustomer(@Param("customerId") Long customerId);
    
    /**
     * 统计客户的总退款金额
     */
    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM Payment p WHERE p.customer.id = :customerId AND p.status IN ('REFUNDED', 'PARTIALLY_REFUNDED')")
    Double getTotalRefundedAmountByCustomer(@Param("customerId") Long customerId);
    
    /**
     * 统计指定时间范围内的支付数量（按状态）
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusBetween(
        @Param("status") PaymentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
