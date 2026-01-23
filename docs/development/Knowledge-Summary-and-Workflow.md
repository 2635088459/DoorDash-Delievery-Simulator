# Spring Boot 项目开发知识点总结与流程

## 📚 核心知识点速查表

### 1️⃣ 实体层（Entity Layer）- 已完成 ✅

#### 必须记住的注解

| 注解 | 用途 | 示例 | 重要性 |
|------|------|------|--------|
| `@Entity` | 标记为 JPA 实体 | `@Entity` | ⭐⭐⭐⭐⭐ |
| `@Table` | 指定表名和索引 | `@Table(name = "users")` | ⭐⭐⭐⭐⭐ |
| `@Id` | 标记主键 | `@Id` | ⭐⭐⭐⭐⭐ |
| `@GeneratedValue` | 主键生成策略 | `strategy = GenerationType.IDENTITY` | ⭐⭐⭐⭐⭐ |
| `@Column` | 字段配置 | `@Column(nullable = false, unique = true)` | ⭐⭐⭐⭐⭐ |
| `@ManyToOne` | 多对一关系 | `@ManyToOne(fetch = FetchType.LAZY)` | ⭐⭐⭐⭐⭐ |
| `@OneToMany` | 一对多关系 | `@OneToMany(mappedBy = "user")` | ⭐⭐⭐⭐⭐ |
| `@OneToOne` | 一对一关系 | `@OneToOne` | ⭐⭐⭐⭐ |
| `@Enumerated` | 枚举类型 | `@Enumerated(EnumType.STRING)` | ⭐⭐⭐⭐ |
| `@JoinColumn` | 外键列名 | `@JoinColumn(name = "user_id")` | ⭐⭐⭐⭐⭐ |

#### 关键概念（必须理解）

**1. 主键生成策略**
```java
@GeneratedValue(strategy = GenerationType.IDENTITY)  // ✅ 推荐：数据库自增
```

**2. 关系映射规则**
```
@ManyToOne  → 外键在"多"的一方
@OneToMany  → 需要 mappedBy 避免中间表
@OneToOne   → 需要 unique = true
```

**3. 延迟加载 vs 立即加载**
```java
fetch = FetchType.LAZY   // ✅ 推荐：按需加载
fetch = FetchType.EAGER  // ❌ 不推荐：立即加载所有
```

**4. 级联操作**
```java
cascade = CascadeType.ALL          // 所有操作都级联
cascade = CascadeType.PERSIST      // 只保存时级联
orphanRemoval = true               // 删除孤儿对象
```

**5. 枚举映射**
```java
@Enumerated(EnumType.STRING)  // ✅ 推荐：存储字符串
@Enumerated(EnumType.ORDINAL) // ❌ 不推荐：存储数字序号
```

**6. BigDecimal 用于金额**
```java
// ✅ 金额字段必须用 BigDecimal
@Column(precision = 10, scale = 2)
private BigDecimal price;

// ❌ 永远不要用 double 存储金额
```

---

### 2️⃣ Repository 层（数据访问层）- 下一步

#### 核心概念

**Repository = 数据库操作接口**
- 继承 `JpaRepository<实体类, 主键类型>`
- 自动提供 CRUD 方法
- 可以自定义查询方法

#### 必须记住的知识点

**1. 基础接口**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository 自动提供：
    // - save(entity)          保存/更新
    // - findById(id)          按ID查询
    // - findAll()             查询所有
    // - deleteById(id)        按ID删除
    // - count()               统计数量
    // - existsById(id)        判断是否存在
}
```

**2. 方法命名规则（自动生成查询）**
```java
// 命名规则：findBy + 字段名 + 条件
Optional<User> findByEmail(String email);              // WHERE email = ?
List<User> findByRole(UserRole role);                  // WHERE role = ?
boolean existsByEmail(String email);                   // 检查是否存在
long countByRole(UserRole role);                       // 统计数量

// 多条件查询
List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
// WHERE role = ? AND is_active = ?

// 模糊查询
List<User> findByFirstNameContaining(String name);     // WHERE first_name LIKE %?%

// 排序
List<User> findByRoleOrderByCreatedAtDesc(UserRole role);  // ORDER BY created_at DESC

// 分页
Page<User> findByRole(UserRole role, Pageable pageable);
```

**3. @Query 自定义查询**
```java
// JPQL 查询（推荐）
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmailCustom(@Param("email") String email);

