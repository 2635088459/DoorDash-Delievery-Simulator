# Driver 配送系统设计文档

## 📋 系统概述

Driver 配送系统是 DoorDash 的核心功能之一，负责管理配送员、订单分配、配送跟踪等功能。

### 业务流程
```
1. 客户下单 → 订单创建 (PENDING)
2. 系统分配订单给配送员 → 订单状态 (ASSIGNED)
3. 配送员接单 → 订单状态 (CONFIRMED)
4. 配送员取餐 → 订单状态 (PICKED_UP)
5. 配送员送达 → 订单状态 (DELIVERED)
6. 客户确认收货 → 订单完成 (COMPLETED)
```

---

## 🎯 核心功能模块

### 1. Driver Management（配送员管理）
- 配送员注册/认证
- 配送员信息管理
- 配送员状态（在线/离线/忙碌）
- 配送员评分系统

### 2. Order Assignment（订单分配）
- 自动分配算法（就近原则）
- 手动分配
- 订单池（未分配订单）
- 重新分配机制

### 3. Delivery Tracking（配送跟踪）
- 配送员实时位置
- 配送路线规划
- 预计送达时间（ETA）
- 配送历史记录

### 4. Driver Earnings（配送员收益）
- 配送费计算
- 小费管理
- 收益统计
- 提现功能

---

## 📊 数据库设计

### 1. Driver 表（配送员）
```sql
CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    
    -- 基本信息
    vehicle_type VARCHAR(50) NOT NULL,  -- BICYCLE, MOTORCYCLE, CAR
    license_number VARCHAR(100),
    vehicle_model VARCHAR(100),
    
    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',  -- ONLINE, OFFLINE, BUSY, INACTIVE
    current_latitude DECIMAL(10, 8),
    current_longitude DECIMAL(11, 8),
    
    -- 统计信息
    rating DECIMAL(3, 2) DEFAULT 5.00,
    total_deliveries INTEGER DEFAULT 0,
    completed_deliveries INTEGER DEFAULT 0,
    cancelled_deliveries INTEGER DEFAULT 0,
    
    -- 收益信息
    total_earnings DECIMAL(10, 2) DEFAULT 0.00,
    available_balance DECIMAL(10, 2) DEFAULT 0.00,
    
    -- 认证信息
    is_verified BOOLEAN DEFAULT FALSE,
    verification_date TIMESTAMP,
    background_check_status VARCHAR(20),  -- PENDING, APPROVED, REJECTED
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id)
);

CREATE INDEX idx_driver_status ON drivers(status);
CREATE INDEX idx_driver_location ON drivers(current_latitude, current_longitude);
```

### 2. Delivery 表（配送记录）
```sql
CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    driver_id BIGINT NOT NULL REFERENCES drivers(id),
    
    -- 配送状态
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',  
    -- ASSIGNED, ACCEPTED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED
    
    -- 时间信息
    assigned_at TIMESTAMP,
    accepted_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    delivered_at TIMESTAMP,
    estimated_delivery_time TIMESTAMP,
    
    -- 位置信息
    pickup_latitude DECIMAL(10, 8),
    pickup_longitude DECIMAL(11, 8),
    delivery_latitude DECIMAL(10, 8),
    delivery_longitude DECIMAL(11, 8),
    
    -- 距离和时间
    distance_km DECIMAL(8, 2),
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    
    -- 收益信息
    delivery_fee DECIMAL(8, 2),
    tip_amount DECIMAL(8, 2) DEFAULT 0.00,
    total_earnings DECIMAL(8, 2),
    
    -- 评价信息
    driver_rating INTEGER CHECK (driver_rating BETWEEN 1 AND 5),
    customer_feedback TEXT,
    
    -- 其他
    notes TEXT,
    cancellation_reason VARCHAR(255),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_order ON deliveries(order_id);
CREATE INDEX idx_delivery_driver ON deliveries(driver_id);
CREATE INDEX idx_delivery_status ON deliveries(status);
```

