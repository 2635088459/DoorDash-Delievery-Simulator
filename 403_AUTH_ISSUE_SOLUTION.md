# 403 认证问题诊断与解决方案

## 📋 问题总结

**症状**: 使用正确的JWT Token访问需要认证的API端点时返回403 Forbidden错误

**影响范围**: 
- Orders API (`/api/orders`)
- Payments API (`/api/payments`)
- Favorites API (`/api/favorites`)
- Cart API (`/api/carts`)
- 其他所有需要认证的端点

**根本原因**: JWT Access Token 中缺少必要的用户信息（email 和 role），导致认证失败

---

## 🔍 问题诊断过程

### 1. JWT Token 内容分析

**当前 Token Payload**:
```json
{
  "sub": "e4a8a458-5071-708f-d59e-691f3c9bb2cd",
  "iss": "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_a6gt5CsAi",
  "client_id": "7fv4l4ftq2qrioplfcrmu5a2d9",
  "token_use": "access",
  "scope": "aws.cognito.signin.user.admin",
  "username": "user_3c2e80e5",
  "auth_time": 1768976601,
  "exp": 1768980201,
  "iat": 1768976601
}
```

**问题发现**:
- ❌ 缺少 `email` 字段
- ❌ 缺少 `custom:role` 字段
- ✅ 有 `username` 字段（Cognito 用户名）
- ✅ 有 `sub` 字段（Cognito User ID）

### 2. 代码流程分析

**认证流程**:
```
1. 客户端登录 → 获取 Access Token
   ↓
2. 客户端请求 API (Header: Authorization: Bearer {token})
   ↓
3. JwtAuthenticationFilter.doFilterInternal()
   ↓
4. jwtValidator.extractEmail(signedJWT)  ← 返回 null (Token中没有email)
   ↓
5. email == null → 无法创建 Authentication 对象
   ↓
6. SecurityContext 为空 → Spring Security 拒绝访问 → 403
```

**关键代码位置**:
```java
// JwtAuthenticationFilter.java (第 70-72 行)
String email = jwtValidator.extractEmail(signedJWT);  // 返回 null
...
if (email != null) {  // 条件不满足，跳过认证
    // 创建 Authentication
}
```

### 3. 为什么 Token 中没有 email 和 role？

**AWS Cognito Token 类型**:
- **Access Token**: 用于授权访问资源，包含 scopes 和 username，**不包含自定义属性**
- **ID Token**: 用于身份识别，**包含所有用户属性**（email、custom:role 等）

**当前问题**: 
系统使用的是 **Access Token**，但代码期望的是 **ID Token** 的字段

---

## 💡 解决方案

### 方案 1: 使用 ID Token 代替 Access Token (推荐) ⭐

**原理**: ID Token 包含完整的用户信息

**需要修改的代码**:

#### 1.1 修改 AuthService.java

**文件位置**: `src/main/java/com/shydelivery/doordashsimulator/service/AuthService.java`

**需要修改的方法**: `login()`

**修改位置**: 第 80-90 行左右

**原代码**:
```java
return AuthResponse.builder()
        .accessToken(authResult.getAccessToken())  // ← 当前返回 Access Token
        .refreshToken(authResult.getRefreshToken())
        .expiresIn(authResult.getExpiresIn())
        .tokenType("Bearer")
        .user(userDTO)
        .build();
```

**修改为**:
```java
return AuthResponse.builder()
        .accessToken(authResult.getIdToken())  // ← 改为返回 ID Token
        .refreshToken(authResult.getRefreshToken())
        .expiresIn(authResult.getExpiresIn())
        .tokenType("Bearer")
        .user(userDTO)
        .build();
```

**说明**: 
- 只需修改一行代码
- ID Token 包含 email 和 custom:role
- 前端无需任何改动（仍然使用 accessToken 字段）

---

### 方案 2: 使用 username 查询数据库 (备选方案)

**原理**: Token 有 username，用它查询数据库获取 email 和 role

**需要修改的代码**:

#### 2.1 修改 CognitoJwtValidator.java

**文件位置**: `src/main/java/com/shydelivery/doordashsimulator/security/CognitoJwtValidator.java`

**添加新方法** (在文件末尾，第 195 行后):

