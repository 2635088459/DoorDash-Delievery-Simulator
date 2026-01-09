# Order 实体类解析 - 关系映射核心知识点

## 📋 类概述

**最复杂的实体类** - 包含 3 个 `@ManyToOne` 关系，展示了 JPA 关系映射的核心用法。

```java
@Entity
@Table(name = "orders")
public class Order {
    // 3个多对一关系
    @ManyToOne private User customer;
    @ManyToOne private Restaurant restaurant;
    @ManyToOne private Address deliveryAddress;
    
    // 3个枚举类型
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    
    // 4个金额字段（BigDecimal）
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal totalAmount;
}
```

---

## 🔥 核心知识点（User 文档未涵盖）

### 1️⃣ `@ManyToOne` - 多对一关系 ⭐⭐⭐⭐⭐

**最重要的新知识点！**

#### 示例 1：订单属于客户
```java
/**
 * Many orders belong to one customer
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id", nullable = false)
private User customer;
```

**关键点：**
- `@ManyToOne`：多个订单 → 一个客户
- `@JoinColumn(name = "customer_id")`：外键列名
- `fetch = FetchType.LAZY`：延迟加载（性能优化）
- `nullable = false`：客户必须存在

**生成 SQL：**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id)
);
```

---

#### 示例 2：订单属于餐厅
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "restaurant_id", nullable = false)
private Restaurant restaurant;
```

**业务含义：**
- 一个餐厅可以接收多个订单
- 每个订单只能来自一个餐厅
- 外键在 `orders` 表（多的一方）

---

#### 示例 3：订单使用配送地址
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "delivery_address_id", nullable = false)
private Address deliveryAddress;
```

**为什么需要？**
- 用户可能有多个地址
- 订单需要记录具体配送到哪个地址
- 地址删除不影响历史订单（如果做软删除）

---

### 2️⃣ `@JoinColumn` 详解

```java
@JoinColumn(name = "customer_id", nullable = false)
```

**参数说明：**

| 参数 | 说明 | 示例值 |
|------|------|--------|
| `name` | 外键列名 | `"customer_id"` |
| `nullable` | 是否允许 NULL | `false` |
| `unique` | 是否唯一（@OneToOne 需要） | `true` |

**命名规范：**
```
{关联实体名}_id
customer_id    → 关联 User (customer)
restaurant_id  → 关联 Restaurant
```

---

### 3️⃣ `FetchType.LAZY` - 延迟加载

**工作原理：**

```java
// 1. 查询订单
Order order = orderRepository.findById(1L).get();
// SQL: SELECT * FROM orders WHERE id = 1
// 只查询 orders 表，不查询 users 表

// 2. 访问客户信息时才查询
String name = order.getCustomer().getFirstName();
// SQL: SELECT * FROM users WHERE id = ?
// 现在才查询 users 表
```

**vs FetchType.EAGER（立即加载）：**
```java
@ManyToOne(fetch = FetchType.EAGER)  // ❌ 不推荐
private User customer;

// 查询订单时立即 JOIN 查询 users
// SQL: SELECT o.*, u.* FROM orders o LEFT JOIN users u ...
// 可能加载不需要的数据
```

**最佳实践：默认用 LAZY，需要时用 JOIN FETCH**
```java
// Repository 中按需加载
@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.id = :id")
Order findByIdWithCustomer(@Param("id") Long id);
```

---

## 📊 多个枚举类型管理

Order 包含 **3 个枚举**，展示了复杂状态管理：

### 1️⃣ OrderStatus - 订单状态（8 个状态）

```java
public enum OrderStatus {
    PENDING,           // 待确认
    CONFIRMED,         // 已确认
    PREPARING,         // 准备中
    READY_FOR_PICKUP,  // 待取餐
    PICKED_UP,         // 已取餐
    IN_TRANSIT,        // 配送中
    DELIVERED,         // 已送达
    CANCELLED          // 已取消
}
```

**状态流转：**
```
PENDING → CONFIRMED → PREPARING → READY_FOR_PICKUP 
  → PICKED_UP → IN_TRANSIT → DELIVERED

  ↓ (任何时候都可以)