### 3. Driver Location History 表（位置历史）
```sql
CREATE TABLE driver_location_history (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES drivers(id),
    delivery_id BIGINT REFERENCES deliveries(id),
    
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    
    speed_kmh DECIMAL(5, 2),
    heading DECIMAL(5, 2),  -- 方向角度
    
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_driver_time ON driver_location_history(driver_id, recorded_at);
CREATE INDEX idx_location_delivery ON driver_location_history(delivery_id);
```

---

## 🏗️ 实体设计（Entity）

### Driver Entity
```java
@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    
    @Enumerated(EnumType.STRING)
    private DriverStatus status;
    
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    
    private BigDecimal rating;
    private Integer totalDeliveries;
    private Integer completedDeliveries;
    
    private BigDecimal totalEarnings;
    private BigDecimal availableBalance;
    
    @OneToMany(mappedBy = "driver")
    private List<Delivery> deliveries;
}

public enum VehicleType {
    BICYCLE,
    MOTORCYCLE,
    CAR
}

public enum DriverStatus {
    OFFLINE,    // 离线
    ONLINE,     // 在线（可接单）
    BUSY,       // 忙碌（配送中）
    INACTIVE    // 暂停接单
}
```

### Delivery Entity
```java
@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    
    private BigDecimal deliveryFee;
    private BigDecimal tipAmount;
    private BigDecimal totalEarnings;
    
    private Integer driverRating;
    private String customerFeedback;
}

public enum DeliveryStatus {
    ASSIGNED,      // 已分配
    ACCEPTED,      // 已接受
    PICKED_UP,     // 已取餐
    IN_TRANSIT,    // 配送中
    DELIVERED,     // 已送达
    CANCELLED      // 已取消
}
```

---

## 🔌 REST API 设计

### 1. Driver Management APIs

#### 注册为配送员
```
POST /api/drivers/register
```
**Request Body:**
```json
{
  "vehicleType": "MOTORCYCLE",
  "licenseNumber": "DL123456789",
  "vehicleModel": "Honda CBR"
}
```

#### 获取配送员信息
```
GET /api/drivers/me
```

#### 更新配送员状态
```
PUT /api/drivers/status
```
**Request Body:**
```json
{
  "status": "ONLINE",
  "latitude": 37.7749,
  "longitude": -122.4194
}
```

#### 更新配送员位置
```
PUT /api/drivers/location
```
**Request Body:**
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "speed": 25.5,
  "heading": 180
}
```

### 2. Delivery Assignment APIs

#### 获取可用订单（配送员端）
```
GET /api/deliveries/available
```
**Query Parameters:**
- `latitude`: 配送员当前纬度
- `longitude`: 配送员当前经度
- `maxDistance`: 最大距离（km）

#### 接受订单
```
POST /api/deliveries/{deliveryId}/accept
```

#### 拒绝订单
```
POST /api/deliveries/{deliveryId}/reject
```
**Request Body:**
```json
{
  "reason": "太远了"
}
```

### 3. Delivery Tracking APIs

#### 更新配送状态
```
PUT /api/deliveries/{deliveryId}/status
```
**Request Body:**
```json
{
  "status": "PICKED_UP",
  "notes": "已从餐厅取餐"
}
```

#### 完成配送
```
POST /api/deliveries/{deliveryId}/complete
```
**Request Body:**
```json
{
  "deliveryProof": "签名图片URL",
  "notes": "已送达门口"
}
```

#### 获取配送详情
```
GET /api/deliveries/{deliveryId}
```

#### 获取配送历史
```
GET /api/drivers/deliveries
```
**Query Parameters:**
- `status`: 过滤状态
- `startDate`: 开始日期
- `endDate`: 结束日期
- `page`: 页码
- `size`: 每页数量

### 4. Earnings APIs

#### 获取收益统计
```
GET /api/drivers/earnings
```
**Query Parameters:**
- `period`: TODAY, WEEK, MONTH

#### 获取收益明细
```
GET /api/drivers/earnings/details
```

#### 申请提现
```
POST /api/drivers/earnings/withdraw
```
**Request Body:**
```json
{
  "amount": 500.00,
  "withdrawMethod": "BANK_TRANSFER",
  "accountInfo": "账户信息"
}
```

---

## 🎯 核心业务逻辑

### 1. 订单分配算法

#### 自动分配策略（就近原则）
```java
public class DeliveryAssignmentService {
    
