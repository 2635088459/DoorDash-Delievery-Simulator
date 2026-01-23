# DoorDash 系统 - 基于角色的访问控制 (RBAC) 设计文档

## 📋 目录

1. [系统概述](#系统概述)
2. [角色定义](#角色定义)
3. [权限矩阵](#权限矩阵)
4. [API 端点权限设计](#api-端点权限设计)
5. [实现技术方案](#实现技术方案)
6. [安全策略](#安全策略)
7. [测试计划](#测试计划)

---

## 系统概述

### 什么是 RBAC？

**基于角色的访问控制（Role-Based Access Control）** 是一种权限管理机制，通过为用户分配角色，然后为角色分配权限，从而控制用户对系统资源的访问。

### 为什么需要 RBAC？

- ✅ **安全性**：防止未授权访问敏感数据和操作
- ✅ **可维护性**：集中管理权限，便于修改和审计
- ✅ **业务隔离**：不同角色只能访问自己的业务范围
- ✅ **合规性**：满足数据保护和隐私法规要求

### 我们系统的 RBAC 架构

```
┌─────────────┐
│   用户      │
│   (User)    │
└──────┬──────┘
       │ has one
       ▼
┌─────────────┐
│   角色      │──────► CUSTOMER (顾客)
│   (Role)    │──────► RESTAURANT_OWNER (餐厅老板)
└──────┬──────┘──────► DRIVER (配送员)
       │ has many
       ▼
┌─────────────┐
│   权限      │──────► 浏览餐厅
│(Permission) │──────► 创建订单
└─────────────┘──────► 管理菜单
                 ──────► 接单配送
                 ──────► ...
```

---

## 角色定义

### 1. CUSTOMER（顾客）

**角色描述**：使用平台点餐的终端用户

**业务场景**：
- 浏览餐厅和菜单
- 下单购买食物
- 跟踪订单状态
- 评价订单

**关键特征**：
- 可以有多个地址
- 可以有多个订单
- 可以收藏餐厅
- 可以查看订单历史和评价历史

---

### 2. RESTAURANT_OWNER（餐厅老板）

**角色描述**：在平台上经营餐厅的商家

**业务场景**：
- 创建和管理餐厅信息
- 管理菜单（添加、编辑、删除菜品）
- 接收和处理订单
- 查看营业数据和统计

**关键特征**：
- 可以拥有多个餐厅
- 每个餐厅有独立的菜单
- 只能管理自己的餐厅
- 可以查看餐厅的订单和评价

---

### 3. DRIVER（配送员）

**角色描述**：负责配送订单的配送员

**业务场景**：
- 查看可接单列表
- 接单并配送
- 更新配送状态
- 查看配送历史和收入

**关键特征**：
- 可以同时处理多个订单
- 实时位置跟踪
- 配送统计和评分
- 收入计算

---

## 权限矩阵

### 完整权限对照表

| 功能模块 | API 端点 | CUSTOMER | RESTAURANT_OWNER | DRIVER | 说明 |
|---------|---------|----------|------------------|--------|------|
| **用户管理** |
| 注册用户 | POST /api/users | ✅ Public | ✅ Public | ✅ Public | 任何人可注册 |
| 获取当前用户 | GET /api/auth/me | ✅ | ✅ | ✅ | 必须登录 |
| 更新个人信息 | PUT /api/users/me | ✅ | ✅ | ✅ | 仅自己 |
| 删除账户 | DELETE /api/users/me | ✅ | ✅ | ✅ | 仅自己 |
| **餐厅管理** |
| 查看餐厅列表 | GET /api/restaurants | ✅ Public | ✅ Public | ✅ Public | 任何人可查看 |
| 查看餐厅详情 | GET /api/restaurants/{id} | ✅ Public | ✅ Public | ✅ Public | 任何人可查看 |
| 创建餐厅 | POST /api/restaurants | ❌ | ✅ | ❌ | 仅餐厅老板 |
| 更新餐厅 | PUT /api/restaurants/{id} | ❌ | ✅ | ❌ | 仅所有者 |
| 删除餐厅 | DELETE /api/restaurants/{id} | ❌ | ✅ | ❌ | 仅所有者 |
| 获取我的餐厅 | GET /api/restaurants/my | ❌ | ✅ | ❌ | 仅餐厅老板 |
| **菜单管理** |
| 查看菜单 | GET /api/restaurants/{id}/menu | ✅ Public | ✅ Public | ✅ Public | 任何人可查看 |
| 添加菜品 | POST /api/menu-items | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 更新菜品 | PUT /api/menu-items/{id} | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 删除菜品 | DELETE /api/menu-items/{id} | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 上架/下架菜品 | PATCH /api/menu-items/{id}/availability | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| **订单管理** |
| 创建订单 | POST /api/orders | ✅ | ❌ | ❌ | 仅顾客 |
| 查看我的订单 | GET /api/orders/my | ✅ | ❌ | ❌ | 仅顾客 |
| 查看订单详情 | GET /api/orders/{id} | ✅ | ✅ | ✅ | 相关方可见 |
| 取消订单 | POST /api/orders/{id}/cancel | ✅ | ❌ | ❌ | 仅顾客（限时） |
| 餐厅查看订单 | GET /api/orders/restaurant/{restaurantId} | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 接受订单 | POST /api/orders/{id}/accept | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 拒绝订单 | POST /api/orders/{id}/reject | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| 订单准备完成 | POST /api/orders/{id}/ready | ❌ | ✅ | ❌ | 仅餐厅所有者 |
| **配送管理** |
| 查看可接单列表 | GET /api/deliveries/available | ❌ | ❌ | ✅ | 仅配送员 |
| 接单 | POST /api/deliveries/{orderId}/accept | ❌ | ❌ | ✅ | 仅配送员 |
| 取货 | POST /api/deliveries/{orderId}/pickup | ❌ | ❌ | ✅ | 仅配送员 |
| 送达 | POST /api/deliveries/{orderId}/deliver | ❌ | ❌ | ✅ | 仅配送员 |
| 配送员订单历史 | GET /api/deliveries/my | ❌ | ❌ | ✅ | 仅配送员 |
| 更新位置 | PUT /api/deliveries/location | ❌ | ❌ | ✅ | 仅配送员 |
| **评价管理** |
| 创建评价 | POST /api/reviews | ✅ | ❌ | ❌ | 仅顾客 |
| 查看餐厅评价 | GET /api/restaurants/{id}/reviews | ✅ Public | ✅ Public | ✅ Public | 任何人可查看 |
| 我的评价 | GET /api/reviews/my | ✅ | ❌ | ❌ | 仅顾客 |
| 更新评价 | PUT /api/reviews/{id} | ✅ | ❌ | ❌ | 仅评价者 |
| 删除评价 | DELETE /api/reviews/{id} | ✅ | ❌ | ❌ | 仅评价者 |
| 餐厅回复评价 | POST /api/reviews/{id}/reply | ❌ | ✅ | ❌ | 仅餐厅所有者 |

**图例说明**：
- ✅ = 允许访问
- ❌ = 禁止访问
- ✅ Public = 公开访问（无需登录）

---

## API 端点权限设计

### 1. 用户认证模块 (AuthController)

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // ✅ 公开 - 任何人可以注册
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) { }
    
    // ✅ 公开 - 任何人可以登录
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) { }
    
    // 🔒 需要登录 - 任何已认证用户
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() { }
    
    // 🔒 需要登录 - 任何已认证用户
    @PostMapping("/logout")
    public ResponseEntity<?> logout() { }
}
```

---

### 2. 餐厅管理模块 (RestaurantController)

```java
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {
    
    // ✅ 公开 - 所有人可以浏览餐厅
    @GetMapping
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurants() { }
    
    // ✅ 公开 - 所有人可以查看详情
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurant(@PathVariable Long id) { }
    
    // 🔒 仅餐厅老板 - 创建餐厅
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping
    public ResponseEntity<RestaurantDTO> createRestaurant(@RequestBody CreateRestaurantRequest request) { }
    
    // 🔒 仅餐厅老板 - 查看我的餐厅
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @GetMapping("/my")
    public ResponseEntity<List<RestaurantDTO>> getMyRestaurants() { }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 更新餐厅
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantDTO> updateRestaurant(
        @PathVariable Long id,
        @RequestBody UpdateRestaurantRequest request,
        Principal principal
    ) {
        // 业务层验证：确保当前用户是餐厅所有者
        restaurantService.verifyOwnership(id, principal.getName());
        // ...
    }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 删除餐厅
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestaurant(@PathVariable Long id, Principal principal) { }
}
```

**关键点**：
- `@PreAuthorize("hasRole('RESTAURANT_OWNER')")` - Spring Security 注解，在方法执行前检查角色
- **双重验证**：先检查角色，再在业务层验证资源所有权
- **资源所有权验证**：确保用户只能操作自己的餐厅

---

### 3. 菜单管理模块 (MenuItemController)

```java
@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {
    
    // ✅ 公开 - 任何人可以查看菜单
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<MenuItemDTO>> getMenuItems(@PathVariable Long restaurantId) { }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 添加菜品
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping
    public ResponseEntity<MenuItemDTO> createMenuItem(
        @RequestBody CreateMenuItemRequest request,
        Principal principal
    ) {
        // 验证餐厅所有权
        restaurantService.verifyOwnership(request.getRestaurantId(), principal.getName());
        // ...
    }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 更新菜品
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<MenuItemDTO> updateMenuItem(
        @PathVariable Long id,
        @RequestBody UpdateMenuItemRequest request,
        Principal principal
    ) { }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 删除菜品
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMenuItem(@PathVariable Long id, Principal principal) { }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 上架/下架
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PatchMapping("/{id}/availability")
    public ResponseEntity<MenuItemDTO> updateAvailability(
        @PathVariable Long id,
        @RequestBody UpdateAvailabilityRequest request,
        Principal principal
    ) { }
}
```

---

### 4. 订单管理模块 (OrderController)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    // 🔒 仅顾客 - 创建订单
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
        @RequestBody CreateOrderRequest request,
        Principal principal
    ) {
        // 自动关联到当前登录的顾客
        String email = principal.getName();
        return orderService.createOrder(request, email);
    }
    
    // 🔒 仅顾客 - 查看我的订单
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Principal principal) {
        String email = principal.getName();
        return orderService.getOrdersByCustomer(email);
    }
    
    // 🔒 需要登录 + 所有权验证 - 查看订单详情
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id, Principal principal) {
        // 验证：顾客本人、餐厅老板、或配送员可以查看
        orderService.verifyAccess(id, principal.getName());
        // ...
    }
    
    // 🔒 仅顾客 + 所有者验证 - 取消订单
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id, Principal principal) {
        // 验证是否是订单创建者
        // 验证订单状态是否允许取消（例如：只能在10分钟内取消）
    }
    
    // 🔒 仅餐厅老板 - 查看餐厅订单
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderDTO>> getRestaurantOrders(
        @PathVariable Long restaurantId,
        Principal principal
    ) {
        // 验证餐厅所有权
        restaurantService.verifyOwnership(restaurantId, principal.getName());
        // ...
    }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 接受订单
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping("/{id}/accept")
    public ResponseEntity<OrderDTO> acceptOrder(@PathVariable Long id, Principal principal) {
        // 验证订单属于该餐厅老板的餐厅
        orderService.verifyRestaurantOwnership(id, principal.getName());
        // 更新订单状态为 ACCEPTED
    }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 订单准备完成
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping("/{id}/ready")
    public ResponseEntity<OrderDTO> markOrderReady(@PathVariable Long id, Principal principal) { }
}
```

**订单状态流转**：
```
PENDING → ACCEPTED → PREPARING → READY → PICKED_UP → DELIVERED → COMPLETED
    ↓         ↓
CANCELLED  REJECTED
```

**权限控制点**：
- **PENDING → ACCEPTED/REJECTED**: 仅餐厅老板
- **ACCEPTED → PREPARING → READY**: 仅餐厅老板
- **READY → PICKED_UP**: 仅配送员
- **PICKED_UP → DELIVERED**: 仅配送员
- **任意状态 → CANCELLED**: 仅顾客（有时间限制）

---

### 5. 配送管理模块 (DeliveryController)

```java
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    
    // 🔒 仅配送员 - 查看可接单列表
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/available")
    public ResponseEntity<List<OrderDTO>> getAvailableOrders() {
        // 返回状态为 READY 且未分配配送员的订单
    }
    
    // 🔒 仅配送员 - 接单
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{orderId}/accept")
    public ResponseEntity<OrderDTO> acceptDelivery(
        @PathVariable Long orderId,
        Principal principal
    ) {
        // 验证订单状态为 READY
        // 分配配送员
        // 更新状态为 PICKED_UP
    }
    
    // 🔒 仅配送员 + 所有者验证 - 标记取货
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{orderId}/pickup")
    public ResponseEntity<OrderDTO> pickupOrder(
        @PathVariable Long orderId,
        Principal principal
    ) {
        // 验证是否是当前配送员的订单
        deliveryService.verifyDriver(orderId, principal.getName());
        // ...
    }
    
    // 🔒 仅配送员 + 所有者验证 - 标记送达
    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderDTO> deliverOrder(
        @PathVariable Long orderId,
        Principal principal
    ) { }
    
    // 🔒 仅配送员 - 查看配送历史
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/my")
    public ResponseEntity<List<OrderDTO>> getMyDeliveries(Principal principal) { }
    
    // 🔒 仅配送员 - 更新位置
    @PreAuthorize("hasRole('DRIVER')")
    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateRequest request, Principal principal) {
        // 实时位置跟踪
    }
}
```

---

### 6. 评价管理模块 (ReviewController)

```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    // 🔒 仅顾客 - 创建评价
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
        @RequestBody CreateReviewRequest request,
        Principal principal
    ) {
        // 验证：顾客必须完成过该餐厅的订单才能评价
        reviewService.verifyOrderCompleted(request.getRestaurantId(), principal.getName());
        // ...
    }
    
    // ✅ 公开 - 查看餐厅评价
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<ReviewDTO>> getRestaurantReviews(@PathVariable Long restaurantId) { }
    
    // 🔒 仅顾客 - 我的评价
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(Principal principal) { }
    
    // 🔒 仅顾客 + 所有者验证 - 更新评价
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
        @PathVariable Long id,
        @RequestBody UpdateReviewRequest request,
        Principal principal
    ) {
        // 验证是否是评价创建者
        reviewService.verifyReviewer(id, principal.getName());
        // ...
    }
    
    // 🔒 仅餐厅老板 + 所有者验证 - 回复评价
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    @PostMapping("/{id}/reply")
    public ResponseEntity<ReviewDTO> replyToReview(
        @PathVariable Long id,
        @RequestBody ReplyRequest request,
        Principal principal
    ) {
        // 验证评价是针对该餐厅老板的餐厅
        reviewService.verifyRestaurantOwner(id, principal.getName());
        // ...
    }
}
```

---

## 实现技术方案

### 1. Spring Security 配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← 启用 @PreAuthorize 注解
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/menu-items/**").permitAll()
                
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 2. JWT Filter 增强 - 从数据库获取最新角色

当前实现已经支持，但我们需要优化以从数据库获取最新角色：

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // ... JWT 验证 ...
        
        // 从数据库获取最新的用户角色
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        // 使用数据库中的最新角色（而不是 Token 中的）
        String role = user.getRole().name();
        
        List<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role)
        );
        
        // ...
    }
}
```

**优势**：
- ✅ 角色变更立即生效，无需重新登录
- ✅ 支持动态角色管理
- ✅ 更安全（Token 无法被伪造角色）

### 3. 资源所有权验证服务

创建通用的验证服务：

```java
@Service
public class AuthorizationService {
    
    @Autowired
    private RestaurantRepository restaurantRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 验证餐厅所有权
     */
    public void verifyRestaurantOwnership(Long restaurantId, String email) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在"));
        
        User owner = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        if (!restaurant.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("您没有权限访问此餐厅");
        }
    }
    
    /**
     * 验证订单访问权限
     * 允许：订单创建者（顾客）、餐厅老板、配送员
     */
    public void verifyOrderAccess(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        
        boolean hasAccess = 
            // 是顾客本人
            order.getCustomer().getId().equals(user.getId()) ||
            // 是餐厅老板
            order.getRestaurant().getOwner().getId().equals(user.getId()) ||
            // 是配送员
            (order.getDriver() != null && order.getDriver().getId().equals(user.getId()));
        
        if (!hasAccess) {
            throw new AccessDeniedException("您没有权限查看此订单");
        }
    }
    
    /**
     * 验证订单是否属于餐厅老板的餐厅
     */
    public void verifyOrderRestaurantOwnership(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        
        verifyRestaurantOwnership(order.getRestaurant().getId(), email);
    }
}
```

### 4. 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理访问被拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "访问被拒绝",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
            ));
    }
    
    /**
     * 处理未认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "error", "未认证",
                "message", "请先登录",
                "timestamp", LocalDateTime.now()
            ));
    }
}
```

---

## 安全策略

### 1. 纵深防御（Defense in Depth）

```
┌─────────────────────────────────────────┐
│  Layer 1: Spring Security Filter Chain  │ ← JWT Token 验证
├─────────────────────────────────────────┤
│  Layer 2: @PreAuthorize Annotation      │ ← 角色验证
├─────────────────────────────────────────┤
│  Layer 3: Service Layer Validation      │ ← 资源所有权验证
├─────────────────────────────────────────┤
│  Layer 4: Database Constraints           │ ← 外键约束、唯一约束
└─────────────────────────────────────────┘
```

### 2. 最小权限原则

- ✅ 用户只能访问他们**需要**的资源
- ✅ 默认拒绝访问，显式授权
- ✅ 细粒度权限控制

### 3. 审计日志

记录所有关键操作：

```java
@Aspect
@Component
public class AuditAspect {
    
    @Around("@annotation(PreAuthorize)")
    public Object auditSecuredMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录谁在什么时间访问了什么资源
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String method = joinPoint.getSignature().getName();
        
        log.info("用户 {} 访问了 {}", user, method);
        
        return joinPoint.proceed();
    }
}
```

---

## 测试计划

### 1. 单元测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class RestaurantControllerSecurityTest {
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_cannot_create_restaurant() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void owner_can_create_restaurant() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());
    }
}
```

### 2. 集成测试

测试完整的权限流程：

```bash
# 1. 顾客登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"customer@example.com","password":"Test@123"}' | jq -r '.idToken')