CANCELLED
```

---

### 2️⃣ PaymentMethod - 支付方式

```java
public enum PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    CASH,
    PAYPAL,
    APPLE_PAY,
    GOOGLE_PAY
}
```

---

### 3️⃣ PaymentStatus - 支付状态

```java
public enum PaymentStatus {
    PENDING,    // 待支付
    COMPLETED,  // 已完成
    FAILED,     // 失败
    REFUNDED    // 已退款
}
```

**为什么分开？**
- `paymentMethod`：用什么支付（信用卡/现金）
- `paymentStatus`：支付是否成功
- 一个订单可以是 `CREDIT_CARD` + `PENDING`（刷卡中）

---

## 💰 BigDecimal 金额计算

### 4 个金额字段

```java
@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal subtotal;        // 商品小计

@Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
private BigDecimal deliveryFee;     // 配送费

@Column(nullable = false, precision = 10, scale = 2)
private BigDecimal tax;             // 税费

@Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
private BigDecimal totalAmount;     // 总金额
```

**精度说明：**
- `precision = 10`：总共 10 位数字
- `scale = 2`：小数点后 2 位
- 范围：-99,999,999.99 到 99,999,999.99

---

### 业务方法：金额计算

```java
public void calculateTotalAmount() {
    this.totalAmount = this.subtotal
        .add(this.deliveryFee)
        .add(this.tax);
}
```

**使用示例：**
```java
Order order = new Order();
order.setSubtotal(new BigDecimal("50.00"));     // 商品 $50
order.setDeliveryFee(new BigDecimal("5.00"));   // 配送 $5
order.setTax(new BigDecimal("4.50"));           // 税 $4.5

order.calculateTotalAmount();
System.out.println(order.getTotalAmount());     // $59.50
```

**为什么不用 double？**
```java
// ❌ double 有精度问题
double total = 0.1 + 0.2;  // 0.30000000000000004