    /**
     * 自动分配订单给最近的在线配送员
     */
    public Delivery autoAssignOrder(Order order) {
        // 1. 获取所有在线配送员
        List<Driver> availableDrivers = driverRepository
            .findByStatus(DriverStatus.ONLINE);
        
        // 2. 计算每个配送员到餐厅的距离
        Restaurant restaurant = order.getRestaurant();
        Driver nearestDriver = availableDrivers.stream()
            .min(Comparator.comparing(driver -> 
                calculateDistance(
                    driver.getCurrentLatitude(), 
                    driver.getCurrentLongitude(),
                    restaurant.getLatitude(), 
                    restaurant.getLongitude()
                )
            ))
            .orElseThrow(() -> new BusinessException("没有可用的配送员"));
        
        // 3. 创建配送记录
        Delivery delivery = createDelivery(order, nearestDriver);
        
        // 4. 发送通知给配送员
        notificationService.notifyNewDelivery(nearestDriver, delivery);
        
        return delivery;
    }
    
    /**
     * 计算两点之间的距离（Haversine公式）
     */
    private double calculateDistance(
        BigDecimal lat1, BigDecimal lon1,
        BigDecimal lat2, BigDecimal lon2) {
        
        double earthRadiusKm = 6371;
        
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) * 
                   Math.cos(Math.toRadians(lat2.doubleValue())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadiusKm * c;
    }
}
```

### 2. 配送费计算

```java
public class DeliveryFeeCalculator {
    
    private static final BigDecimal BASE_FEE = new BigDecimal("3.00");
    private static final BigDecimal PER_KM_RATE = new BigDecimal("1.50");
    private static final BigDecimal MINIMUM_FEE = new BigDecimal("2.00");
    
    /**
     * 计算配送费
     */
    public BigDecimal calculateDeliveryFee(
        double distanceKm, 
        LocalDateTime orderTime) {
        
        BigDecimal fee = BASE_FEE;
        
        // 距离费用
        BigDecimal distanceFee = PER_KM_RATE
            .multiply(new BigDecimal(distanceKm));
        fee = fee.add(distanceFee);
        
        // 高峰期加价（午餐和晚餐时间）
        if (isPeakHour(orderTime)) {
            fee = fee.multiply(new BigDecimal("1.5"));
        }
        
        // 最低配送费
        if (fee.compareTo(MINIMUM_FEE) < 0) {
            fee = MINIMUM_FEE;
        }
        
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
    
    private boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 11 && hour <= 13) || (hour >= 17 && hour <= 20);
    }
}
```

### 3. ETA（预计送达时间）计算

```java
public class ETACalculator {
    
    private static final double AVERAGE_SPEED_KMH = 25.0;  // 平均速度
    private static final int RESTAURANT_PREP_TIME_MINUTES = 15;  // 餐厅准备时间
    private static final int PICKUP_TIME_MINUTES = 5;  // 取餐时间
    