# 2. 尝试创建餐厅（应该失败）
curl -X POST http://localhost:8080/api/restaurants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Restaurant"}'
# 预期: 403 Forbidden

# 3. 创建订单（应该成功）
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...}'
# 预期: 200 OK
```

### 3. 安全测试清单

- [ ] 未登录用户无法访问受保护端点
- [ ] 顾客无法创建餐厅
- [ ] 顾客无法编辑其他人的订单
- [ ] 餐厅老板无法编辑其他人的餐厅
- [ ] 配送员无法接已被接的订单
- [ ] Token 过期后无法访问
- [ ] 伪造 Token 无法通过验证
- [ ] 角色变更后权限立即生效

---

## 实施步骤

### Phase 1: 基础设施（第1周）
1. ✅ 完成 JWT 认证（已完成）
2. ✅ 配置 Spring Security（已完成）
3. ⏳ 创建 AuthorizationService
4. ⏳ 配置全局异常处理

### Phase 2: Controller 层权限（第2周）
1. ⏳ 为所有 Controller 添加 @PreAuthorize 注解
2. ⏳ 实现资源所有权验证
3. ⏳ 编写单元测试

### Phase 3: 测试与优化（第3周）
1. ⏳ 集成测试
2. ⏳ 安全测试
3. ⏳ 性能优化
4. ⏳ 文档完善

---

## 附录

### A. 常用 @PreAuthorize 表达式

```java
// 单一角色
@PreAuthorize("hasRole('CUSTOMER')")

// 多个角色（任一）
@PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")

// 组合条件
@PreAuthorize("hasRole('RESTAURANT_OWNER') and #restaurantId == principal.restaurantId")

// 自定义验证
@PreAuthorize("@authorizationService.canAccessOrder(#orderId, principal.name)")
```

### B. 错误响应格式

```json
{
  "timestamp": "2026-01-19T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "您没有权限执行此操作",
  "path": "/api/restaurants/123"
}
```

### C. 相关链接

- [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/index.html)
- [JWT 最佳实践](https://tools.ietf.org/html/rfc8725)
- [OWASP 访问控制备忘单](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)

---

**文档版本**: v1.0  
**最后更新**: 2026-01-19  
**作者**: DoorDash 开发团队