```java
/**
 * 从 Token 提取 Cognito 用户名
 */
public String extractUsername(SignedJWT signedJWT) {
    try {
        return signedJWT.getJWTClaimsSet().getStringClaim("username");
    } catch (ParseException e) {
        log.error("Failed to extract username from JWT: {}", e.getMessage());
        return null;
    }
}
```

#### 2.2 修改 JwtAuthenticationFilter.java

**文件位置**: `src/main/java/com/shydelivery/doordashsimulator/security/JwtAuthenticationFilter.java`

**修改位置**: 第 60-75 行

**原代码**:
```java
// 3. 提取用户信息
String cognitoSub = jwtValidator.extractCognitoSub(signedJWT);
String email = jwtValidator.extractEmail(signedJWT);  // 返回 null
String role = jwtValidator.extractRole(signedJWT);    // 返回 null

log.debug("Token validated for user: {} ({}), role: {}", email, cognitoSub, role);

// 4. 创建 Spring Security 的 Authentication 对象
if (email != null) {  // ← email 为 null，跳过
    // 从数据库获取用户的最新角色（而不是依赖 Token）
    User user = userRepository.findByEmail(email).orElse(null);
    ...
}
```

**修改为**:
```java
// 3. 提取用户信息
String cognitoSub = jwtValidator.extractCognitoSub(signedJWT);
String email = jwtValidator.extractEmail(signedJWT);
String username = jwtValidator.extractUsername(signedJWT);  // 新增
String role = jwtValidator.extractRole(signedJWT);

log.debug("Token validated for user: cognitoSub={}, email={}, username={}, role={}", 
    cognitoSub, email, username, role);

// 4. 创建 Spring Security 的 Authentication 对象
// 优先使用 email，如果没有则使用 cognitoSub 或 username
String userIdentifier = email;
User user = null;

if (userIdentifier != null) {
    user = userRepository.findByEmail(userIdentifier).orElse(null);
} else if (cognitoSub != null) {
    // 如果没有 email，尝试用 cognitoSub 查询
    user = userRepository.findByCognitoSub(cognitoSub).orElse(null);
    if (user != null) {
        userIdentifier = user.getEmail();
    }
}

if (userIdentifier != null && user != null) {
    // 使用数据库中的角色创建权限
    List<GrantedAuthority> authorities = Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
    );
    
    log.debug("User {} has role: {} from database", userIdentifier, user.getRole());
    
    // 创建 Authentication 对象
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userIdentifier,  // Principal (使用 email)
                    null,            // Credentials
                    authorities      // Authorities
            );
    
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    log.debug("User authenticated: {} with authorities: {}", userIdentifier, authorities);
} else {
    log.warn("Could not authenticate user from token - missing user identifier or user not found in database");
}
```

---

### 方案 3: 混合方案 - 同时支持 Access Token 和 ID Token (最灵活)

**原理**: 检测 Token 类型，智能处理

**需要修改的代码**:

#### 3.1 修改 JwtAuthenticationFilter.java

**文件位置**: `src/main/java/com/shydelivery/doordashsimulator/security/JwtAuthenticationFilter.java`

**完整替换 doFilterInternal 方法的认证部分** (第 60-120 行):

```java
// 3. 提取用户信息
String cognitoSub = jwtValidator.extractCognitoSub(signedJWT);
String email = jwtValidator.extractEmail(signedJWT);
String username = jwtValidator.extractUsername(signedJWT);
String role = jwtValidator.extractRole(signedJWT);

// 判断 Token 类型
String tokenType = "Unknown";
try {
    tokenType = signedJWT.getJWTClaimsSet().getStringClaim("token_use");
} catch (Exception e) {
    log.warn("Could not determine token type");
}

log.debug("Token type: {}, cognitoSub={}, email={}, username={}, role={}", 
    tokenType, cognitoSub, email, username, role);

// 4. 获取用户信息
User user = null;
String userIdentifier = null;

// 策略1: 如果有 email (ID Token 或自定义 Token)
if (email != null && !email.isEmpty()) {
    user = userRepository.findByEmail(email).orElse(null);
    userIdentifier = email;
    log.debug("Found user by email: {}", email);
}

// 策略2: 如果没有 email，尝试用 cognitoSub 查询 (Access Token)
if (user == null && cognitoSub != null && !cognitoSub.isEmpty()) {
    user = userRepository.findByCognitoSub(cognitoSub).orElse(null);
    if (user != null) {
        userIdentifier = user.getEmail();
        log.debug("Found user by cognitoSub: {}, email: {}", cognitoSub, userIdentifier);
    }
}

// 策略3: 如果还没找到，记录警告
if (user == null) {
    log.warn("User not found in database - token_use: {}, cognitoSub: {}, email: {}, username: {}", 
        tokenType, cognitoSub, email, username);
}

// 5. 创建 Authentication 对象
if (user != null && userIdentifier != null) {
    // 使用数据库中的角色
    List<GrantedAuthority> authorities = Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
    );
    
    log.debug("Authenticating user: {} with role: {}", userIdentifier, user.getRole());
    
    // 创建 Authentication 对象
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userIdentifier,  // Principal
                    null,            // Credentials
                    authorities      // Authorities
            );
    
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    log.info("✅ User authenticated successfully: {} (role: {}, token_type: {})", 
        userIdentifier, user.getRole(), tokenType);
} else {
    log.error("❌ Authentication failed - user not found or invalid token");
}
```