// 原生 SQL 查询
@Query(value = "SELECT * FROM users WHERE email = ?1", nativeQuery = true)
Optional<User> findByEmailNative(String email);

// JOIN 查询
@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.id = :id")
Optional<Order> findByIdWithCustomer(@Param("id") Long id);
```

**4. 分页和排序**
```java
// Controller 中接收分页参数
@GetMapping("/users")
public Page<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    return userRepository.findAll(pageable);
}
```

---

### 3️⃣ Service 层（业务逻辑层）- 第三步

#### 核心概念

**Service = 业务逻辑处理**
- 调用 Repository 访问数据
- 处理业务规则
- 事务管理
- DTO 转换

#### 必须记住的知识点

**1. 基本结构**
```java
@Service
@Transactional  // 事务管理
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 构造函数注入（推荐）
    public UserService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

**2. CRUD 操作模板**
```java
// 创建
public UserDTO createUser(CreateUserRequest request) {
    // 1. 验证
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new BusinessException("邮箱已存在");
    }
    
    // 2. DTO → Entity
    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    
    // 3. 保存
    User saved = userRepository.save(user);
    
    // 4. Entity → DTO
    return UserDTO.from(saved);
}

// 查询
public UserDTO getUserById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    return UserDTO.from(user);
}

// 更新
public UserDTO updateUser(Long id, UpdateUserRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    
    User updated = userRepository.save(user);
    return UserDTO.from(updated);
}

// 删除
public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
        throw new ResourceNotFoundException("用户不存在");
    }
    userRepository.deleteById(id);
}
```

**3. 事务管理**
```java
@Transactional  // 类级别：所有方法都有事务
public class OrderService {
    
    @Transactional  // 方法级别：只有这个方法有事务
    public Order createOrder(CreateOrderRequest request) {
        // 多个数据库操作
        Order order = orderRepository.save(new Order());
        orderItemRepository.saveAll(items);
        // 全部成功才提交，否则全部回滚
    }
    
    @Transactional(readOnly = true)  // 只读事务（性能优化）
    public OrderDTO getOrder(Long id) {
        return orderRepository.findById(id)...;
    }
}
```

**4. 异常处理**
```java
// 自定义业务异常
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Service 中抛出
if (user == null) {
    throw new ResourceNotFoundException("用户ID " + id + " 不存在");
}
```

---

### 4️⃣ Controller 层（API 接口层）- 第四步

#### 核心概念

**Controller = REST API 端点**
- 接收 HTTP 请求
- 调用 Service 处理
- 返回 HTTP 响应

#### 必须记住的注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@RestController` | REST 控制器 | `@RestController` |
| `@RequestMapping` | 基础路径 | `@RequestMapping("/api/users")` |
| `@GetMapping` | GET 请求 | `@GetMapping("/{id}")` |
| `@PostMapping` | POST 请求 | `@PostMapping("/register")` |
| `@PutMapping` | PUT 请求 | `@PutMapping("/{id}")` |
| `@DeleteMapping` | DELETE 请求 | `@DeleteMapping("/{id}")` |
| `@PathVariable` | 路径参数 | `@PathVariable Long id` |
| `@RequestParam` | 查询参数 | `@RequestParam String name` |
| `@RequestBody` | 请求体 | `@RequestBody UserDTO dto` |
| `@Valid` | 数据验证 | `@Valid @RequestBody UserDTO dto` |

#### REST API 设计规范（必须遵守）

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // 查询所有 - GET /api/users
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    // 按ID查询 - GET /api/users/1
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    // 创建 - POST /api/users
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // 更新 - PUT /api/users/1
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }
    
    // 删除 - DELETE /api/users/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    // 查询参数 - GET /api/users/search?email=john@example.com
    @GetMapping("/search")
    public ResponseEntity<UserDTO> searchByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
}
```

#### HTTP 状态码规范

| 状态码 | 含义 | 使用场景 |
|--------|------|---------|
| 200 OK | 成功 | GET, PUT 成功 |
| 201 Created | 已创建 | POST 成功 |
| 204 No Content | 无内容 | DELETE 成功 |
| 400 Bad Request | 请求错误 | 参数验证失败 |
| 401 Unauthorized | 未认证 | 未登录 |
| 403 Forbidden | 无权限 | 权限不足 |
| 404 Not Found | 未找到 | 资源不存在 |
| 500 Internal Server Error | 服务器错误 | 系统异常 |

---

### 5️⃣ DTO 模式（数据传输对象）

#### 为什么需要 DTO？

```java
// ❌ 不推荐：直接返回 Entity
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).get();
    // 问题：
    // 1. 暴露所有字段（包括密码）
    // 2. 懒加载可能导致序列化错误
    // 3. 循环引用问题
}

