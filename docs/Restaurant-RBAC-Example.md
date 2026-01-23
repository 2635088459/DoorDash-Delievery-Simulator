# Restaurant 模块 RBAC 示例说明

## 📚 概述

这是一个完整的基于角色的访问控制 (RBAC) 实现示例，演示了如何在 Spring Boot + Spring Security + JWT 环境下保护 REST API 端点。

## 🏗️ 架构组件

### 1. 实体层 (Entity)
- **Restaurant.java** - 餐厅实体，包含所有者关系

### 2. 数据访问层 (Repository)
- **RestaurantRepository.java** - 餐厅数据访问接口

### 3. 服务层 (Service)
- **AuthorizationService.java** - 资源所有权验证服务
- **RestaurantService.java** - 餐厅业务逻辑

### 4. 控制层 (Controller)
- **RestaurantController.java** - REST API 端点（包含 RBAC 注解）

### 5. DTO 层
- **CreateRestaurantRequest.java** - 创建餐厅请求
- **UpdateRestaurantRequest.java** - 更新餐厅请求
- **RestaurantDTO.java** - 餐厅响应数据

## 🔐 权限控制策略

### 三层防护机制

```
┌─────────────────────────────────────────┐
│  Layer 1: Spring Security Filter       │ ← JWT Token 验证
├─────────────────────────────────────────┤
│  Layer 2: @PreAuthorize Annotation     │ ← 角色验证
├─────────────────────────────────────────┤
│  Layer 3: AuthorizationService          │ ← 资源所有权验证
└─────────────────────────────────────────┘
```

### API 端点权限矩阵

| API 端点 | HTTP 方法 | 访问权限 | 说明 |
|---------|----------|---------|------|
| `/api/restaurants` | GET | 公开 | 所有人可浏览餐厅列表 |
| `/api/restaurants/{id}` | GET | 公开 | 所有人可查看餐厅详情 |
| `/api/restaurants` | POST | RESTAURANT_OWNER | 仅餐厅老板可创建 |
| `/api/restaurants/my` | GET | RESTAURANT_OWNER | 仅餐厅老板查看自己的餐厅 |
| `/api/restaurants/{id}` | PUT | RESTAURANT_OWNER + 所有权 | 仅所有者可更新 |
| `/api/restaurants/{id}` | DELETE | RESTAURANT_OWNER + 所有权 | 仅所有者可删除 |

## 💡 关键实现细节

### 1. Controller 层的角色验证

```java
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
@PostMapping
public ResponseEntity<RestaurantDTO> createRestaurant(
        @Valid @RequestBody CreateRestaurantRequest request,
        Principal principal) {
    // 只有 RESTAURANT_OWNER 角色才能执行到这里
    String ownerEmail = principal.getName();
    RestaurantDTO created = restaurantService.createRestaurant(request, ownerEmail);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

**工作原理**：
1. Spring Security 在方法执行前拦截
2. 检查当前用户的角色
3. 如果角色不匹配，返回 `403 Forbidden`
4. 如果角色匹配，继续执行方法

### 2. Service 层的所有权验证

```java
@Transactional
public RestaurantDTO updateRestaurant(Long id, UpdateRestaurantRequest request, String ownerEmail) {
    // 第一步：验证用户是否拥有这个餐厅
    authorizationService.verifyRestaurantOwnership(id, ownerEmail);
    
    // 第二步：如果验证通过，执行更新
    Restaurant restaurant = restaurantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在"));
    // ... 更新逻辑 ...
}
```

**工作原理**：
1. `AuthorizationService.verifyRestaurantOwnership()` 检查：
   - 餐厅是否存在
   - 用户是否存在
   - 用户ID 是否与餐厅所有者ID 匹配
2. 如果任何条件不满足，抛出异常
3. 如果验证通过，继续执行业务逻辑

### 3. AuthorizationService 实现

```java
public void verifyRestaurantOwnership(Long restaurantId, String email) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
        .orElseThrow(() -> new ResourceNotFoundException("餐厅不存在"));
    
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    
    if (!restaurant.getOwner().getId().equals(user.getId())) {
        throw new AccessDeniedException("您没有权限访问此餐厅");
    }
}
```

## 🧪 测试场景

### 场景 1: 公开接口访问
```bash
# 无需登录即可访问
curl http://localhost:8080/api/restaurants
```
**预期结果**: `200 OK` - 返回餐厅列表

---

### 场景 2: CUSTOMER 尝试创建餐厅（应该失败）
```bash
# 1. 先登录获取 Token
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"customer@example.com","password":"Test@123"}' | jq -r '.idToken')

# 2. 尝试创建餐厅
curl -X POST http://localhost:8080/api/restaurants \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"name":"Test Restaurant",...}'
```
**预期结果**: `403 Forbidden`  
**原因**: CUSTOMER 角色不满足 `@PreAuthorize("hasRole('RESTAURANT_OWNER')")`

---

### 场景 3: RESTAURANT_OWNER 创建餐厅（应该成功）
```bash
# 1. 登录餐厅老板账户
OWNER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"owner@restaurant.com","password":"Test@123"}' | jq -r '.idToken')

# 2. 创建餐厅
curl -X POST http://localhost:8080/api/restaurants \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -d '{
    "name": "美味中餐馆",
    "description": "正宗川菜",
    "cuisineType": "中餐",
    "streetAddress": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94102",
    "phoneNumber": "+14155551234"
  }'