    /**
     * 计算预计送达时间
     */
    public LocalDateTime calculateETA(
        BigDecimal driverLat, BigDecimal driverLon,
        BigDecimal restaurantLat, BigDecimal restaurantLon,
        BigDecimal customerLat, BigDecimal customerLon) {
        
        // 1. 配送员到餐厅的距离
        double distanceToRestaurant = calculateDistance(
            driverLat, driverLon, restaurantLat, restaurantLon);
        
        // 2. 餐厅到客户的距离
        double distanceToCustomer = calculateDistance(
            restaurantLat, restaurantLon, customerLat, customerLon);
        
        // 3. 计算总时间
        int travelTime = (int) Math.ceil(
            (distanceToRestaurant + distanceToCustomer) / AVERAGE_SPEED_KMH * 60);
        
        int totalMinutes = travelTime + RESTAURANT_PREP_TIME_MINUTES + PICKUP_TIME_MINUTES;
        
        return LocalDateTime.now().plusMinutes(totalMinutes);
    }
}
```

---

## 🔐 权限控制

### RBAC 权限设计

| 端点 | CUSTOMER | DRIVER | RESTAURANT_OWNER | ADMIN |
|------|----------|--------|------------------|-------|
| POST /drivers/register | ✅ | ❌ | ❌ | ✅ |
| GET /drivers/me | ❌ | ✅ | ❌ | ✅ |
| PUT /drivers/status | ❌ | ✅ | ❌ | ✅ |
| GET /deliveries/available | ❌ | ✅ | ❌ | ✅ |
| POST /deliveries/{id}/accept | ❌ | ✅ | ❌ | ❌ |
| GET /deliveries/{id}/track | ✅ | ✅ | ✅ | ✅ |

---

## 📱 实时功能

### WebSocket 实时更新

#### 1. 配送员位置更新
```java
@MessageMapping("/driver/location")
@SendTo("/topic/delivery/{deliveryId}/location")
public LocationUpdate updateLocation(LocationUpdate location) {
    // 更新配送员位置
    driverService.updateLocation(location);
    return location;
}
```

#### 2. 订单状态更新
```java
@MessageMapping("/delivery/status")
@SendToUser("/queue/delivery/status")
public DeliveryStatusUpdate updateStatus(DeliveryStatusUpdate update) {
    // 更新配送状态
    deliveryService.updateStatus(update);
    return update;
}
```

---

## 📊 统计和报表

### 配送员统计指标

1. **今日统计**
   - 完成订单数
   - 今日收益
   - 平均评分
   - 在线时长

2. **历史统计**
   - 总配送订单
   - 总收益
   - 平均配送时间
   - 客户满意度

3. **排名**
   - 配送效率排名
   - 收益排名
   - 评分排名

---

## 🧪 测试用例

### 1. 订单分配测试
```java
@Test
public void testAutoAssignOrder() {
    // 创建在线配送员
    Driver driver = createOnlineDriver(37.7749, -122.4194);
    
    // 创建订单
    Order order = createOrder();
    
    // 自动分配
    Delivery delivery = deliveryService.autoAssign(order);
    
    // 验证
    assertNotNull(delivery);
    assertEquals(driver.getId(), delivery.getDriver().getId());
    assertEquals(DeliveryStatus.ASSIGNED, delivery.getStatus());
}
```

### 2. 配送状态更新测试
```java
@Test
public void testUpdateDeliveryStatus() {
    // 创建配送记录
    Delivery delivery = createDelivery(DeliveryStatus.ACCEPTED);
    
    // 更新为已取餐
    deliveryService.updateStatus(delivery.getId(), DeliveryStatus.PICKED_UP);
    
    // 验证
    Delivery updated = deliveryRepository.findById(delivery.getId()).get();
    assertEquals(DeliveryStatus.PICKED_UP, updated.getStatus());
    assertNotNull(updated.getPickedUpAt());
}
```

---

## 📚 技术知识点

### 1. 地理位置计算

#### Haversine 公式
用于计算地球表面两点之间的大圆距离（Great Circle Distance）。

**公式：**
```
a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
c = 2 ⋅ atan2(√a, √(1−a))
d = R ⋅ c
```

其中：
- φ 是纬度（latitude）
- λ 是经度（longitude）
- R 是地球半径（约 6371 km）

### 2. PostGIS（空间数据库扩展）

PostgreSQL 的 PostGIS 扩展提供了更强大的地理位置查询功能：

```sql
-- 启用 PostGIS
CREATE EXTENSION postgis;

-- 添加地理位置列
ALTER TABLE drivers ADD COLUMN location GEOGRAPHY(POINT, 4326);