**同时添加 extractUsername 方法到 CognitoJwtValidator.java** (见方案2)

---

## 🎯 推荐实施步骤

### ✅ 快速修复（5分钟）- 方案 1

1. 打开 `AuthService.java`
2. 找到 `login()` 方法中的 `AuthResponse.builder()`
3. 将 `authResult.getAccessToken()` 改为 `authResult.getIdToken()`
4. 保存文件
5. 重新构建并部署：
   ```bash
   docker-compose down
   docker-compose build
   docker-compose up -d
   ```

### ✅ 完整修复（15分钟）- 方案 3（推荐）

1. 在 `CognitoJwtValidator.java` 添加 `extractUsername()` 方法
2. 在 `JwtAuthenticationFilter.java` 替换认证逻辑
3. 重新构建并部署
4. 测试验证

---

## 🧪 验证测试

### 测试脚本

```bash
#!/bin/bash

echo "=== 步骤1: 登录获取Token ==="
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carttest@example.com",
    "password": "Password123!"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')
echo "Token: ${TOKEN:0:50}..."

echo -e "\n=== 步骤2: 解析Token内容 ==="
python3 -c "
import base64, json
payload = '$TOKEN'.split('.')[1]
padding = len(payload) % 4
decoded = base64.urlsafe_b64decode(payload + '=' * padding)
print(json.dumps(json.loads(decoded), indent=2))
"

echo -e "\n=== 步骤3: 测试Orders API ==="
curl -s -X GET "http://localhost:8080/api/orders" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo -e "\n=== 步骤4: 测试Payments API ==="
curl -s -X GET "http://localhost:8080/api/payments/my-payments" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo -e "\n=== 步骤5: 测试Favorites API ==="
curl -s -X GET "http://localhost:8080/api/favorites/restaurants" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 预期结果

**修复前**:
```
HTTP/1.1 403 Forbidden
{"timestamp":"...","status":403,"error":"Forbidden","path":"/api/orders"}
```

**修复后**:
```
HTTP/1.1 200 OK
[
  {
    "id": 1,
    "orderNumber": "ORD-...",
    "status": "PENDING",
    ...
  }
]
```

---

## 📚 知识点总结

### 1. JWT Token 类型

#### Access Token
- **用途**: 授权访问API资源
- **包含内容**: 
  - `sub` (Subject - User ID)
  - `username` (Cognito用户名)
  - `scope` (权限范围)
  - `token_use: access`
- **特点**: **不包含**用户自定义属性（email、role等）
- **有效期**: 通常1小时

#### ID Token
- **用途**: 身份识别和用户信息
- **包含内容**:
  - `sub` (Subject - User ID)
  - `email` (用户邮箱)
  - `custom:role` (自定义角色属性)
  - `token_use: id`
  - 所有 Cognito 用户属性
- **特点**: **包含完整**用户信息
- **有效期**: 通常1小时

#### Refresh Token
- **用途**: 刷新 Access Token 和 ID Token
- **有效期**: 通常30天

### 2. Spring Security 认证流程

```
HTTP Request
    ↓
SecurityFilterChain
    ↓