```
**预期结果**: `201 Created` - 餐厅创建成功  
**原因**: RESTAURANT_OWNER 角色满足权限要求

---

### 场景 4: 用户尝试修改其他人的餐厅（应该失败）
```bash
# 假设餐厅ID为1，属于 owner@restaurant.com
# 另一个餐厅老板尝试修改
curl -X PUT http://localhost:8080/api/restaurants/1 \
  -H "Authorization: Bearer $OTHER_OWNER_TOKEN" \
  -d '{"name":"黑客修改"}'
```
**预期结果**: `403 Forbidden`  
**原因**: `AuthorizationService.verifyRestaurantOwnership()` 检测到所有权不匹配

---

### 场景 5: 所有者更新自己的餐厅（应该成功）
```bash
curl -X PUT http://localhost:8080/api/restaurants/1 \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -d '{
    "name": "美味中餐馆（更新）",
    "description": "正宗川菜，新增粤菜"
  }'
```
**预期结果**: `200 OK` - 餐厅更新成功  
**原因**: 角色匹配 + 所有权验证通过

## 🚀 运行自动化测试

我们提供了一个完整的测试脚本来演示所有 RBAC 场景：

```bash
# 给脚本添加执行权限
chmod +x scripts/test-rbac-restaurant.sh

# 运行测试
./scripts/test-rbac-restaurant.sh
```

测试脚本会自动执行：
1. ✅ 公开接口访问测试
2. ❌ CUSTOMER 尝试创建餐厅（预期失败）
3. ❌ CUSTOMER 尝试查看"我的餐厅"（预期失败）
4. ✅ RESTAURANT_OWNER 创建餐厅（预期成功）
5. ✅ RESTAURANT_OWNER 查看"我的餐厅"（预期成功）
6. ✅ RESTAURANT_OWNER 更新自己的餐厅（预期成功）
7. ❌ 其他用户尝试修改不属于自己的餐厅（预期失败）

## 📊 测试输出示例

```
╔═══════════════════════════════════════════════════════════╗
║   DoorDash RBAC 示例测试 - Restaurant 模块               ║
╚═══════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
第 1 部分：公开接口测试（无需登录）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 测试 1: 获取所有餐厅列表（公开接口）
   请求: GET /api/restaurants
   ✅ 成功 (HTTP 200)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
第 2 部分：CUSTOMER 角色测试
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔐 登录为 CUSTOMER 用户...
   ✅ 登录成功
   角色: CUSTOMER

❌ 测试 2: CUSTOMER 尝试创建餐厅（应该被拒绝）
   请求: POST /api/restaurants
   权限检查: @PreAuthorize("hasRole('RESTAURANT_OWNER')")
   ✅ 正确拒绝访问 (HTTP 403 Forbidden)
   RBAC 工作正常：CUSTOMER 无权创建餐厅
```

## 🎯 学习要点

### 1. **@PreAuthorize 注解**
- 在方法执行前进行权限检查
- 支持 SpEL 表达式
- 常用表达式：
  - `hasRole('ROLE_NAME')` - 检查单个角色
  - `hasAnyRole('ROLE1', 'ROLE2')` - 检查多个角色之一
  - `hasAuthority('PERMISSION')` - 检查权限

### 2. **双重验证模式**
- **第一层**：角色验证（Controller 层）
- **第二层**：资源所有权验证（Service 层）
- 这确保了：
  - 用户有正确的角色
  - 用户只能操作自己的资源

### 3. **Principal 对象**
```java
public ResponseEntity<?> someMethod(Principal principal) {
    String email = principal.getName(); // 获取当前登录用户的邮箱
}
```
- `Principal` 由 Spring Security 自动注入
- 包含当前认证用户的信息
- 通过 JWT Filter 设置到 SecurityContext 中

### 4. **异常处理**
- `AccessDeniedException` → 403 Forbidden
- `ResourceNotFoundException` → 404 Not Found
- `UsernameNotFoundException` → 401 Unauthorized
- 通过 `GlobalExceptionHandler` 统一处理

## 🔄 扩展到其他模块

使用相同的模式可以轻松扩展到其他模块：

### Order 模块示例
```java
@PreAuthorize("hasRole('CUSTOMER')")
@PostMapping("/api/orders")
public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request, Principal principal) {
    // 只有 CUSTOMER 可以创建订单
}

@PreAuthorize("hasRole('RESTAURANT_OWNER')")
@PostMapping("/api/orders/{id}/accept")
public ResponseEntity<OrderDTO> acceptOrder(@PathVariable Long id, Principal principal) {
    // 验证订单属于该餐厅老板的餐厅
    authorizationService.verifyOrderRestaurantOwnership(id, principal.getName());
    // ...
}

@PreAuthorize("hasRole('DRIVER')")
@PostMapping("/api/deliveries/{id}/accept")
public ResponseEntity<OrderDTO> acceptDelivery(@PathVariable Long id, Principal principal) {
    // 只有 DRIVER 可以接单配送
}
```

## 📚 相关文档

- [完整 RBAC 设计文档](../docs/RBAC-Design-Document.md)
- [JWT 认证指南](../docs/User-Authentication-JWT-Guide.md)
- [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/index.html)

## 🎓 下一步

1. ✅ **已完成**: Restaurant 模块 RBAC 实现
2. ⏭️ **建议下一步**: 
   - 实现 Order 模块的 RBAC
   - 实现 Menu 模块的 RBAC
   - 实现 Delivery 模块的 RBAC
   - 添加单元测试和集成测试

---

**作者**: DoorDash 开发团队  
**日期**: 2026-01-19  
**版本**: v1.0
