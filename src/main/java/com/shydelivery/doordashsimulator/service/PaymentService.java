package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreatePaymentRequest;
import com.shydelivery.doordashsimulator.dto.request.RefundRequest;
import com.shydelivery.doordashsimulator.dto.response.PaymentDTO;
import com.shydelivery.doordashsimulator.entity.*;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.PaymentRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 支付服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    
    /**
     * 创建支付记录
     */
    @Transactional
    public PaymentDTO createPayment(CreatePaymentRequest request, String customerEmail) {
        log.info("创建支付记录: orderId={}, customerEmail={}", request.getOrderId(), customerEmail);
        
        // 查找订单
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("订单不存在: " + request.getOrderId()));
        
        // 查找客户
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + customerEmail));
        
        // 验证订单属于该客户
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("订单不属于当前用户");
        }
        
        // 检查订单是否已有支付记录
        if (paymentRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("订单已存在支付记录");
        }
        
        // 创建支付记录
        Payment payment = Payment.builder()
            .order(order)
            .customer(customer)
            .amount(order.getTotalAmount())
            .paymentMethod(request.getPaymentMethod())
            .status(PaymentStatus.PENDING)
            .cardLastFour(request.getCardLastFour())
            .paymentProvider(request.getPaymentProvider())
            .refundedAmount(BigDecimal.ZERO)
            .notes(request.getNotes())
            .build();
        
        payment = paymentRepository.save(payment);
        log.info("支付记录创建成功: paymentId={}", payment.getId());
        
        return convertToDTO(payment);
    }
    
    /**
     * 处理支付（模拟第三方支付）
     */
    @Transactional
    public PaymentDTO processPayment(Long paymentId, String customerEmail) {
        log.info("处理支付: paymentId={}, customerEmail={}", paymentId, customerEmail);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("支付记录不存在: " + paymentId));
        
        // 验证支付属于该客户
        if (!payment.getCustomer().getEmail().equals(customerEmail)) {
            throw new RuntimeException("无权操作此支付记录");
        }
        
        // 验证支付状态
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("支付状态不正确，当前状态: " + payment.getStatus());
        }
        
        // 更新状态为处理中
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);
        
        // 模拟第三方支付处理（实际应调用支付网关API）
        try {
            String transactionId = simulateThirdPartyPayment(payment);
            payment.markAsCompleted(transactionId);
            payment = paymentRepository.save(payment);
            
            // 更新订单的支付状态为已完成
            updateOrderPaymentStatus(payment.getOrder(), Order.PaymentStatus.COMPLETED);
            
            log.info("支付成功: paymentId={}, transactionId={}", paymentId, transactionId);
        } catch (Exception e) {
            String failureReason = "支付处理失败: " + e.getMessage();
            payment.markAsFailed(failureReason);
            payment = paymentRepository.save(payment);
            
            // 更新订单的支付状态为失败
            updateOrderPaymentStatus(payment.getOrder(), Order.PaymentStatus.FAILED);
            
            log.error("支付失败: paymentId={}, reason={}", paymentId, failureReason);
            throw new RuntimeException(failureReason);
        }
        
        return convertToDTO(payment);
    }
    
    /**
     * 确认支付（管理员操作或webhook回调）
     */
    @Transactional
    public PaymentDTO confirmPayment(Long paymentId, String transactionId) {
        log.info("确认支付: paymentId={}, transactionId={}", paymentId, transactionId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("支付记录不存在: " + paymentId));
        
        payment.markAsCompleted(transactionId);
        payment = paymentRepository.save(payment);
        
        // 更新订单的支付状态为已完成
        updateOrderPaymentStatus(payment.getOrder(), Order.PaymentStatus.COMPLETED);
        
        log.info("支付确认成功: paymentId={}", paymentId);
        return convertToDTO(payment);
    }
    
    /**
     * 支付失败
     */
    @Transactional
    public PaymentDTO failPayment(Long paymentId, String failureReason) {
        log.info("标记支付失败: paymentId={}, reason={}", paymentId, failureReason);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("支付记录不存在: " + paymentId));
        
        payment.markAsFailed(failureReason);
        payment = paymentRepository.save(payment);
        
        log.info("支付失败标记成功: paymentId={}", paymentId);
        return convertToDTO(payment);
    }
    
    /**
     * 退款
     */
    @Transactional
    public PaymentDTO refundPayment(Long paymentId, RefundRequest request, String customerEmail) {
        log.info("处理退款: paymentId={}, amount={}, customerEmail={}", 
            paymentId, request.getAmount(), customerEmail);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("支付记录不存在: " + paymentId));
        
        // 验证支付属于该客户（客户只能退自己的款）
        if (!payment.getCustomer().getEmail().equals(customerEmail)) {
            throw new RuntimeException("无权操作此支付记录");
        }
        
        // 验证是否可以退款
        if (!payment.canRefund()) {
            throw new RuntimeException("该支付不能退款，当前状态: " + payment.getStatus());
        }
        
        // 处理退款
        try {
            payment.processRefund(request.getAmount());
            
            // 更新失败原因字段为退款原因
            if (payment.getFailureReason() == null) {
                payment.setFailureReason("退款原因: " + request.getReason());
            } else {
                payment.setFailureReason(payment.getFailureReason() + " | 退款原因: " + request.getReason());
            }
            
            payment = paymentRepository.save(payment);
            
            // 更新订单的支付状态为已退款
            updateOrderPaymentStatus(payment.getOrder(), Order.PaymentStatus.REFUNDED);
            
            log.info("退款成功: paymentId={}, amount={}, newStatus={}", 
                paymentId, request.getAmount(), payment.getStatus());
        } catch (IllegalArgumentException e) {
            log.error("退款失败: paymentId={}, error={}", paymentId, e.getMessage());
            throw new RuntimeException("退款失败: " + e.getMessage());
        }
        
        return convertToDTO(payment);
    }
    
    /**
     * 根据订单ID查找支付
     */
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByOrderId(Long orderId, String customerEmail) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("订单支付记录不存在: " + orderId));
        
        // 验证权限
        if (!payment.getCustomer().getEmail().equals(customerEmail)) {
            throw new RuntimeException("无权查看此支付记录");
        }
        
        return convertToDTO(payment);
    }
    
    /**
     * 根据支付ID查找支付
     */
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long paymentId, String customerEmail) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("支付记录不存在: " + paymentId));
        
        // 验证权限
        if (!payment.getCustomer().getEmail().equals(customerEmail)) {
            throw new RuntimeException("无权查看此支付记录");
        }
        
        return convertToDTO(payment);
    }
    
    /**
     * 获取客户的支付历史
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentHistory(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + customerEmail));
        
        List<Payment> payments = paymentRepository.findByCustomerOrderByCreatedAtDesc(customer);
        
        return payments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取客户指定状态的支付记录
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentsByStatus(String customerEmail, PaymentStatus status) {
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new RuntimeException("用户不存在: " + customerEmail));
        
        List<Payment> payments = paymentRepository.findByCustomerAndStatusOrderByCreatedAtDesc(customer, status);
        
        return payments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取指定时间范围内的已完成支付（管理员功能）
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getCompletedPaymentsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findCompletedPaymentsBetween(startDate, endDate);
        
        return payments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 模拟第三方支付处理
     */
    private String simulateThirdPartyPayment(Payment payment) {
        // 实际应用中，这里应该调用第三方支付网关API
        // 例如: Stripe, PayPal, Square 等
        
        log.info("模拟第三方支付: paymentId={}, amount={}, method={}", 
            payment.getId(), payment.getAmount(), payment.getPaymentMethod());
        
        // 模拟随机失败（10%概率）
        if (Math.random() < 0.1) {
            throw new RuntimeException("第三方支付网关拒绝");
        }
        
        // 生成模拟交易ID
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        log.info("第三方支付成功: transactionId={}", transactionId);
        return transactionId;
    }
    
    /**
     * 更新订单的支付状态
     */
    private void updateOrderPaymentStatus(Order order, Order.PaymentStatus paymentStatus) {
        try {
            order.setPaymentStatus(paymentStatus);
            orderRepository.save(order);
            log.info("订单支付状态已更新: orderId={}, status={}", order.getId(), paymentStatus);
        } catch (Exception e) {
            log.error("更新订单支付状态失败: orderId={}, error={}", order.getId(), e.getMessage());
            // 不抛出异常，避免影响支付流程
        }
    }
    
    /**
     * 转换为DTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        String customerName = payment.getCustomer().getFirstName() + " " + payment.getCustomer().getLastName();
        
        return PaymentDTO.builder()
            .id(payment.getId())
            .orderId(payment.getOrder().getId())
            .customerId(payment.getCustomer().getId())
            .customerEmail(payment.getCustomer().getEmail())
            .customerName(customerName)
            .amount(payment.getAmount())
            .refundedAmount(payment.getRefundedAmount())
            .refundableAmount(payment.getRefundableAmount())
            .paymentMethod(payment.getPaymentMethod())
            .status(payment.getStatus())
            .transactionId(payment.getTransactionId())
            .paymentProvider(payment.getPaymentProvider())
            .cardLastFour(payment.getCardLastFour())
            .failureReason(payment.getFailureReason())
            .paidAt(payment.getPaidAt())
            .refundedAt(payment.getRefundedAt())
            .notes(payment.getNotes())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .isPaid(payment.isPaid())
            .canRefund(payment.canRefund())
            .build();
    }
}