// ✅ 推荐：使用 DTO
@GetMapping("/{id}")
public UserDTO getUser(@PathVariable Long id) {
    User user = userRepository.findById(id).get();
    return UserDTO.from(user);
    // 优点：
    // 1. 只返回需要的字段
    // 2. 控制输出格式
    // 3. 避免循环引用
}
```

#### DTO 设计模式

```java
// 响应 DTO
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    // 不包含 password！
    
    // Entity → DTO
    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        return dto;
    }
}

// 创建请求 DTO
public class CreateUserRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;
    
    @NotBlank(message = "名字不能为空")
    private String firstName;
    
    @NotBlank(message = "姓氏不能为空")
    private String lastName;
    
    @NotNull(message = "角色不能为空")
    private UserRole role;
}

// 更新请求 DTO
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    // 不包含 email 和 password（不允许修改）
}
```

---

## 🔄 完整开发流程（必须记住）

### 标准开发顺序

```
1. Entity（实体类）        ✅ 已完成
   ↓
2. Repository（数据访问）   ← 下一步
   ↓
3. DTO（数据传输对象）
   ↓
4. Service（业务逻辑）
   ↓
5. Controller（API接口）
   ↓
6. 测试（Postman/单元测试）
   ↓
7. 部署（Docker/AWS）
```

### 具体实施步骤

#### 步骤 1：创建 Repository
```java
// src/main/java/com/shydelivery/doordashsimulator/repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
}
```

#### 步骤 2：创建 DTO
```java
// src/main/java/com/shydelivery/doordashsimulator/dto/UserDTO.java
// src/main/java/com/shydelivery/doordashsimulator/dto/request/CreateUserRequest.java
// src/main/java/com/shydelivery/doordashsimulator/dto/request/UpdateUserRequest.java
```

#### 步骤 3：创建 Service
```java
// src/main/java/com/shydelivery/doordashsimulator/service/UserService.java
@Service
@Transactional
public class UserService {
    // 实现业务逻辑
}
```

#### 步骤 4：创建 Controller
```java
// src/main/java/com/shydelivery/doordashsimulator/controller/UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {
    // 实现 REST API
}
```

#### 步骤 5：测试
```bash
# Postman 测试
POST http://localhost:8080/api/users
GET http://localhost:8080/api/users/1
PUT http://localhost:8080/api/users/1
DELETE http://localhost:8080/api/users/1
```

---

## 📁 项目结构（标准目录结构）

```
src/main/java/com/shydelivery/doordashsimulator/
│
├── entity/                      # 实体类（已完成）
│   ├── User.java
│   ├── Order.java
│   ├── Restaurant.java
│   └── ...
│
├── repository/                  # Repository 接口（下一步）
│   ├── UserRepository.java
│   ├── OrderRepository.java
│   └── ...
│
├── dto/                         # DTO 类
│   ├── UserDTO.java
│   ├── OrderDTO.java
│   ├── request/                 # 请求 DTO
│   │   ├── CreateUserRequest.java
│   │   └── UpdateUserRequest.java
│   └── response/                # 响应 DTO（可选）
│       └── ApiResponse.java
│
├── service/                     # Service 层
│   ├── UserService.java
│   ├── OrderService.java
│   └── ...
│
├── controller/                  # Controller 层
│   ├── UserController.java
│   ├── OrderController.java
│   └── ...
│
├── exception/                   # 自定义异常
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
│
├── config/                      # 配置类
│   ├── SecurityConfig.java
│   └── CorsConfig.java
│
└── DoorDashSimulatorApplication.java
```

---

## 🎯 关键设计原则（必须遵守）

### 1. 单一职责原则
```
Entity      → 只负责数据映射
Repository  → 只负责数据库操作
Service     → 只负责业务逻辑
Controller  → 只负责请求响应
DTO         → 只负责数据传输
```

### 2. 依赖注入（构造函数注入）
```java
// ✅ 推荐：构造函数注入
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ❌ 不推荐：字段注入
@Autowired
private UserRepository userRepository;
```

### 3. 永远不要暴露 Entity
```java
// ❌ 错误
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).get();
}

