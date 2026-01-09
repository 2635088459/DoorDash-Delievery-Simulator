# User 实体类详细解析

## 📋 目录
1. [类概述](#类概述)
2. [完整代码](#完整代码)
3. [注解详解](#注解详解)
4. [字段分析](#字段分析)
5. [知识点总结](#知识点总结)
6. [数据库映射](#数据库映射)
7. [最佳实践](#最佳实践)

---

## 类概述

**类名**: `User`  
**包路径**: `com.shydelivery.doordashsimulator.entity`  
**业务用途**: 存储所有用户账户（客户、餐厅老板、配送司机）  
**数据库表**: `users`

### 业务场景
这是系统中的**核心实体类**，支持三种用户角色：
- 👤 **CUSTOMER** - 普通客户（下单、评价）
- 🏪 **RESTAURANT_OWNER** - 餐厅老板（管理餐厅和菜单）
- 🚗 **DRIVER** - 配送司机（接单配送）

---

## 完整代码

```java
package com.shydelivery.doordashsimulator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity - Stores all user accounts (customers, restaurant owners, delivery drivers)
 * This is the core entity that supports multiple user roles in the system
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone_number"),
    @Index(name = "idx_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    /**
     * User role: CUSTOMER, RESTAURANT_OWNER, DRIVER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User role enumeration
     */
    public enum UserRole {
        CUSTOMER,
        RESTAURANT_OWNER,
        DRIVER
    }
}
```

---

## 注解详解

### 1️⃣ 类级别注解

#### `@Entity`
```java
@Entity
```

**知识点：JPA 实体标识**

- **作用**：告诉 JPA/Hibernate 这是一个需要持久化到数据库的实体类
- **必需性**：每个 JPA 实体类都必须有此注解
- **背后原理**：
  - JPA 会扫描所有带 `@Entity` 的类
  - 为每个实体类创建 `EntityManager` 管理
  - 自动创建对应的数据库表（当使用 `ddl-auto: update`）

**常见错误**：
```java
// ❌ 错误：忘记添加 @Entity
public class User {
    // Hibernate 不会识别此类
}

// ✅ 正确
@Entity
public class User {
    // Hibernate 会自动管理
}
```

---

#### `@Table`
```java
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_phone", columnList = "phone_number"),
    @Index(name = "idx_role", columnList = "role")
})
```

**知识点：表级配置和索引优化**

##### 参数详解：

1. **`name = "users"`** - 指定表名
   - 不指定时，默认使用类名作为表名
   - 推荐显式指定，避免命名冲突

2. **`indexes = {...}`** - 定义索引

**索引知识点深入讲解：**

##### 什么是索引？
索引就像书的目录，帮助数据库快速查找数据。

##### 为什么需要索引？

**没有索引的查询（全表扫描）：**
```sql
-- 假设 users 表有 100万 条记录
SELECT * FROM users WHERE email = 'john@example.com';
-- 需要扫描 100万 行数据 ❌ 慢！
```

**有索引的查询（索引查找）：**
```sql
-- 使用 idx_email 索引
SELECT * FROM users WHERE email = 'john@example.com';
-- 只需要查找索引树，时间复杂度 O(log n) ✅ 快！
```

##### 本项目中的索引策略：

**索引1：`idx_email`**
```java
@Index(name = "idx_email", columnList = "email")
```
- **使用场景**：用户登录
  ```java
  // UserRepository
  User findByEmail(String email);  // 走索引，快速查找
  ```
- **查询优化**：登录查询从 O(n) 降到 O(log n)

**索引2：`idx_phone`**
```java
@Index(name = "idx_phone", columnList = "phone_number")
```
- **使用场景**：手机号验证、重复检查
  ```java
  boolean existsByPhoneNumber(String phoneNumber);  // 快速检查重复
  ```

**索引3：`idx_role`**
```java
@Index(name = "idx_role", columnList = "role")
```
- **使用场景**：按角色查询用户列表
  ```java
  List<User> findByRole(UserRole role);  // 查找所有司机
  ```

##### 索引的代价：
- ✅ 优点：查询速度快
- ❌ 缺点：
  - 占用额外存储空间
  - 插入/更新/删除时需要更新索引（稍慢）
  
**最佳实践**：只为**频繁查询**的字段建立索引

---

#### `@Data`
```java
@Data
```

**知识点：Lombok 自动代码生成**

- **来源**：Lombok 库（不是 JPA 标准）
- **功能**：自动生成以下方法
  ```java
  // 相当于手动写了这些方法：
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  // ... 所有字段的 getter/setter
  
  @Override
  public String toString() { /* ... */ }
  
  @Override
  public boolean equals(Object o) { /* ... */ }
  
  @Override
  public int hashCode() { /* ... */ }
  ```

**减少代码量统计**：
- 手动写：约 200 行代码
- 使用 @Data：1 行注解
- **节省代码：99.5%**

**注意事项**：
```java
// ⚠️ 警告：@Data 在实体类中可能导致问题

// 1. equals/hashCode 包含所有字段，可能导致递归
@Entity
@Data
public class User {
    @OneToMany
    private List<Order> orders;  // equals() 会递归调用 Order.equals()
}

// 更好的做法：只用 ID 作为 equals/hashCode 依据
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @EqualsAndHashCode.Include
    private Long id;
    
    @OneToMany
    private List<Order> orders;  // 不包含在 equals() 中
}
```

**本项目使用 @Data 的原因**：
- 目前实体类中没有定义双向关系
- 简化开发，快速原型
- 生产环境建议优化为 `@Getter` + `@Setter`

---

#### `@NoArgsConstructor` 和 `@AllArgsConstructor`
```java
@NoArgsConstructor   // 无参构造函数
@AllArgsConstructor  // 全参构造函数
```

**知识点：JPA 实体的构造函数要求**

##### `@NoArgsConstructor` - 无参构造函数

**为什么需要？**
```java
// JPA/Hibernate 内部工作原理：
User user = entityManager.find(User.class, 1L);

// Hibernate 的实现步骤：
// 1. 调用无参构造函数创建对象
User user = new User();  // ✅ 需要无参构造函数

// 2. 使用反射设置字段值
field.set(user, "john@example.com");
```

**如果没有无参构造函数会怎样？**
```java
@Entity
public class User {
    // ❌ 只定义了有参构造函数
    public User(String email) {
        this.email = email;
    }
}

// 运行时错误：
// org.hibernate.InstantiationException: No default constructor for entity: User
```

##### `@AllArgsConstructor` - 全参构造函数

**使用场景**：
```java
// 方便测试和快速创建对象
User user = new User(
    null,                           // id (数据库自动生成)
    "john@example.com",             // email
    "hashedPassword",               // password
    "John",                         // firstName
    "Doe",                          // lastName
    "1234567890",                   // phoneNumber
    UserRole.CUSTOMER,              // role
    true,                           // isActive
    null,                           // createdAt (自动生成)
    null                            // updatedAt (自动生成)
);
```

**更好的做法（Builder 模式）**：
```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // 添加 Builder 支持
public class User {
    // ...
}

// 使用 Builder 创建对象（更清晰）
User user = User.builder()
    .email("john@example.com")
    .password("hashedPassword")
    .firstName("John")
    .lastName("Doe")
    .phoneNumber("1234567890")
    .role(UserRole.CUSTOMER)
    .isActive(true)
    .build();
```

---

### 2️⃣ 字段级别注解

#### `@Id` + `@GeneratedValue`
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**知识点：主键生成策略**

##### `@Id` - 主键标识
- 每个实体类必须有一个主键
- 唯一标识一条记录

##### `@GeneratedValue` - 主键生成策略

**四种策略对比：**

| 策略 | 说明 | 数据库支持 | 性能 | 推荐度 |
|------|------|-----------|------|--------|
| **IDENTITY** | 数据库自增 | MySQL, PostgreSQL | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| AUTO | JPA自动选择 | 所有 | ⭐⭐⭐ | ⭐⭐⭐ |
| SEQUENCE | 使用序列 | Oracle, PostgreSQL | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| TABLE | 专用表生成 | 所有 | ⭐⭐ | ⭐⭐ |

**IDENTITY 策略详解（本项目使用）：**

```sql
-- PostgreSQL 实现
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,  -- 相当于 BIGINT AUTO_INCREMENT
    email VARCHAR(255)
);

-- 插入数据时
INSERT INTO users (email, password, ...) 
VALUES ('john@example.com', 'hash', ...);
-- id 自动生成为 1, 2, 3, ...
```

**Java 代码中的使用：**
```java
User user = new User();
user.setEmail("john@example.com");
// 注意：id 不需要设置，数据库会自动生成

userRepository.save(user);

System.out.println(user.getId());  // 输出：1（数据库自动生成）
```

**为什么用 Long 而不是 Integer？**
```java
// Integer 范围：-2,147,483,648 到 2,147,483,647
// 约 21 亿条记录

// Long 范围：-9,223,372,036,854,775,808 到 9,223,372,036,854,775,807
// 约 922 亿亿条记录 ✅ 永远不会用完
```

---

#### `@Column` - 字段映射配置

##### 示例1：唯一约束字段
```java
@Column(nullable = false, unique = true, length = 255)
private String email;
```

**参数详解：**
- `nullable = false`：非空约束（SQL: `NOT NULL`）
- `unique = true`：唯一约束（SQL: `UNIQUE`）
- `length = 255`：字符串最大长度（SQL: `VARCHAR(255)`）

**生成的 SQL：**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    ...
);

-- 自动创建唯一索引
CREATE UNIQUE INDEX uk_email ON users(email);
```

**业务含义**：
- ✅ 每个邮箱只能注册一次
- ✅ 邮箱不能为空
- ✅ 登录时可以快速查找（利用唯一索引）

**插入数据测试：**
```java
// 第一次插入 ✅ 成功
User user1 = new User();
user1.setEmail("john@example.com");
userRepository.save(user1);

// 第二次插入相同邮箱 ❌ 失败
User user2 = new User();
user2.setEmail("john@example.com");  // 相同邮箱
userRepository.save(user2);
// 抛出异常：
// org.postgresql.util.PSQLException: ERROR: duplicate key value 
// violates unique constraint "uk_email"
```

---

##### 示例2：字段名映射（驼峰 → 下划线）
```java
@Column(name = "first_name", nullable = false, length = 100)
private String firstName;
```

**命名规范转换：**
```
Java (驼峰命名)     →    数据库 (下划线命名)
firstName          →    first_name
lastName           →    last_name
phoneNumber        →    phone_number
isActive           →    is_active
createdAt          →    created_at
```

**为什么要显式指定 name？**

```java
// 1. 不指定（依赖 Hibernate 自动映射）
@Column(nullable = false)
private String firstName;
// Hibernate 可能映射为：first_name 或 firstName（不确定）

// 2. 显式指定（推荐） ✅
@Column(name = "first_name", nullable = false)
private String firstName;
// 确定映射为：first_name
```

**最佳实践：**
- Java 代码：使用驼峰命名（firstName）
- 数据库：使用下划线命名（first_name）
- 通过 `name` 属性明确映射关系

---

##### 示例3：密码字段（安全考虑）
```java
@Column(nullable = false, length = 255)
private String password;
```

**安全知识点：**

⚠️ **永远不要存储明文密码！**

**正确的密码处理流程：**

1. **注册时加密：**
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserService {
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public User register(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        
        // ✅ 存储加密后的密码
        String hashedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(hashedPassword);  // 存储：$2a$10$N9qo8...
        
        return userRepository.save(user);
    }
}
```

2. **登录时验证：**
```java
public boolean login(String email, String rawPassword) {
    User user = userRepository.findByEmail(email);
    
    // ✅ 比对加密密码
    return passwordEncoder.matches(rawPassword, user.getPassword());
}
```

**密码存储示例：**
```
原始密码：       "mySecretPassword123"
加密后存储：     "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
                 ↑ 60个字符，所以 length = 255 足够
```

**为什么是 255？**
- BCrypt 加密结果：60个字符
- 其他算法可能更长
- 255 是安全的冗余长度

---

#### `@Enumerated` - 枚举类型映射
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 50)
private UserRole role;
```

**知识点：枚举映射策略对比**

##### 策略1：EnumType.STRING（推荐）✅
```java
@Enumerated(EnumType.STRING)
private UserRole role;
```

**数据库存储：**
```sql
-- 存储枚举的字符串值
INSERT INTO users (role) VALUES ('CUSTOMER');
INSERT INTO users (role) VALUES ('RESTAURANT_OWNER');
INSERT INTO users (role) VALUES ('DRIVER');
```

**优点：**
- ✅ 可读性强（直接看懂是什么角色）
- ✅ 顺序改变不影响数据
- ✅ 添加新枚举不影响现有数据

**示例场景：**
```java
// 初始枚举定义
public enum UserRole {
    CUSTOMER,           // 存储为 "CUSTOMER"
    RESTAURANT_OWNER,   // 存储为 "RESTAURANT_OWNER"
    DRIVER              // 存储为 "DRIVER"
}

// 后来添加新角色（没问题）✅
public enum UserRole {
    CUSTOMER,           // 仍然是 "CUSTOMER"
    RESTAURANT_OWNER,   // 仍然是 "RESTAURANT_OWNER"
    ADMIN,              // 新增，存储为 "ADMIN"
    DRIVER              // 仍然是 "DRIVER"
}
// 数据库中的数据不受影响！
```

---

##### 策略2：EnumType.ORDINAL（不推荐）❌
```java
@Enumerated(EnumType.ORDINAL)
private UserRole role;
```

**数据库存储：**
```sql
-- 存储枚举的序号（从0开始）
INSERT INTO users (role) VALUES (0);  -- CUSTOMER
INSERT INTO users (role) VALUES (1);  -- RESTAURANT_OWNER
INSERT INTO users (role) VALUES (2);  -- DRIVER
```

**危险示例：**
```java
// 初始枚举定义
public enum UserRole {
    CUSTOMER,           // 序号 = 0
    RESTAURANT_OWNER,   // 序号 = 1
    DRIVER              // 序号 = 2
}
// 数据库中：用户角色 = 1 表示 RESTAURANT_OWNER

// 后来调整顺序（灾难）❌
public enum UserRole {
    CUSTOMER,           // 序号 = 0
    ADMIN,              // 序号 = 1（新增）
    RESTAURANT_OWNER,   // 序号 = 2（变了！）
    DRIVER              // 序号 = 3
}
// 数据库中：用户角色 = 1 现在表示 ADMIN
// 所有 RESTAURANT_OWNER 用户变成了 ADMIN！！！💥
```

**结论：永远使用 `EnumType.STRING`**

---

##### 数据库约束（CHECK）
```sql
-- Hibernate 自动生成 CHECK 约束
ALTER TABLE users 
ADD CONSTRAINT check_role 
CHECK (role IN ('CUSTOMER', 'RESTAURANT_OWNER', 'DRIVER'));

-- 尝试插入无效值会失败 ✅
INSERT INTO users (role) VALUES ('INVALID_ROLE');
-- ERROR: new row for relation "users" violates check constraint "check_role"
```

---

#### 默认值设置
```java
@Column(name = "is_active", nullable = false)
private Boolean isActive = true;
```

**知识点：JPA 中的默认值**

**两种设置方式：**

##### 方式1：Java 字段初始化（本项目使用）
```java
private Boolean isActive = true;  // 在 Java 层面设置默认值
```

**优点：**
- ✅ 清晰明确
- ✅ 在任何构造函数中都生效

**执行流程：**
```java
User user = new User();  // isActive 自动为 true
user.setEmail("john@example.com");
userRepository.save(user);

// 生成 SQL：
INSERT INTO users (email, is_active) VALUES ('john@example.com', true);
```

---

##### 方式2：数据库级别默认值
```java
@Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
private Boolean isActive;
```

**生成 SQL：**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

**缺点：**
- ❌ 只在数据库直接插入时生效
- ❌ JPA 保存对象时可能不触发（因为 JPA 会显式设置所有字段）

**最佳实践：两者结合**
```java
@Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
private Boolean isActive = true;  // Java 和数据库都设置默认值
```

---

#### 时间戳自动管理
```java
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at", nullable = false)
private LocalDateTime updatedAt;
```

**知识点：Hibernate 时间戳注解**

##### `@CreationTimestamp` - 创建时间

**作用：**
- 插入记录时，自动设置为当前时间
- 后续更新不会改变（`updatable = false`）

**执行流程：**
```java
User user = new User();
user.setEmail("john@example.com");
// 不需要手动设置 createdAt

userRepository.save(user);
// Hibernate 自动设置：
// user.setCreatedAt(LocalDateTime.now());

System.out.println(user.getCreatedAt());
// 输出：2026-01-09T12:30:45
```

**生成 SQL：**
```sql
INSERT INTO users (email, created_at, updated_at) 
VALUES ('john@example.com', '2026-01-09 12:30:45', '2026-01-09 12:30:45');
```

---

##### `@UpdateTimestamp` - 更新时间

**作用：**
- 插入记录时，自动设置为当前时间
- **每次更新时，自动更新为当前时间**

**执行流程：**
```java
// 1. 创建用户
User user = new User();
user.setEmail("john@example.com");
userRepository.save(user);
// createdAt = 2026-01-09 12:30:45
// updatedAt = 2026-01-09 12:30:45

// 2. 更新用户（1小时后）
user.setFirstName("John Updated");
userRepository.save(user);
// createdAt = 2026-01-09 12:30:45 (不变)
// updatedAt = 2026-01-09 13:30:45 (自动更新) ✅
```

**业务价值：**
- 📅 审计追踪：知道记录何时创建、何时最后修改
- 🔍 调试：排查数据变化时间
- 📊 分析：用户活跃度统计

---

##### LocalDateTime vs Timestamp

**Java 时间类型选择：**

| 类型 | 说明 | 推荐度 |
|------|------|--------|
| **LocalDateTime** | Java 8+ 新API | ⭐⭐⭐⭐⭐ |
| Date | 老旧API | ⭐ |
| Timestamp | SQL 类型 | ⭐⭐ |

**LocalDateTime 优势：**
```java
// ✅ 现代 API（推荐）
private LocalDateTime createdAt;

// 使用方便
LocalDateTime now = LocalDateTime.now();
LocalDateTime tomorrow = now.plusDays(1);
LocalDateTime lastWeek = now.minusWeeks(1);

// 格式化
String formatted = createdAt.format(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
);

// ❌ 老旧 API（不推荐）
private Date createdAt;

// 使用麻烦
Date now = new Date();
Calendar cal = Calendar.getInstance();
cal.setTime(now);
cal.add(Calendar.DAY_OF_MONTH, 1);
Date tomorrow = cal.getTime();  // 繁琐！
```

---

### 3️⃣ 内部枚举类

```java
/**
 * User role enumeration
 */
public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DRIVER
}
```

**知识点：枚举类设计**

##### 为什么用枚举而不是字符串？

**❌ 不好的做法（使用字符串）：**
```java
@Column(nullable = false)
private String role;  // 存储："customer", "restaurant_owner", "driver"

// 问题：
// 1. 拼写错误
user.setRole("CUSTMER");  // 编译通过，运行时出错 ❌

// 2. 不统一
user.setRole("customer");   // 小写
user.setRole("CUSTOMER");   // 大写
user.setRole("Customer");   // 混合
// 都能存入数据库，导致数据不一致！

// 3. 代码提示差
user.setRole("???");  // IDE 无法提示有哪些角色
```

**✅ 好的做法（使用枚举）：**
```java
public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DRIVER
}

@Enumerated(EnumType.STRING)
private UserRole role;

// 优点：
// 1. 类型安全
user.setRole(UserRole.CUSTOMER);  // ✅ 编译检查

// 2. IDE 自动提示
user.setRole(UserRole.);  // IDE 自动列出所有选项
                          // - CUSTOMER
                          // - RESTAURANT_OWNER
                          // - DRIVER

// 3. 统一性
// 数据库中只可能是："CUSTOMER", "RESTAURANT_OWNER", "DRIVER"

// 4. 重构友好
// 修改枚举名称，IDE 可以一键重构所有引用
```

---

##### 枚举类位置选择

**方式1：内部枚举（本项目使用）**
```java
@Entity
public class User {
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    // ✅ 嵌套在 User 类内部
    public enum UserRole {
        CUSTOMER,
        RESTAURANT_OWNER,
        DRIVER
    }
}

// 使用方式
User.UserRole role = User.UserRole.CUSTOMER;
```

**优点：**
- ✅ 紧密关联（角色只属于用户）
- ✅ 避免命名冲突

**缺点：**
- ❌ 其他类使用时需要写 `User.UserRole`（稍长）

---

**方式2：独立枚举类**
```java
// UserRole.java（独立文件）
public enum UserRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    DRIVER
}

// User.java
@Entity
public class User {
    @Enumerated(EnumType.STRING)
    private UserRole role;
}

// 使用方式
UserRole role = UserRole.CUSTOMER;
```

**优点：**
- ✅ 代码更清晰
- ✅ 多个类可以共享（如果需要）

**缺点：**
- ❌ 文件数量增加

**推荐：**
- 小项目/枚举只在一个类中使用：内部枚举
- 大项目/枚举被多个类共享：独立枚举

---

##### 枚举的高级用法

**添加业务方法：**
```java
public enum UserRole {
    CUSTOMER("普通客户", "C"),
    RESTAURANT_OWNER("餐厅老板", "R"),
    DRIVER("配送司机", "D");
    
    private final String displayName;
    private final String code;
    
    UserRole(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    // 业务方法：判断是否有管理权限
    public boolean hasManagementPermission() {
        return this == RESTAURANT_OWNER || this == DRIVER;
    }
}

// 使用
UserRole role = UserRole.CUSTOMER;
System.out.println(role.getDisplayName());  // "普通客户"
System.out.println(role.getCode());         // "C"
System.out.println(role.hasManagementPermission());  // false
```

---

## 知识点总结

### 核心JPA知识点（12个）

| 序号 | 知识点 | 注解/技术 | 掌握程度 |
|------|--------|-----------|---------|
| 1 | 实体类标识 | `@Entity` | ⭐⭐⭐⭐⭐ 必须 |
| 2 | 表映射配置 | `@Table` | ⭐⭐⭐⭐⭐ 必须 |
| 3 | 主键定义 | `@Id` | ⭐⭐⭐⭐⭐ 必须 |
| 4 | 主键生成策略 | `@GeneratedValue` | ⭐⭐⭐⭐⭐ 必须 |
| 5 | 字段映射 | `@Column` | ⭐⭐⭐⭐⭐ 必须 |
| 6 | 枚举映射 | `@Enumerated` | ⭐⭐⭐⭐ 重要 |
| 7 | 时间戳管理 | `@CreationTimestamp` / `@UpdateTimestamp` | ⭐⭐⭐⭐ 重要 |
| 8 | 索引优化 | `@Index` | ⭐⭐⭐⭐ 重要 |
| 9 | 约束定义 | `nullable`, `unique` | ⭐⭐⭐⭐⭐ 必须 |
| 10 | 无参构造函数 | `@NoArgsConstructor` | ⭐⭐⭐⭐⭐ 必须 |
| 11 | Lombok简化 | `@Data`, `@AllArgsConstructor` | ⭐⭐⭐⭐ 重要 |
| 12 | 枚举设计 | `enum` | ⭐⭐⭐⭐ 重要 |

---

### 数据库知识点（8个）

| 序号 | 知识点 | 说明 | 实际应用 |
|------|--------|------|---------|
| 1 | 主键约束 | PRIMARY KEY | `id BIGSERIAL PRIMARY KEY` |
| 2 | 非空约束 | NOT NULL | `email VARCHAR(255) NOT NULL` |
| 3 | 唯一约束 | UNIQUE | `email VARCHAR(255) UNIQUE` |
| 4 | 检查约束 | CHECK | `CHECK (role IN ('CUSTOMER', ...))` |
| 5 | 索引优化 | INDEX | `CREATE INDEX idx_email ON users(email)` |
| 6 | 默认值 | DEFAULT | `is_active BOOLEAN DEFAULT TRUE` |
| 7 | 自动更新 | ON UPDATE | `updated_at` 字段自动更新 |
| 8 | 字符长度 | VARCHAR(n) | `email VARCHAR(255)` |

---

### 安全知识点（3个）

| 序号 | 知识点 | 最佳实践 |
|------|--------|---------|
| 1 | 密码存储 | ✅ 加密存储（BCrypt）<br>❌ 明文存储 |
| 2 | 字段长度 | ✅ 255足够存储加密密码<br>❌ 60可能不够 |
| 3 | 唯一约束 | ✅ email/phone唯一<br>❌ 允许重复导致冲突 |

---

### 性能优化知识点（4个）

| 序号 | 知识点 | 说明 | 效果 |
|------|--------|------|------|
| 1 | 索引策略 | 为常查询字段建索引 | 查询速度 100x+ 提升 |
| 2 | 字段长度 | 精确控制 VARCHAR 长度 | 减少存储空间 |
| 3 | 主键类型 | 使用 BIGINT 而非 INTEGER | 支持海量数据 |
| 4 | 默认值 | Java 层面设置默认值 | 减少数据库交互 |

---

## 数据库映射

### 生成的 SQL（PostgreSQL）

```sql
-- 创建 users 表
CREATE TABLE users (
    -- 主键（自增）
    id BIGSERIAL PRIMARY KEY,
    
    -- 基本字段（带约束）
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    
    -- 枚举字段（带 CHECK 约束）
    role VARCHAR(50) NOT NULL 
        CHECK (role IN ('CUSTOMER', 'RESTAURANT_OWNER', 'DRIVER')),
    
    -- 布尔字段（带默认值）
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- 时间戳字段
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 创建索引（性能优化）
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_phone ON users(phone_number);
CREATE INDEX idx_role ON users(role);

-- 创建唯一索引（自动，因为 UNIQUE 约束）
CREATE UNIQUE INDEX uk_email ON users(email);
CREATE UNIQUE INDEX uk_phone ON users(phone_number);
```

---

### 表结构可视化

```
┌─────────────────────────────────────────────────────────┐
│                      users 表                            │
├──────────────┬─────────────────┬──────────────────────┤
│ 字段名        │ 数据类型         │ 约束                  │
├──────────────┼─────────────────┼──────────────────────┤
│ id           │ BIGSERIAL       │ PRIMARY KEY          │
│ email        │ VARCHAR(255)    │ NOT NULL, UNIQUE ⚡  │
│ password     │ VARCHAR(255)    │ NOT NULL             │
│ first_name   │ VARCHAR(100)    │ NOT NULL             │
│ last_name    │ VARCHAR(100)    │ NOT NULL             │
│ phone_number │ VARCHAR(20)     │ NOT NULL, UNIQUE ⚡  │
│ role         │ VARCHAR(50)     │ NOT NULL, CHECK ⚡   │
│ is_active    │ BOOLEAN         │ NOT NULL, DEFAULT    │
│ created_at   │ TIMESTAMP       │ NOT NULL             │
│ updated_at   │ TIMESTAMP       │ NOT NULL             │
└──────────────┴─────────────────┴──────────────────────┘

索引（⚡ 表示有索引）：
- idx_email (email)           → 登录查询优化
- idx_phone (phone_number)    → 手机验证优化
- idx_role (role)             → 角色筛选优化
- uk_email (email, UNIQUE)    → 唯一性保证
- uk_phone (phone_number, UNIQUE) → 唯一性保证
```

---

### 示例数据

```sql
-- 插入测试数据
INSERT INTO users (email, password, first_name, last_name, phone_number, role, is_active, created_at, updated_at)
VALUES 
    ('customer@example.com', 
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'John',
     'Doe',
     '1234567890',
     'CUSTOMER',
     true,
     '2026-01-09 12:00:00',
     '2026-01-09 12:00:00'),
     
    ('owner@restaurant.com',
     '$2a$10$abcdefghijklmnopqrstuvwxyz123456789ABCDEFGHIJKLMN',
     'Jane',
     'Smith',
     '0987654321',
     'RESTAURANT_OWNER',
     true,
     '2026-01-09 13:00:00',
     '2026-01-09 13:00:00'),
     
    ('driver@delivery.com',
     '$2a$10$zxcvbnmasdfghjklqwertyuiop123456789ZXCVBNM',
     'Mike',
     'Johnson',
     '5555555555',
     'DRIVER',
     true,
     '2026-01-09 14:00:00',
     '2026-01-09 14:00:00');

-- 查询结果
SELECT id, email, first_name, last_name, role, is_active 
FROM users;

/*
 id |        email         | first_name | last_name |       role        | is_active
----+----------------------+------------+-----------+-------------------+-----------
  1 | customer@example.com | John       | Doe       | CUSTOMER          | true
  2 | owner@restaurant.com | Jane       | Smith     | RESTAURANT_OWNER  | true
  3 | driver@delivery.com  | Mike       | Johnson   | DRIVER            | true
*/
```

---

## 最佳实践

### ✅ 推荐做法

1. **主键使用 BIGINT**
   ```java
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;  // ✅ 不用 Integer
   ```

2. **枚举使用 STRING 类型**
   ```java
   @Enumerated(EnumType.STRING)  // ✅ 不用 ORDINAL
   private UserRole role;
   ```

3. **显式指定字段名**
   ```java
   @Column(name = "first_name")  // ✅ 明确映射
   private String firstName;
   ```

4. **为常查询字段建索引**
   ```java
   @Table(indexes = {
       @Index(name = "idx_email", columnList = "email")  // ✅ 登录优化
   })
   ```

5. **密码加密存储**
   ```java
   // ✅ 使用 BCrypt
   String hashed = passwordEncoder.encode(rawPassword);
   user.setPassword(hashed);
   ```

6. **时间戳自动管理**
   ```java
   @CreationTimestamp  // ✅ 自动设置
   private LocalDateTime createdAt;
   ```

7. **必要字段加非空约束**
   ```java
   @Column(nullable = false)  // ✅ 数据完整性
   private String email;
   ```

8. **唯一字段加唯一约束**
   ```java
   @Column(unique = true)  // ✅ 防止重复
   private String email;
   ```

---

### ❌ 避免的错误

1. **忘记 @Entity 注解**
   ```java
   // ❌ Hibernate 无法识别
   public class User { }
   
   // ✅ 正确
   @Entity
   public class User { }
   ```

2. **忘记无参构造函数**
   ```java
   // ❌ JPA 无法实例化
   public class User {
       public User(String email) { }
   }
   
   // ✅ 添加 Lombok 注解
   @NoArgsConstructor
   public class User {
       public User(String email) { }
   }
   ```

3. **密码明文存储**
   ```java
   // ❌ 安全隐患
   user.setPassword("myPassword123");
   
   // ✅ 加密存储
   user.setPassword(passwordEncoder.encode("myPassword123"));
   ```

4. **使用 ORDINAL 枚举**
   ```java
   // ❌ 顺序改变会出错
   @Enumerated(EnumType.ORDINAL)
   
   // ✅ 使用字符串
   @Enumerated(EnumType.STRING)
   ```

5. **索引过多或过少**
   ```java
   // ❌ 没有索引（查询慢）
   @Table(name = "users")
   
   // ❌ 索引太多（插入慢）
   @Table(indexes = {
       @Index(...), @Index(...), @Index(...), /* 10+ 个索引 */
   })
   
   // ✅ 适当索引（常查询字段）
   @Table(indexes = {
       @Index(name = "idx_email", columnList = "email"),
       @Index(name = "idx_role", columnList = "role")
   })
   ```

---

## 扩展学习

### 相关实体类

User 实体与其他实体的关系：

1. **User → Address**（一对多）
   - 一个用户可以有多个地址
   
2. **User → Restaurant**（一对多）
   - 一个餐厅老板可以拥有多个餐厅
   
3. **User → Order**（一对多）
   - 一个客户可以下多个订单
   
4. **User ↔ Driver**（一对一）
   - 一个用户账号对应一个司机档案

### 下一步学习

1. 学习 **Repository 层**（数据访问）
2. 学习 **Service 层**（业务逻辑）
3. 学习 **DTO 模式**（数据传输对象）
4. 学习 **关系映射**（@OneToMany, @ManyToOne）

---

## 总结

User 实体类是本项目的**核心基础实体**，包含了：

✅ **12个 JPA 核心知识点**  
✅ **8个数据库设计要点**  
✅ **3个安全最佳实践**  
✅ **4个性能优化策略**

通过学习这个实体类，你已经掌握了：
- JPA/Hibernate 的基础使用
- 数据库表设计和约束
- 索引优化策略
- 枚举类型的正确使用
- 时间戳自动管理
- 密码安全存储
- Lombok 代码简化

**这些知识可以直接应用到其他 8 个实体类中！** 🎉
