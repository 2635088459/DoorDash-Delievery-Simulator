package com.shydelivery.doordashsimulator.controller;

import com.shydelivery.doordashsimulator.dto.request.CreatePaymentRequest;
import com.shydelivery.doordashsimulator.dto.request.RefundRequest;
import com.shydelivery.doordashsimulator.dto.response.PaymentDTO;
import com.shydelivery.doordashsimulator.entity.PaymentStatus;
import com.shydelivery.doordashsimulator.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付控制器
 * 提供支付相关的REST API端点
 * 
 * 注意：application.yml 中配置了 context-path: /api
 * 所以这里的 @RequestMapping 不需要 /api 前缀
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 创建支付记录
     * POST /api/payments
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        try {
            log.info("创建支付记录: orderId={}, user={}", request.getOrderId(), authentication.getName());
            PaymentDTO payment = paymentService.createPayment(request, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (RuntimeException e) {
            log.error("创建支付失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 处理支付（模拟支付流程）
     * POST /api/payments/{id}/process
     */
    @PostMapping("/{id}/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentDTO> processPayment(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            log.info("处理支付: paymentId={}, user={}", id, authentication.getName());
            PaymentDTO payment = paymentService.processPayment(id, authentication.getName());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("处理支付失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 确认支付（管理员或系统操作）
     * POST /api/payments/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDTO> confirmPayment(
            @PathVariable Long id,
            @RequestParam String transactionId) {
        try {
            log.info("确认支付: paymentId={}, transactionId={}", id, transactionId);
            PaymentDTO payment = paymentService.confirmPayment(id, transactionId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("确认支付失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 标记支付失败
     * POST /api/payments/{id}/fail
     */
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentDTO> failPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            log.info("标记支付失败: paymentId={}, reason={}", id, reason);
            PaymentDTO payment = paymentService.failPayment(id, reason);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("标记支付失败操作失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 申请退款
     * POST /api/payments/{id}/refund
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentDTO> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        try {
            log.info("申请退款: paymentId={}, amount={}, user={}", 
                id, request.getAmount(), authentication.getName());
            PaymentDTO payment = paymentService.refundPayment(id, request, authentication.getName());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("退款失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据支付ID查询支付详情
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PaymentDTO> getPaymentById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            PaymentDTO payment = paymentService.getPaymentById(id, authentication.getName());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("查询支付失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
    
    /**
     * 根据订单ID查询支付
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PaymentDTO> getPaymentByOrderId(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            PaymentDTO payment = paymentService.getPaymentByOrderId(orderId, authentication.getName());
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            log.error("查询订单支付失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * 获取我的支付历史
     * GET /api/payments/my-payments
     */
    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentDTO>> getMyPayments(Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            List<PaymentDTO> payments = paymentService.getPaymentHistory(customerEmail);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            log.error("查询支付历史失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据状态筛选我的支付记录
     * GET /api/payments/my-payments/status/{status}
     */
    @GetMapping("/my-payments/status/{status}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentDTO>> getMyPaymentsByStatus(
            @PathVariable PaymentStatus status,
            Authentication authentication) {
        try {
            List<PaymentDTO> payments = paymentService.getPaymentsByStatus(authentication.getName(), status);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            log.error("查询指定状态的支付失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 获取指定时间范围内的已完成支付（管理员功能）
     * GET /api/payments/completed
     */
    @GetMapping("/completed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getCompletedPayments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<PaymentDTO> payments = paymentService.getCompletedPaymentsBetween(startDate, endDate);
            return ResponseEntity.ok(payments);
        } catch (RuntimeException e) {
            log.error("查询已完成支付失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
