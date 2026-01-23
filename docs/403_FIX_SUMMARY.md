# 403 认证问题修复总结

## 📋 问题概述

### 症状
用户在访问以下端点时收到 `403 Forbidden` 错误：
- `GET /api/orders/my` → 403
- `GET /api/payments/my-payments` → 403
- `GET /api/favorites/restaurants` → 403

### 错误信息
```
Pre-authenticated entry point called. Rejecting access
```

## 🔍 根本原因分析

### 1. JWT Token 类型错误
**问题**：系统返回的是 **Access Token**，但需要的是 **ID Token**

#### Access Token 结构（问题所在）
```json
{
  "sub": "e4a8a458-5071-708f-d59e-691f3c9bb2cd",
  "username": "user_3c2e80e5",
  "token_use": "access",
  "scope": "aws.cognito.signin.user.admin"
  // ❌ 缺少: "email", "custom:role"
}
```

#### ID Token 结构（正确）
```json
{
  "sub": "e4a8a458-5071-708f-d59e-691f3c9bb2cd",
  "email": "carttest@example.com",
  "cognito:username": "user_3c2e80e5",
  "token_use": "id",
  "given_name": "Cart",
  "family_name": "Tester"
  // ✅ 包含: "email" (必需)
}
```

### 2. 认证流程分析

```
登录 → 获取 Access Token
  ↓
前端使用 Access Token 调用 API
  ↓
JwtAuthenticationFilter.extractEmail(token)
  ↓
email = null (Access Token 没有 email 字段)
  ↓
跳过 Authentication 创建
  ↓
SecurityContext 为空
  ↓
Spring Security 拒绝请求 → 403 Forbidden
```

## ✅ 解决方案

### 修复 1: AuthService - 返回 ID Token
**文件**: `src/main/java/com/shydelivery/doordashsimulator/service/AuthService.java`

**修改**:
```java
// 修改前:
.accessToken(accessToken)  // 返回 Access Token

// 修改后:
.accessToken(idToken)  // 返回 ID Token，包含用户属性
```

**说明**: 
- 在 `accessToken` 字段中返回 ID Token
- 前端无需修改（仍使用 `.accessToken` 字段）
- ID Token 包含 `email` 字段，满足认证需求

### 修复 2: PaymentController - 修正路径前缀
**文件**: `src/main/java/com/shydelivery/doordashsimulator/controller/PaymentController.java`

**修改**:
```java
// 修改前:
@RequestMapping("/api/payments")  // 会导致 /api/api/payments

// 修改后:
@RequestMapping("/payments")  // 正确: /api/payments
```

**原因**: `application.yml` 已配置 `context-path: /api`

### 修复 3: PaymentController - 修正认证参数
**文件**: `src/main/java/com/shydelivery/doordashsimulator/controller/PaymentController.java`

**修改**:
```java
// 修改前:
public ResponseEntity<List<PaymentDTO>> getMyPayments(
    @AuthenticationPrincipal UserDetails userDetails) {
    paymentService.getPaymentHistory(userDetails.getUsername());
}

// 修改后:
public ResponseEntity<List<PaymentDTO>> getMyPayments(
    Authentication authentication) {
    paymentService.getPaymentHistory(authentication.getName());
}
```

**原因**: 系统使用 email 作为 Principal，不是 UserDetails 对象

### 修复 4: FavoriteController - 修正权限注解
**文件**: `src/main/java/com/shydelivery/doordashsimulator/controller/FavoriteController.java`

**修改**:
```java
// 修改前:
@PreAuthorize("hasAuthority('CUSTOMER')")  // ❌ 错误

// 修改后:
@PreAuthorize("hasRole('CUSTOMER')")  // ✅ 正确
```

**说明**:
- `hasRole('CUSTOMER')` → 检查 `ROLE_CUSTOMER` 权限
- `hasAuthority('CUSTOMER')` → 检查 `CUSTOMER` 权限（不带 ROLE_ 前缀）
- 系统中 GrantedAuthority 使用 `ROLE_` 前缀

## 📊 验证结果

### 测试脚本
```bash
TOKEN=$(curl -s POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carttest@example.com","password":"Password123!"}' \
  | jq -r '.accessToken')

# 测试 Orders API
curl GET http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer $TOKEN"
# ✅ HTTP 200

# 测试 Payments API
curl GET http://localhost:8080/api/payments/my-payments \
  -H "Authorization: Bearer $TOKEN"
# ✅ HTTP 200

# 测试 Favorites API
curl GET http://localhost:8080/api/favorites/restaurants \
  -H "Authorization: Bearer $TOKEN"
# ✅ HTTP 200
```

### 结果
| 端点 | 修复前 | 修复后 |
|------|--------|--------|
| `GET /api/orders/my` | ❌ 403 | ✅ 200 |
| `GET /api/payments/my-payments` | ❌ 403 | ✅ 200 |
| `GET /api/favorites/restaurants` | ❌ 403 | ✅ 200 |

## 🧠 知识点总结

### 1. JWT Token 类型
| Token 类型 | 用途 | 包含信息 |
|------------|------|----------|
| **Access Token** | API 授权 | `sub`, `scope`, `username` |
| **ID Token** | 用户身份 | `email`, `name`, `custom:role` |

**最佳实践**: 用户认证场景使用 ID Token

### 2. Spring Security 权限检查
```java
// 方式 1: hasRole (自动添加 ROLE_ 前缀)
@PreAuthorize("hasRole('CUSTOMER')")  // 检查 ROLE_CUSTOMER

// 方式 2: hasAuthority (精确匹配)
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")  // 检查 ROLE_CUSTOMER
```

### 3. Authentication Principal
```java
// 方式 1: 使用 Authentication (推荐)
public void method(Authentication auth) {
    String email = auth.getName();  // 直接获取 principal
}

// 方式 2: 使用 @AuthenticationPrincipal (需要 UserDetails)
public void method(@AuthenticationPrincipal UserDetails user) {
    String email = user.getUsername();
}
```

**本项目**: Principal 是 String (email)，应使用方式 1

### 4. Context Path 配置
```yaml
# application.yml
server:
  servlet:
    context-path: /api  # 全局路径前缀
```

```java
// Controller 不应重复添加 /api
@RequestMapping("/orders")  // ✅ 最终路径: /api/orders
@RequestMapping("/api/orders")  // ❌ 最终路径: /api/api/orders
```

## 📝 修改文件清单

1. ✅ `AuthService.java` - 返回 ID Token
2. ✅ `PaymentController.java` - 修正路径和认证参数
3. ✅ `FavoriteController.java` - 修正权限注解

## 🚀 部署步骤

```bash
# 1. 重新构建
docker-compose build

# 2. 重启服务
docker-compose down
docker-compose up -d

# 3. 等待启动 (约 15 秒)
sleep 15

# 4. 验证修复
# (使用上述测试脚本)
```

## 📚 相关文档

- [403_AUTH_ISSUE_SOLUTION.md](./403_AUTH_ISSUE_SOLUTION.md) - 详细诊断和解决方案
- [AWS Cognito JWT Tokens](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-with-identity-providers.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

---

**修复日期**: 2026-01-21  
**状态**: ✅ 已解决  
**影响范围**: Orders, Payments, Favorites 所有认证端点