-- 查找附近的配送员（5km 内）
SELECT * FROM drivers 
WHERE ST_DWithin(
    location,
    ST_MakePoint(-122.4194, 37.7749)::geography,
    5000  -- 5km in meters
)
AND status = 'ONLINE';
```

### 3. 实时通信技术

#### WebSocket vs SSE vs Long Polling

| 技术 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **WebSocket** | 双向通信、低延迟 | 复杂度高 | 实时聊天、位置跟踪 |
| **SSE** | 简单、服务器推送 | 单向通信 | 状态更新、通知 |
| **Long Polling** | 兼容性好 | 效率低 | 低频更新 |

### 4. 并发控制

#### 订单抢单的并发问题

**问题：** 多个配送员同时接单

**解决方案：** 乐观锁 + 数据库唯一约束

```java
@Entity
public class Delivery {
    @Version
    private Long version;  // 乐观锁版本号
}

public Delivery acceptDelivery(Long deliveryId, Long driverId) {
    Delivery delivery = deliveryRepository.findById(deliveryId)
        .orElseThrow();
    
    if (delivery.getStatus() != DeliveryStatus.ASSIGNED) {
        throw new BusinessException("订单已被接取");
    }
    
    delivery.setDriver(driver);
    delivery.setStatus(DeliveryStatus.ACCEPTED);
    delivery.setAcceptedAt(LocalDateTime.now());
    
    return deliveryRepository.save(delivery);  // 版本冲突会抛出异常
}
```

### 5. 缓存策略

#### Redis 缓存在线配送员

```java
@Service
public class DriverCacheService {
    
    @Autowired
    private RedisTemplate<String, Driver> redisTemplate;
    
    private static final String ONLINE_DRIVERS_KEY = "drivers:online";
    
    /**
     * 缓存在线配送员
     */
    public void cacheOnlineDriver(Driver driver) {
        redisTemplate.opsForGeo().add(
            ONLINE_DRIVERS_KEY,
            new Point(driver.getCurrentLongitude(), driver.getCurrentLatitude()),
            driver.getId().toString()
        );
    }
    
    /**
     * 查找附近的配送员
     */
    public List<Driver> findNearbyDrivers(double lat, double lon, double radius) {
        GeoResults<GeoLocation<String>> results = redisTemplate.opsForGeo()
            .radius(ONLINE_DRIVERS_KEY, 
                    new Circle(new Point(lon, lat), new Distance(radius, Metrics.KILOMETERS)));
        
        return results.getContent().stream()
            .map(result -> driverRepository.findById(Long.parseLong(result.getContent().getName())).get())
            .collect(Collectors.toList());
    }
}
```

---

## 🚀 实施步骤

### Phase 1: 基础功能（第 1-2 天）
1. ✅ 创建数据库表
2. ✅ 创建 Entity 实体类
3. ✅ 创建 Repository
4. ✅ 实现配送员注册
5. ✅ 实现配送员状态管理

### Phase 2: 订单分配（第 3-4 天）
1. ✅ 实现自动分配算法
2. ✅ 实现手动分配
3. ✅ 实现配送员接单
4. ✅ 实现订单重新分配

### Phase 3: 配送跟踪（第 5-6 天）
1. ✅ 实现配送状态更新
2. ✅ 实现位置跟踪
3. ✅ 实现 ETA 计算
4. ✅ 实现配送完成

### Phase 4: 收益管理（第 7 天）
1. ✅ 实现配送费计算
2. ✅ 实现收益统计
3. ✅ 实现提现功能

### Phase 5: 优化和测试（第 8-9 天）
1. ✅ 添加单元测试
2. ✅ 性能优化
3. ✅ API 文档
4. ✅ 集成测试

---

## 📖 参考资料

1. **地理位置计算**
   - [Haversine Formula](https://en.wikipedia.org/wiki/Haversine_formula)
   - [PostGIS Documentation](https://postgis.net/docs/)

2. **实时通信**
   - [Spring WebSocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
   - [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

3. **并发控制**
   - [Optimistic Locking in JPA](https://www.baeldung.com/jpa-optimistic-locking)
   - [Distributed Locks with Redis](https://redis.io/topics/distlock)

4. **缓存**
   - [Spring Data Redis](https://spring.io/projects/spring-data-redis)
   - [Redis Geospatial](https://redis.io/commands/geoadd)

---

**文档版本**: 1.0  
**创建日期**: 2026-01-21  
**下一步**: 开始实现 Phase 1 - 基础功能
