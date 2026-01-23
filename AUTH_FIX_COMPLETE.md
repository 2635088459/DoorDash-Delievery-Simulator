# ✅ 认证 API 修复完成

## 问题描述
前端应用在尝试注册和登录时遇到以下错误：
- 注册：POST `/api/auth/register` 返回 500 Internal Server Error
- 登录：POST `/api/auth/login` 返回 400 Bad Request

## 根本原因
后端应用原本依赖 **AWS Cognito** 进行用户认证，但我们的开发环境没有配置 Cognito 服务。这导致：
1. `AuthService` 调用 Cognito API 失败
2. `AuthController` 缺少 `register` 端点
3. User 实体没有 `password_hash` 字段

## 解决方案

### 1. 数据库修改
添加 `password_hash` 字段到 `users` 表：
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
```

### 2. 实体层修改
在 `User.java` 中添加密码字段：
```java
@Column(name = "password_hash", length = 255)
private String passwordHash;
```

### 3. 依赖添加
在 `pom.xml` 中添加 JJWT 依赖（用于 JWT 生成和验证）：
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### 4. 安全层修改
创建 `JwtTokenProvider.java`：
- `generateToken(email)` - 生成 Access Token（24小时有效期）
- `generateRefreshToken(email)` - 生成 Refresh Token（7天有效期）
- `validateToken(token)` - 验证 Token 是否有效
- `getEmailFromToken(token)` - 从 Token 中提取用户邮箱

### 5. DTO 层修改
创建 `RegisterRequest.java`：
```java
{
  "email": "user@example.com",        // Required, Email 格式
  "password": "password123",          // Required, 最少6字符
  "firstName": "John",                // Required
  "lastName": "Doe",                  // Required
  "phone": "+1234567890",             // Required
  "role": "CUSTOMER"                  // Required: CUSTOMER | RESTAURANT_OWNER | DRIVER
}
```

### 6. 服务层修改
重写 `AuthService.java`（本地版本）：
- `register()` - 用户注册，使用 BCrypt 加密密码
- `login()` - 用户登录，验证密码并生成 JWT
- `refreshToken()` - 刷新 Token
- `logout()` - 登出（本地版本简化实现）
- `getUserByEmail()` - 根据邮箱获取用户信息

### 7. 控制器层修改
更新 `AuthController.java`：
- 添加 `POST /api/auth/register` 端点
- 保持现有的 `POST /api/auth/login` 端点
- 保持现有的 `POST /api/auth/refresh` 端点
- 保持现有的 `POST /api/auth/logout` 端点
- 保持现有的 `GET /api/auth/me` 端点

## API 端点文档

### 1. 用户注册
**请求：**
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "role": "CUSTOMER"
}
```

**响应（成功）：**
```json
{
  "idToken": null,
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 86400,
  "tokenType": "Bearer",
  "user": {
    "id": 9,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER",
    "isActive": true,
    "createdAt": "2026-01-21T23:04:38.201476",
    "updatedAt": "2026-01-21T23:04:38.201542",
    "fullName": "John Doe"
  }
}
```

**响应（失败）：**
```json
{
  "timestamp": "2026-01-21T23:04:27.856241591",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists: user@example.com",
  "path": "/api/auth/register"
}
```

### 2. 用户登录
**请求：**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**响应（成功）：**
```json
{
  "idToken": null,
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 86400,
  "tokenType": "Bearer",
  "user": {
    "id": 9,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER",
    "isActive": true,
    "createdAt": "2026-01-21T23:04:38.201476",
    "updatedAt": "2026-01-21T23:04:38.201542",
    "fullName": "John Doe"
  }
}
```

**响应（失败）：**
```json
{
  "timestamp": "2026-01-21T23:05:00.123456789",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

### 3. 获取当前用户信息
**请求：**
```http
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**响应：**
```json
{
  "id": 9,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER",
  "isActive": true,
  "createdAt": "2026-01-21T23:04:38.201476",
  "updatedAt": "2026-01-21T23:04:38.201542",
  "fullName": "John Doe"
}
```

