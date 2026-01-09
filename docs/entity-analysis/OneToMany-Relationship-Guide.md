# @OneToMany 双向关系详解

## 📋 概述

`@OneToMany` 是 JPA 中最常用的关系映射之一，表示**一对多**的双向关系。

**业务场景：**
- 一个用户 → 多个订单
- 一个订单 → 多个订单项
- 一个餐厅 → 多个菜单项

**当前项目状态：** 实体类中只定义了 `@ManyToOne`（单向），未定义 `@OneToMany`（双向）

---

## 🔥 核心概念

### 单向 vs 双向关系

#### ❌ 单向关系（当前实现）
```java
// Order.java - 只有这一侧
@ManyToOne
@JoinColumn(name = "customer_id")
private User customer;

// User.java - 没有定义关系
// 无法直接获取用户的所有订单
```

**缺点：**
```java
User user = userRepository.findById(1L).get();
// ❌ 无法这样获取订单
List<Order> orders = user.getOrders();  // 编译错误！没有这个方法

// ❌ 只能通过 OrderRepository 查询
List<Order> orders = orderRepository.findByCustomer(user);
```

---

#### ✅ 双向关系（推荐）
```java
// Order.java - "多"的一侧
@ManyToOne
@JoinColumn(name = "customer_id")
private User customer;

// User.java - "一"的一侧
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

**优点：**
```java
User user = userRepository.findById(1L).get();
// ✅ 可以直接获取订单
List<Order> orders = user.getOrders();
```

---

## 💻 实战示例

### 示例 1：用户与订单（User ↔ Order）

#### Step 1: Order 侧（已有）
```java
// Order.java
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;  // 多个订单 → 一个用户
}
```

---

#### Step 2: User 侧（需要添加）
```java
// User.java
import java.util.ArrayList;
import java.util.List;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ... 其他字段 ...
    
    /**
     * 用户的所有订单 - 一个用户可以有多个订单
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
}
```

---

### 关键参数详解

#### 1. `mappedBy = "customer"`

**最重要的参数！**

```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;
```

**作用：**
- 告诉 JPA："这个关系由 Order 实体的 `customer` 字段维护"
- **避免生成中间表**
- 表示这是关系的"从"侧（非拥有方）

**❌ 不使用 mappedBy 的后果：**
```java
@OneToMany  // 忘记写 mappedBy
private List<Order> orders;

// Hibernate 会生成额外的中间表：
CREATE TABLE user_orders (
    user_id BIGINT,
    orders_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (orders_id) REFERENCES orders(id)
);
// ❌ 不需要这个表！外键已经在 orders 表中了！
```

**✅ 使用 mappedBy：**
```java
@OneToMany(mappedBy = "customer")
private List<Order> orders;

// ✅ 不生成中间表，使用 orders 表中的 customer_id 外键
```

---

#### 2. `cascade = CascadeType.ALL`

**级联操作 - 父子同步**

```java
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
private List<Order> orders;
```

**级联类型：**

| 类型 | 说明 | 示例 |
|------|------|------|
| `PERSIST` | 保存父实体时保存子实体 | 保存用户时保存订单 |
| `MERGE` | 更新父实体时更新子实体 | 更新用户时更新订单 |
| `REMOVE` | 删除父实体时删除子实体 | 删除用户时删除订单 |
| `REFRESH` | 刷新父实体时刷新子实体 | - |
| `DETACH` | 分离父实体时分离子实体 | - |
| `ALL` | 包含以上所有 | **最常用** |

**实际应用：**
```java
// 创建用户和订单
User user = new User();
user.setEmail("john@example.com");

Order order1 = new Order();
order1.setCustomer(user);

Order order2 = new Order();
order2.setCustomer(user);

user.getOrders().add(order1);
user.getOrders().add(order2);

userRepository.save(user);
// ✅ cascade = ALL，自动保存 order1 和 order2
```

**⚠️ 注意：User-Order 场景不适合 REMOVE 级联！**
```java
// ❌ 删除用户会删除所有历史订单（数据丢失）
@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)

// ✅ 推荐：不级联删除，或使用软删除
@OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
```

---

#### 3. `orphanRemoval = true`

**孤儿删除 - 自动清理**

```java
@OneToMany(mappedBy = "customer", orphanRemoval = true)
private List<Order> orders;
```

**作用：**
从集合中移除的对象会被自动删除

**示例：**
```java
User user = userRepository.findById(1L).get();
Order order = user.getOrders().get(0);

// 从集合中移除
user.getOrders().remove(order);

userRepository.save(user);
// ✅ orphanRemoval = true，order 会从数据库删除
```

**vs 不使用 orphanRemoval：**
```java
@OneToMany(mappedBy = "customer")  // 没有 orphanRemoval
private List<Order> orders;

user.getOrders().remove(order);
userRepository.save(user);
// ❌ order 仍在数据库中，但 customer_id 变为 NULL
```

**⚠️ User-Order 场景通常不用 orphanRemoval**
- 订单是重要业务数据，不应随意删除

---

### 示例 2：订单与订单项（Order ↔ OrderItem）

**最典型的父子关系！**

#### Order 侧
```java
// Order.java
@Entity
public class Order {
    @Id
    private Long id;
    
    /**
     * 订单包含的所有商品
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    // 便捷方法：添加订单项
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);  // 同步双向关系
    }
    
    // 便捷方法：移除订单项
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);  // 同步双向关系
    }
}
```

#### OrderItem 侧
```java
// OrderItem.java
@Entity
public class OrderItem {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;  // 多个订单项 → 一个订单
}
```

**使用示例：**
```java
Order order = new Order();

OrderItem item1 = new OrderItem();
item1.setQuantity(2);
item1.setUnitPrice(new BigDecimal("10.00"));

OrderItem item2 = new OrderItem();
item2.setQuantity(1);
item2.setUnitPrice(new BigDecimal("15.00"));

// 添加订单项
order.addOrderItem(item1);
order.addOrderItem(item2);

orderRepository.save(order);
// ✅ cascade = ALL，自动保存 item1 和 item2

// 删除订单项
order.removeOrderItem(item1);
orderRepository.save(order);
// ✅ orphanRemoval = true，item1 从数据库删除
```

**为什么这里适合使用 orphanRemoval？**
- 订单项是订单的一部分
- 订单删除 → 订单项应该删除
- 订单项没有独立业务意义

---

### 示例 3：餐厅与菜单项（Restaurant ↔ MenuItem）

#### Restaurant 侧
```java
// Restaurant.java
@Entity
public class Restaurant {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuItem> menuItems = new ArrayList<>();
}
```

#### MenuItem 侧
```java
// MenuItem.java
@Entity
public class MenuItem {
    @Id
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
}
```

---

## 🎯 双向关系最佳实践

### 1. 双向同步（重要！）

**问题：**
```java
Order order = new Order();
OrderItem item = new OrderItem();

item.setOrder(order);  // 只设置一侧
// ❌ order.getOrderItems() 为空！关系不同步
```

**解决方案：便捷方法**
```java
// Order.java
public void addOrderItem(OrderItem item) {
    this.orderItems.add(item);  // 设置集合侧
    item.setOrder(this);         // 设置引用侧
}

public void removeOrderItem(OrderItem item) {
    this.orderItems.remove(item);
    item.setOrder(null);
}

// 使用
order.addOrderItem(item);  // ✅ 双向关系自动同步
```

---

### 2. 初始化集合

```java
// ✅ 推荐：字段声明时初始化
@OneToMany(mappedBy = "customer")
private List<Order> orders = new ArrayList<>();

// ❌ 不推荐：不初始化
@OneToMany(mappedBy = "customer")
private List<Order> orders;  // 可能 NullPointerException
```

---

### 3. equals() 和 hashCode() 注意事项

**问题：**
```java
@Data  // 包含 equals() 和 hashCode()
@Entity
public class User {
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}

// 可能导致递归：
user.equals(anotherUser)
  → orders.equals(anotherOrders)
    → order.equals(anotherOrder)
      → customer.equals(anotherCustomer)  // 递归！
```

**解决方案：**
```java
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // 只用指定字段
public class User {
    @Id
    @EqualsAndHashCode.Include  // 只用 ID
    private Long id;
    
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;  // 不参与 equals()
}
```

---

### 4. 避免 toString() 递归

```java
@Data  // 包含 toString()
@Entity
public class User {
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}

System.out.println(user);
// ❌ 可能无限递归：User → Order → User → Order...
```

**解决方案：**
```java
@Entity
@Getter
@Setter
@ToString(exclude = "orders")  // 排除集合字段
public class User {
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}
```

---

## 📊 性能优化

### 1. N+1 查询问题

**问题：**
```java
List<User> users = userRepository.findAll();  // 1次查询

for (User user : users) {
    List<Order> orders = user.getOrders();    // N次查询（每个用户1次）
    System.out.println(orders.size());
}
// 总共 1 + N 次查询！
```

**解决方案：JOIN FETCH**
```java
// UserRepository.java
@Query("SELECT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithOrders();

// 只执行1次查询（带 JOIN）
List<User> users = userRepository.findAllWithOrders();
```

---

### 2. 按需加载

```java
// ✅ 默认 LAZY，不加载订单
User user = userRepository.findById(1L).get();

// ✅ 需要时才加载
if (needOrders) {
    List<Order> orders = user.getOrders();  // 此时才查询
}
```

---

## 🔄 完整示例：为项目添加双向关系

### User.java（修改）
```java
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"orders", "restaurants"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    // ... 原有字段 ...
    
    /**
     * 用户的订单（作为客户）
     */
    @OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Order> orders = new ArrayList<>();
    
    /**
     * 用户拥有的餐厅（作为餐厅老板）
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Restaurant> restaurants = new ArrayList<>();
    
    /**
     * 用户的地址
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();
}
```

### Order.java（修改）
```java
@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString(exclude = "orderItems")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ... 原有字段 ...
    
    /**
     * 订单包含的商品
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
    
    // 便捷方法
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }
    
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }
}
```

---

## ✅ 使用场景总结

| 关系 | cascade | orphanRemoval | 原因 |
|------|---------|---------------|------|
| User → Order | PERSIST, MERGE | ❌ false | 订单是重要数据，不应随用户删除 |
| User → Address | ALL | ✅ true | 地址是用户的一部分 |
| User → Restaurant | ALL | ✅ true | 餐厅属于老板 |
| Order → OrderItem | ALL | ✅ true | 订单项依赖订单 |
| Restaurant → MenuItem | ALL | ✅ true | 菜单项依赖餐厅 |

---

## 🎓 核心知识点总结

1. **`mappedBy`** - 必须指定，避免中间表
2. **`cascade`** - 控制级联操作范围
3. **`orphanRemoval`** - 是否自动删除孤儿对象
4. **双向同步** - 提供便捷方法维护关系
5. **初始化集合** - 避免 NullPointerException
6. **性能优化** - JOIN FETCH 解决 N+1 问题
7. **equals/hashCode** - 避免递归，只用 ID

**@OneToMany 是双向关系的核心，掌握它是 JPA 进阶的关键！** 🚀