// ✅ BigDecimal 精确
BigDecimal total = new BigDecimal("0.1").add(new BigDecimal("0.2"));
// 0.3
```

---

## 🎯 业务逻辑方法

### 1. 检查是否可取消

```java
public boolean isCancellable() {
    return this.status == OrderStatus.PENDING || 
           this.status == OrderStatus.CONFIRMED;
}
```

**使用场景：**
```java
if (order.isCancellable()) {
    order.setStatus(OrderStatus.CANCELLED);
} else {
    throw new BusinessException("订单已开始制作，无法取消");
}
```

---

### 2. 检查是否完成

```java
public boolean isCompleted() {
    return this.status == OrderStatus.DELIVERED || 
           this.status == OrderStatus.CANCELLED;
}
```

---

### 3. 检查是否进行中

```java
public boolean isInProgress() {
    return this.status == OrderStatus.PREPARING || 
           this.status == OrderStatus.READY_FOR_PICKUP ||
           this.status == OrderStatus.PICKED_UP ||
           this.status == OrderStatus.IN_TRANSIT;
}
```

**最佳实践：**
- 简单的业务判断可以放在实体类
- 复杂的业务逻辑放在 Service 层

---

## 📐 索引策略

```java
@Table(name = "orders", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_restaurant_id", columnList = "restaurant_id"),
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
```

**索引用途：**

| 索引 | 使用场景 | 查询示例 |
|------|---------|---------|
| `idx_customer_id` | 查询用户的所有订单 | `WHERE customer_id = ?` |
| `idx_restaurant_id` | 查询餐厅的所有订单 | `WHERE restaurant_id = ?` |
| `idx_order_number` | 通过订单号查询 | `WHERE order_number = ?` |
| `idx_status` | 查询待处理订单 | `WHERE status = 'PENDING'` |
| `idx_created_at` | 按时间范围查询 | `WHERE created_at > ?` |

**性能提升：**
- 无索引：全表扫描（慢）
- 有索引：索引查找（快 100x+）

---

## 🔄 完整的订单生命周期

```java
// 1. 创建订单
Order order = new Order();
order.setCustomer(user);
order.setRestaurant(restaurant);
order.setDeliveryAddress(address);
order.setOrderNumber("ORD-2026-001");
order.setStatus(OrderStatus.PENDING);
order.setPaymentStatus(PaymentStatus.PENDING);

// 2. 计算金额
order.setSubtotal(new BigDecimal("50.00"));
order.setDeliveryFee(new BigDecimal("5.00"));
order.setTax(new BigDecimal("4.50"));
order.calculateTotalAmount();  // $59.50

orderRepository.save(order);

// 3. 餐厅确认
order.setStatus(OrderStatus.CONFIRMED);

// 4. 开始准备
order.setStatus(OrderStatus.PREPARING);

// 5. 准备完成
order.setStatus(OrderStatus.READY_FOR_PICKUP);

// 6. 司机取餐
order.setStatus(OrderStatus.PICKED_UP);

// 7. 配送中
order.setStatus(OrderStatus.IN_TRANSIT);

// 8. 送达
order.setStatus(OrderStatus.DELIVERED);
order.setActualDelivery(LocalDateTime.now());
```

---

## 📊 数据库表结构

```sql
CREATE TABLE orders (
    -- 主键
    id BIGSERIAL PRIMARY KEY,
    
    -- 外键（3个 @ManyToOne 关系）
    customer_id BIGINT NOT NULL REFERENCES users(id),
    restaurant_id BIGINT NOT NULL REFERENCES restaurants(id),
    delivery_address_id BIGINT NOT NULL REFERENCES addresses(id),
    
    -- 订单信息
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    
    -- 金额信息
    subtotal DECIMAL(10,2) NOT NULL,
    delivery_fee DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    
    -- 支付信息
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    
    -- 其他
    special_instructions TEXT,
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    -- 索引
    INDEX idx_customer_id (customer_id),
    INDEX idx_restaurant_id (restaurant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

---

## ✅ 核心知识点总结

| 知识点 | 说明 | User文档 |
|--------|------|---------|
| `@ManyToOne` | 多对一关系（核心） | ❌ 未涵盖 |
| `@JoinColumn` | 外键配置 | ❌ 未涵盖 |
| `FetchType.LAZY` | 延迟加载 | ⚠️ 简单提及 |
| 多枚举管理 | 3个枚举类型 | ⚠️ 只展示1个 |
| `BigDecimal` 计算 | 金额加法 | ⚠️ 只展示字段 |
| 业务方法 | 实体类中的判断逻辑 | ❌ 未涵盖 |
| 复杂索引策略 | 5个索引 | ⚠️ 只展示基础 |

---

## 🎓 与 User 实体的对比

| 特性 | User | Order |
|------|------|-------|
| 关系数量 | 0 | 3 个 @ManyToOne |
| 枚举数量 | 1 | 3 |
| 金额字段 | 0 | 4 |
| 业务方法 | 0 | 3 |
| 索引数量 | 3 | 5 |
| 复杂度 | ⭐⭐ | ⭐⭐⭐⭐⭐ |

**Order 是学习 JPA 关系映射的最佳示例！** 🚀

---

## 💡 最佳实践

1. ✅ 外键字段统一命名：`{entity}_id`
2. ✅ 关系映射默认使用 `LAZY` 加载
3. ✅ 金额字段必须用 `BigDecimal`
4. ✅ 为常查询字段建立索引
5. ✅ 枚举使用 `EnumType.STRING`
6. ✅ 简单业务逻辑可以放实体类
7. ✅ 订单号使用唯一约束

---

**下一步：学习 `@OneToMany` 双向关系（Order → OrderItem）** 📚