## 测试结果

### 后端测试
使用 `test-auth.sh` 脚本测试：
```bash
./test-auth.sh
```

**结果：**
- ✅ 用户注册成功
- ✅ 用户登录成功
- ✅ 获取用户信息成功
- ✅ JWT Token 生成正常
- ✅ 密码加密存储（BCrypt）

### 前端测试步骤
1. 打开浏览器访问 `http://localhost:3000/register`
2. 填写注册表单：
   - Email: `test@example.com`
   - Password: `password123`
   - First Name: `Test`
   - Last Name: `User`
   - Phone: `+1234567890`
   - Role: `Customer`
3. 点击"注册"按钮
4. **预期结果**：
   - 自动跳转到首页（`/`）
   - LocalStorage 中保存了 token
   - Navbar 显示用户信息
   - WebSocket 自动连接

5. 测试登录：
   - 访问 `http://localhost:3000/login`
   - 输入邮箱和密码
   - 点击"登录"按钮
   - **预期结果**：同注册成功

## 技术细节

### 密码加密
使用 **BCryptPasswordEncoder**（Spring Security 提供）：
- 盐值自动生成
- 每次加密结果不同（即使密码相同）
- 单向加密，无法解密
- 验证时使用 `passwordEncoder.matches(rawPassword, encodedPassword)`

### JWT Token 结构
```
Header:
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload:
{
  "sub": "user@example.com",   // 用户邮箱
  "iat": 1769036678,            // 发行时间
  "exp": 1769123078,            // 过期时间
  "type": "refresh"             // Token 类型（仅 Refresh Token 有此字段）
}

Signature:
HMACSHA512(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Token 管理
- **Access Token**: 24小时有效期，用于 API 认证
- **Refresh Token**: 7天有效期，用于刷新 Access Token
- **存储位置**: LocalStorage（前端）
- **传输方式**: Authorization Header (`Bearer {token}`)

## 已知问题和改进建议

### 当前实现
- ✅ 基本认证功能完整
- ✅ 密码安全加密
- ✅ JWT Token 生成和验证
- ⚠️ Token 黑名单未实现（登出后 Token 仍然有效）
- ⚠️ 密码重置功能未实现
- ⚠️ 邮箱验证未实现

### 未来改进
1. **Token 黑名单**：使用 Redis 存储已登出的 Token
2. **密码重置**：发送重置邮件，生成临时 Token
3. **邮箱验证**：注册后发送验证邮件
4. **双因素认证**：支持 TOTP（如 Google Authenticator）
5. **OAuth 集成**：支持 Google、Facebook 登录
6. **密码策略**：强制复杂密码（大小写、数字、特殊字符）
7. **登录日志**：记录登录时间、IP、设备信息
8. **异常登录检测**：检测异常登录行为并发送通知

## 部署说明

### Docker 重新构建
修改代码后需要重新构建 Docker 镜像：
```bash
docker-compose down
docker-compose up -d --build
```

### 数据库迁移
新环境部署时，需要运行数据库迁移：
```sql
-- 如果 password_hash 字段不存在
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
```

### 环境变量
在 `application.yml` 中配置（可选）：
```yaml
jwt:
  secret: your-secret-key-change-this-in-production
  expiration: 86400000           # 24小时
  refresh-expiration: 604800000  # 7天
```

**注意**：生产环境必须修改 JWT secret，使用至少256位的随机字符串！

## 总结
认证系统已从 **AWS Cognito** 迁移到 **本地 JWT 认证**，完全支持用户注册、登录和 Token 管理。前端应用现在可以正常使用认证功能。

---
**修复时间**: 2026-01-21 23:00  
**修复人**: GitHub Copilot  
**测试状态**: ✅ 通过