// ✅ 正确
@GetMapping("/{id}")
public UserDTO getUser(@PathVariable Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    return UserDTO.from(user);
}
```

### 4. 统一异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

### 5. 数据验证
```java
// DTO 中使用验证注解
public class CreateUserRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
}

// Controller 中启用验证
@PostMapping
public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
    // @Valid 会自动验证，失败抛出 MethodArgumentNotValidException
}
```

---

## 🔧 常用工具和配置

### 1. Lombok 注解（简化代码）
```java
@Data                    // getter + setter + toString + equals + hashCode
@Getter                  // 只生成 getter
@Setter                  // 只生成 setter
@NoArgsConstructor      // 无参构造函数
@AllArgsConstructor     // 全参构造函数
@Builder                // Builder 模式
@ToString(exclude = {"orders"})  // toString 排除某些字段
@EqualsAndHashCode(onlyExplicitlyIncluded = true)  // 只用指定字段
```

### 2. Spring Boot 配置文件
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/doordash_db
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update        # 开发: update, 生产: validate
    show-sql: true            # 显示 SQL
    properties:
      hibernate:
        format_sql: true      # 格式化 SQL
  
  jackson:
    serialization:
      write-dates-as-timestamps: false
    
server:
  port: 8080
  servlet:
    context-path: /api
```

### 3. Maven 依赖
```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

---

## ✅ 检查清单（每个功能开发完成后）

### Entity 层检查
- [ ] @Entity, @Table 注解正确
- [ ] 主键配置正确（@Id + @GeneratedValue）
- [ ] 关系映射正确（@ManyToOne, @OneToMany）
- [ ] 枚举使用 EnumType.STRING
- [ ] 金额字段使用 BigDecimal
- [ ] 索引配置合理
- [ ] 时间戳自动管理

### Repository 层检查
- [ ] 继承 JpaRepository
- [ ] 方法命名符合规范
- [ ] 自定义查询使用 @Query
- [ ] @Repository 注解添加

### Service 层检查
- [ ] @Service 注解
- [ ] @Transactional 配置
- [ ] 构造函数注入
- [ ] 异常处理完善
- [ ] DTO 转换正确
- [ ] 业务逻辑清晰

### Controller 层检查
- [ ] @RestController 注解
- [ ] REST 路径规范
- [ ] HTTP 方法正确
- [ ] 参数验证（@Valid）
- [ ] 返回 ResponseEntity
- [ ] 状态码正确
- [ ] 不暴露 Entity

### 测试检查
- [ ] Postman 所有接口测试通过
- [ ] 边界条件测试
- [ ] 异常情况测试
- [ ] 数据库状态验证

---

## 🚀 快速参考命令

### Docker 命令
```bash
# 启动所有容器
docker-compose up -d

# 重启应用容器
docker-compose restart doordash-app

# 查看日志
docker-compose logs -f doordash-app

# 停止所有容器
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

### 测试命令
```bash
# 健康检查
curl http://localhost:8080/api/health

# 测试 API（示例）
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"123456"}'

curl http://localhost:8080/api/users/1
```

---

## 📖 推荐学习资源

### 官方文档
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Hibernate: https://hibernate.org/orm/documentation/

### 视频教程
- Spring Boot 完整教程
- JPA/Hibernate 深入理解
- RESTful API 设计最佳实践

---

## 💡 最后的建议

### 记住这些核心原则：

1. **分层清晰**：Entity → Repository → Service → Controller
2. **职责单一**：每层只做自己的事
3. **使用 DTO**：永远不要暴露 Entity
4. **异常处理**：统一异常处理机制
5. **数据验证**：使用 @Valid 验证输入
6. **事务管理**：Service 层使用 @Transactional
7. **依赖注入**：使用构造函数注入
8. **RESTful**：遵守 REST API 设计规范

### 开发流程：
```
先完成功能 → 再优化性能 → 最后部署上线
先本地开发 → 再本地测试 → 最后云端部署
```

### 遇到问题：
1. 查看日志（docker logs）
2. 查看文档（已创建的文档）
3. Google 搜索错误信息
4. Stack Overflow

---

**现在你已经有了完整的知识体系，可以开始 Repository 层开发了！** 🎉