JwtAuthenticationFilter (OncePerRequestFilter)
    ├─ 提取 Token (从 Authorization Header)
    ├─ 验证 Token (签名、过期时间等)
    ├─ 提取用户信息 (email, role, sub等)
    ├─ 查询数据库获取完整用户信息
    ├─ 创建 Authentication 对象
    └─ 设置到 SecurityContext
    ↓
Controller Method
    ├─ @PreAuthorize 检查角色权限
    └─ @AuthenticationPrincipal 获取当前用户
```

### 3. Spring Security 关键概念

#### SecurityContext
- 存储当前请求的认证信息
- 线程隔离（ThreadLocal）
- 每个请求都有独立的 SecurityContext

#### Authentication 对象
- **Principal**: 用户标识（通常是 email 或 username）
- **Credentials**: 凭证（通常为 null，因为已认证）
- **Authorities**: 权限列表（如 ROLE_CUSTOMER）

#### GrantedAuthority
- Spring Security 的权限表示
- 格式: `ROLE_` + 角色名
- 例如: `ROLE_CUSTOMER`, `ROLE_ADMIN`

### 4. @PreAuthorize 注解

```java
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<List<OrderDTO>> getMyOrders() {
    // Spring Security 会检查：
    // 1. SecurityContext 是否有 Authentication
    // 2. Authentication 是否包含 ROLE_CUSTOMER 权限
    // 如果任一条件不满足 → 403 Forbidden
}
```

### 5. 常见认证错误

#### 403 Forbidden
- **原因**: 认证成功但权限不足
- **SecurityContext**: 有 Authentication，但没有所需的 Role
- **解决**: 检查用户角色和 @PreAuthorize

#### 401 Unauthorized  
- **原因**: 未认证
- **SecurityContext**: 无 Authentication 对象
- **解决**: 检查 Token 是否有效

#### "Pre-authenticated entry point called"
- **原因**: SecurityContext 为空，但访问需要认证的资源
- **解决**: 确保 JwtAuthenticationFilter 正确设置 Authentication

---

## 🔧 故障排查清单

### ✅ 检查 Token 是否正确

```bash
# 1. 登录并获取 Token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.accessToken')

# 2. 解析 Token
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq .

# 检查内容:
# - 是否有 email 字段?
# - 是否有 custom:role 字段?
# - token_use 是 "access" 还是 "id"?
```

### ✅ 检查数据库用户信息

```sql
-- 检查用户是否存在
SELECT id, email, cognito_sub, role, is_active 
FROM users 
WHERE email = 'test@example.com';

-- 检查 cognito_sub 是否匹配
SELECT id, email, cognito_sub, role 
FROM users 
WHERE cognito_sub = 'xxx-xxx-xxx';
```

### ✅ 检查应用日志

```bash
# 查看认证相关日志
docker logs doordash-app 2>&1 | grep -i "jwt\|authentication\|403"

# 查看详细的 DEBUG 日志
docker logs doordash-app 2>&1 | grep "Token validated for user"
```

### ✅ 检查 SecurityConfig

```java
// 确保端点需要认证
.anyRequest().authenticated()  // ✓ 正确

// 而不是
.anyRequest().permitAll()  // ✗ 错误 - 所有请求都允许
```

---

## 📖 延伸阅读

### AWS Cognito 文档
- [Access Token vs ID Token](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-tokens-with-identity-providers.html)
- [JWT Token 结构](https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-id-token.html)

### Spring Security 文档
- [Authentication Architecture](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html)
- [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

### JWT 规范
- [RFC 7519 - JSON Web Token](https://datatracker.ietf.org/doc/html/rfc7519)
- [JWT.io - Token Debugger](https://jwt.io/)

---

## 🎓 总结

### 核心问题
使用 **Access Token**（不含用户属性）代替 **ID Token**（含完整用户信息）

### 最佳解决方案
**方案 1**: 修改 AuthService 返回 ID Token（1行代码修改）

### 关键学习点
1. 理解 Access Token 和 ID Token 的区别
2. 掌握 Spring Security 认证流程
3. 学会分析 JWT Token 内容
4. 熟悉 SecurityContext 和 Authentication

### 下一步
1. 实施推荐修复方案
2. 运行测试脚本验证
3. 检查所有API端点是否正常工作
4. 监控生产环境日志

---

**文档版本**: 1.0  
**创建日期**: 2026-01-21  
**最后更新**: 2026-01-21  
**作者**: DoorDash Delivery Simulator Team
