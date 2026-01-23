package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.dto.request.CreateOrderRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateOrderStatusRequest;
import com.shydelivery.doordashsimulator.dto.response.OrderDTO;
import com.shydelivery.doordashsimulator.dto.response.OrderItemDTO;
import com.shydelivery.doordashsimulator.entity.Address;
import com.shydelivery.doordashsimulator.entity.MenuItem;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Order.PaymentStatus;
import com.shydelivery.doordashsimulator.entity.OrderItem;
import com.shydelivery.doordashsimulator.entity.Payment;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.User;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.AddressRepository;
import com.shydelivery.doordashsimulator.repository.MenuItemRepository;
import com.shydelivery.doordashsimulator.repository.OrderItemRepository;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.PaymentRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.UserRepository;
import com.shydelivery.doordashsimulator.util.DeliveryFeeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service - 订单业务逻辑
 * 
 * RBAC 权限模型:
 * - CUSTOMER: 创建订单, 查看自己的订单, 取消订单(PENDING状态)
 * - RESTAURANT_OWNER: 查看餐厅订单, 更新订单状态(CONFIRMED, PREPARING, READY_FOR_PICKUP)
 * - DRIVER: 查看待配送订单, 更新配送状态(PICKED_UP, IN_TRANSIT, DELIVERED)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AuthorizationService authorizationService;
    private final PaymentRepository paymentRepository;
    
    // Phase 2: 动态配送费计算器
    private final DeliveryFeeCalculator deliveryFeeCalculator;
    private final DriverService driverService;
    private final WeatherService weatherService;
    
    // Phase 2: 实时通知服务
    private final NotificationService notificationService;
    
    // Tax rate: 8.5%
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.085);
    
    /**
     * 创建订单 (CUSTOMER 角色)
     * 
     * @param request 创建订单请求
     * @param customerEmail 客户邮箱
     * @return 创建的订单 DTO
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, String customerEmail) {
        log.info("创建订单: restaurant={}, customer={}", request.getRestaurantId(), customerEmail);
        
        // 获取客户
        User customer = authorizationService.getUserAndVerifyCustomer(customerEmail);
        
        // 获取餐厅
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + request.getRestaurantId()));
        
        // TODO: 验证餐厅营业状态和营业时间
        if (!restaurant.getIsActive()) {
            throw new IllegalStateException("餐厅已关闭，无法下单");
        }
        
        // 创建订单实体
        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        
        // 生成订单号
        order.setOrderNumber(generateOrderNumber());
        
        // 设置初始状态
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSpecialInstructions(request.getSpecialInstructions());
        
        // 获取配送地址
        Address deliveryAddress = null;
        
        // 优先使用内联地址
        if (request.getDeliveryAddress() != null) {
            CreateOrderRequest.DeliveryAddressRequest addressReq = request.getDeliveryAddress();
            Address tempAddress = new Address();
            tempAddress.setUser(customer);
            tempAddress.setStreetAddress(addressReq.getStreetAddress());
            tempAddress.setCity(addressReq.getCity());
            tempAddress.setState(addressReq.getState());
            tempAddress.setZipCode(addressReq.getZipCode());
            // 设置默认坐标（San Francisco）
            tempAddress.setLatitude(BigDecimal.valueOf(37.7749));
            tempAddress.setLongitude(BigDecimal.valueOf(-122.4194));
            tempAddress.setIsDefault(false);
            deliveryAddress = addressRepository.save(tempAddress);
        }
        // 否则使用地址 ID
        else if (request.getDeliveryAddressId() != null) {
            deliveryAddress = addressRepository.findById(request.getDeliveryAddressId())
                .orElse(null);
        }
        
        // 如果找不到指定地址，使用客户的默认地址
        if (deliveryAddress == null) {
            deliveryAddress = addressRepository.findByUserAndIsDefaultTrue(customer)
                .orElse(null);
        }
        
        // 如果还是没有地址，创建一个临时默认地址
        if (deliveryAddress == null) {
            log.warn("客户 {} 没有地址，创建临时默认地址", customer.getEmail());
            Address tempAddress = new Address();
            tempAddress.setUser(customer);
            tempAddress.setStreetAddress("123 Main St");
            tempAddress.setCity("San Francisco");
            tempAddress.setState("CA");
            tempAddress.setZipCode("94102");
            tempAddress.setLatitude(BigDecimal.valueOf(37.7749));
            tempAddress.setLongitude(BigDecimal.valueOf(-122.4194));
            tempAddress.setIsDefault(true);
            deliveryAddress = addressRepository.save(tempAddress);
        }
        
        order.setDeliveryAddress(deliveryAddress);
        
        // 计算订单金额（从菜单项获取真实价格）
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // 获取菜单项
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "菜单项不存在，ID: " + itemRequest.getMenuItemId()));
            
            // 验证菜单项属于该餐厅
            if (!menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException(
                    "菜单项 " + menuItem.getName() + " 不属于餐厅 " + restaurant.getName());
            }
            
            // 验证菜单项可用
            if (!menuItem.getIsAvailable()) {
                throw new IllegalStateException(
                    "菜单项 " + menuItem.getName() + " 暂时不可用");
            }
            
            // 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(menuItem.getPrice());  // 保存当前价格快照
            orderItem.setSpecialRequests(itemRequest.getSpecialInstructions());
            orderItem.calculateSubtotal();  // 计算小计
            
            orderItems.add(orderItem);
            
            // 累加到订单总额
            subtotal = subtotal.add(orderItem.getSubtotal());
        }
        
        // 验证订单至少有一个项
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("订单必须至少包含一个菜品");
        }
        
        // Phase 2: 使用动态配送费计算器
        BigDecimal deliveryFee;
        int estimatedDeliveryMinutes = 45; // 默认预计时间
        
        // 计算餐厅到配送地址的距离
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null &&
            deliveryAddress.getLatitude() != null && deliveryAddress.getLongitude() != null) {
            
            double distance = driverService.calculateDistance(
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                deliveryAddress.getLatitude(),
                deliveryAddress.getLongitude()
            );
            
            log.info("订单配送距离: {} km (餐厅: {}, 配送地址: {})", 
                distance, restaurant.getName(), deliveryAddress.getFullAddress());
            
            // Phase 2: 检查天气状况
            boolean isBadWeather = weatherService.isBadWeather(
                deliveryAddress.getLatitude(),
                deliveryAddress.getLongitude(),
                LocalDateTime.now()
            );
            String weatherDesc = weatherService.getWeatherDescription(
                deliveryAddress.getLatitude(),
                deliveryAddress.getLongitude()
            );
            
            // 使用动态定价算法计算配送费
            LocalDateTime orderTime = LocalDateTime.now();
            deliveryFee = deliveryFeeCalculator.calculateDeliveryFee(
                distance,
                orderTime,
                isBadWeather
            );
            
            // 计算预计配送时间
            estimatedDeliveryMinutes = deliveryFeeCalculator.estimateDeliveryTime(distance);
            
            // Phase 2: 保存配送相关信息
            order.setDeliveryDistanceKm(BigDecimal.valueOf(distance));
            order.setWeatherCondition(weatherDesc);
            order.setBadWeatherSurcharge(isBadWeather);
            order.setPeakHourSurcharge(deliveryFeeCalculator.isPeakHour(orderTime));
            
            log.info("动态配送费计算: 距离={}km, 天气={}, 高峰期={}, 配送费=${}, 预计{}分钟送达", 
                distance, isBadWeather ? "恶劣" : "正常", 
                order.getPeakHourSurcharge() ? "是" : "否",
                deliveryFee, estimatedDeliveryMinutes);
            
        } else {
            // 如果没有坐标信息，使用餐厅的基础配送费
            deliveryFee = restaurant.getDeliveryFee();
            log.warn("餐厅或配送地址缺少坐标信息，使用固定配送费: ${}", deliveryFee);
        }
        
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(deliveryFee).add(tax);
        
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTax(tax);
        order.setTotalAmount(totalAmount);
        
        // 设置预计送达时间（基于动态计算的时间）
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(estimatedDeliveryMinutes));
        
        // 保存订单
        Order saved = orderRepository.save(order);
        
        // 保存订单项
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(saved);  // 确保关联到已保存的订单
            orderItemRepository.save(orderItem);
        }
        
        // 自动创建支付记录
        createPaymentForOrder(saved, customer);
        
        log.info("订单创建成功: orderNumber={}, items={}, totalAmount={}", 
            saved.getOrderNumber(), orderItems.size(), saved.getTotalAmount());
        
        // Phase 2: 发送订单创建通知
        String notificationMessage = String.format(
            "您的订单 %s 已创建成功！餐厅 %s 正在确认订单。预计 %d 分钟后送达。",
            saved.getOrderNumber(),
            restaurant.getName(),
            estimatedDeliveryMinutes
        );
        notificationService.notifyOrderStatusChange(saved, notificationMessage);
        
        return convertToDTO(saved);
    }
    
    /**
     * 获取客户的所有订单 (CUSTOMER 角色)
     * 
     * @param customerEmail 客户邮箱
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getMyOrders(String customerEmail) {
        log.info("获取我的订单: customer={}", customerEmail);
        
        User customer = userRepository.findByEmail(customerEmail)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + customerEmail));
        
        return orderRepository.findByCustomer(customer)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取餐厅的所有订单 (RESTAURANT_OWNER 角色)
     * 
     * @param restaurantId 餐厅 ID
     * @param ownerEmail 餐厅所有者邮箱
     * @return 订单列表
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getRestaurantOrders(Long restaurantId, String ownerEmail) {
        log.info("获取餐厅订单: restaurantId={}, owner={}", restaurantId, ownerEmail);
        
        // 验证餐厅所有权
        authorizationService.verifyRestaurantOwnership(restaurantId, ownerEmail);
        
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在，ID: " + restaurantId));
        
        return orderRepository.findByRestaurant(restaurant)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据 ID 获取订单详情
     * 
     * @param orderId 订单 ID
     * @param userEmail 用户邮箱
     * @return 订单 DTO
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId, String userEmail) {
        log.info("获取订单详情: orderId={}, user={}", orderId, userEmail);
        
        // 验证访问权限（客户或餐厅所有者）
        authorizationService.verifyOrderAccess(orderId, userEmail);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        return convertToDTO(order);
    }
    
    /**
     * 更新订单状态
     * 
     * RBAC 规则:
     * - CUSTOMER 只能取消 PENDING 状态的订单
     * - RESTAURANT_OWNER 可以更新: CONFIRMED, PREPARING, READY_FOR_PICKUP
     * - DRIVER 可以更新: PICKED_UP, IN_TRANSIT, DELIVERED
     * 
     * @param orderId 订单 ID
     * @param request 更新请求
     * @param userEmail 用户邮箱
     * @return 更新后的订单 DTO
     */
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, String userEmail) {
        log.info("更新订单状态: orderId={}, newStatus={}, user={}", orderId, request.getStatus(), userEmail);
        
        // 验证访问权限
        authorizationService.verifyOrderAccess(orderId, userEmail);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证状态转换的合法性
        validateStatusTransition(order.getStatus(), request.getStatus());
        
        // 更新状态
        order.setStatus(request.getStatus());
        
        // 如果是 DELIVERED 状态，设置实际送达时间
        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setActualDelivery(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }
        
        // 如果是 CANCELLED 状态，更新支付状态
        if (request.getStatus() == OrderStatus.CANCELLED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        
        Order updated = orderRepository.save(order);
        log.info("订单状态更新成功: orderId={}, status={}", orderId, updated.getStatus());
        
        return convertToDTO(updated);
    }
    
    /**
     * 取消订单 (CUSTOMER 角色, 仅PENDING状态)
     * 
     * @param orderId 订单 ID
     * @param customerEmail 客户邮箱
     * @return 更新后的订单 DTO
     */
    @Transactional
    public OrderDTO cancelOrder(Long orderId, String customerEmail) {
        log.info("取消订单: orderId={}, customer={}", orderId, customerEmail);
        
        // 验证是订单客户
        authorizationService.verifyOrderCustomer(orderId, customerEmail);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 只有 PENDING 状态的订单可以取消
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("只能取消待处理的订单");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        
        Order cancelled = orderRepository.save(order);
        log.info("订单取消成功: orderId={}", orderId);
        
        return convertToDTO(cancelled);
    }
    
    /**
     * 将 Order 实体转换为 DTO
     */
    private OrderDTO convertToDTO(Order order) {
        // 获取订单项并转换为 DTO
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        List<OrderItemDTO> itemDTOs = orderItems.stream()
                .map(this::convertOrderItemToDTO)
                .collect(Collectors.toList());
        
        // 查找关联的支付记录
        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        Long paymentId = payment != null ? payment.getId() : null;
        String paymentTransactionId = payment != null ? payment.getTransactionId() : null;
        
        return OrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .customerEmail(order.getCustomer().getEmail())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .deliveryAddressId(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getId() : null)
                .deliveryAddressStreet(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getStreetAddress() : null)
                .deliveryAddressCity(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getCity() : null)
                .deliveryAddressState(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getState() : null)
                .deliveryAddressZipCode(order.getDeliveryAddress() != null ? order.getDeliveryAddress().getZipCode() : null)
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .tax(order.getTax())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .paymentId(paymentId)
                .paymentTransactionId(paymentTransactionId)
                .specialInstructions(order.getSpecialInstructions())
                .estimatedDelivery(order.getEstimatedDelivery())
                .actualDelivery(order.getActualDelivery())
                // Phase 2: 配送距离和动态定价信息
                .deliveryDistanceKm(order.getDeliveryDistanceKm())
                .weatherCondition(order.getWeatherCondition())
                .badWeatherSurcharge(order.getBadWeatherSurcharge())
                .peakHourSurcharge(order.getPeakHourSurcharge())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    /**
     * 将 OrderItem 实体转换为 OrderItemDTO
     */
    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .id(orderItem.getId())
                .menuItemId(orderItem.getMenuItem().getId())
                .menuItemName(orderItem.getMenuItem().getName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getSubtotal())
                .specialInstructions(orderItem.getSpecialRequests())
                .build();
    }
    
    /**
     * 生成唯一订单号
     */
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 验证状态转换的合法性
     */
    /**
     * 餐厅老板标记订单为准备完成，可以取餐
     * 
     * @param orderId 订单 ID
     * @param ownerEmail 餐厅老板邮箱
     * @return 更新后的订单 DTO
     */
    @Transactional
    public OrderDTO markOrderReadyForPickup(Long orderId, String ownerEmail) {
        log.info("标记订单为准备完成: orderId={}, owner={}", orderId, ownerEmail);
        
        // 验证是订单的餐厅老板
        authorizationService.verifyOrderRestaurantOwner(orderId, ownerEmail);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));
        
        // 验证当前状态必须是 PREPARING
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalStateException("只有准备中(PREPARING)的订单才能标记为准备完成。当前状态: " + order.getStatus());
        }
        
        // 更新状态为 READY_FOR_PICKUP
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        Order updated = orderRepository.save(order);
        
        log.info("订单标记为准备完成成功: orderId={}, status={}", orderId, updated.getStatus());
        return convertToDTO(updated);
    }
    
    /**
     * 为订单创建支付记录
     * 
     * @param order 订单
     * @param customer 客户
     */
    private void createPaymentForOrder(Order order, User customer) {
        log.info("为订单创建支付记录: orderId={}, orderNumber={}", order.getId(), order.getOrderNumber());
        
        try {
            // 根据订单的支付方式创建对应的PaymentMethod枚举
            com.shydelivery.doordashsimulator.entity.PaymentMethod paymentMethod = 
                convertOrderPaymentMethodToPaymentMethod(order.getPaymentMethod());
            
            // 创建支付记录
            Payment payment = Payment.builder()
                .order(order)
                .customer(customer)
                .amount(order.getTotalAmount())
                .paymentMethod(paymentMethod)
                .status(com.shydelivery.doordashsimulator.entity.PaymentStatus.PENDING)
                .refundedAmount(BigDecimal.ZERO)
                .notes("订单创建时自动生成的支付记录")
                .build();
            
            paymentRepository.save(payment);
            
            log.info("支付记录创建成功: orderId={}, paymentMethod={}, amount={}", 
                order.getId(), paymentMethod, order.getTotalAmount());
                
        } catch (Exception e) {
            log.error("创建支付记录失败: orderId={}, error={}", order.getId(), e.getMessage());
            // 不抛出异常，避免影响订单创建
            // 可以在后续手动创建支付记录
        }
    }
    
    /**
     * 转换订单的支付方式为支付系统的PaymentMethod枚举
     */
    private com.shydelivery.doordashsimulator.entity.PaymentMethod convertOrderPaymentMethodToPaymentMethod(
            Order.PaymentMethod orderPaymentMethod) {
        if (orderPaymentMethod == null) {
            return com.shydelivery.doordashsimulator.entity.PaymentMethod.CREDIT_CARD; // 默认值
        }
        
        // 将Order.PaymentMethod映射到Payment.PaymentMethod
        switch (orderPaymentMethod) {
            case CREDIT_CARD:
                return com.shydelivery.doordashsimulator.entity.PaymentMethod.CREDIT_CARD;
            case DEBIT_CARD:
                return com.shydelivery.doordashsimulator.entity.PaymentMethod.DEBIT_CARD;
            case PAYPAL:
            case APPLE_PAY:
            case GOOGLE_PAY:
                return com.shydelivery.doordashsimulator.entity.PaymentMethod.DIGITAL_WALLET;
            case CASH:
                return com.shydelivery.doordashsimulator.entity.PaymentMethod.CASH;
            default:
                log.warn("未知的支付方式: {}, 使用默认CREDIT_CARD", orderPaymentMethod);
                return com.shydelivery.doordashsimulator.entity.PaymentMethod.CREDIT_CARD;
        }
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // 简化版状态机验证
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("已完成或已取消的订单不能更改状态");
        }
        
        // TODO: 实现完整的状态机验证逻辑
    }
}
