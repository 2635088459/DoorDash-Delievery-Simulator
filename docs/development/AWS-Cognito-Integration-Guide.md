# AWS Cognito Integration Guide for DoorDash Simulator

## 🎯 已完成的更改

### 1. ✅ User Entity 更新
- **删除**：`password` 字段（密码由 Cognito 管理）
- **添加**：`cognitoSub` 字段（链接到 Cognito 用户）

```java
// 之前
@Column(nullable = false, length = 255)
private String password;

// 现在
@Column(name = "cognito_sub", unique = true, length = 255)
private String cognitoSub;  // AWS Cognito User Sub
```

### 2. ✅ 添加 AWS SDK 依赖
在 `pom.xml` 中添加了：
- AWS Cognito Identity Provider SDK
- AWS Core Authentication

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>cognitoidentityprovider</artifactId>
    <version>2.21.0</version>
</dependency>
```

### 3. ✅ 创建 CognitoConfig
配置 Cognito 客户端

### 4. ✅ 创建 CognitoService
提供以下功能：
- `signUpUser()` - 在 Cognito 注册用户
- `deleteUser()` - 删除 Cognito 用户
- `enableUser()` - 启用用户
- `disableUser()` - 禁用用户
- `updateUserAttributes()` - 更新用户属性
- `validateToken()` - 验证 JWT 令牌

### 5. ✅ 更新 UserService
- 创建用户时调用 Cognito
- 删除用户时同步删除 Cognito 用户
- 激活/停用时同步更新 Cognito 状态

---

## 🔧 需要配置的内容

### 1. application.yml 配置

添加以下配置到 `src/main/resources/application.yml`:

```yaml
aws:
  cognito:
    region: us-east-1  # AWS 区域
    userPoolId: YOUR_USER_POOL_ID  # 替换为你的 User Pool ID
    clientId: YOUR_CLIENT_ID        # 替换为你的 App Client ID

# 本地开发时使用的 AWS 凭证
# 方式 1: 环境变量（推荐）
# export AWS_ACCESS_KEY_ID=your_access_key
# export AWS_SECRET_ACCESS_KEY=your_secret_key

# 方式 2: AWS CLI 配置
# ~/.aws/credentials
```

### 2. 创建 AWS Cognito User Pool

#### 步骤：

**1. 登录 AWS Console**
- 进入 Cognito 服务

**2. 创建 User Pool**
```bash
名称: doordash-user-pool
登录方式: Email
密码策略: 
  - 最小长度: 6
  - 需要数字
  - 需要特殊字符
```

**3. 添加自定义属性**
```
custom:role (String) - 存储用户角色
```

**4. 创建 App Client**
```bash
名称: doordash-app-client
认证流程: ALLOW_ADMIN_USER_PASSWORD_AUTH
```

**5. 获取配置信息**
- User Pool ID: `us-east-1_xxxxxxx`
- App Client ID: `xxxxxxxxxxxxxxxxxxxxxxxxxx`

### 3. 数据库迁移

因为删除了 `password` 字段，需要更新数据库：

```sql
-- 删除 password 列
ALTER TABLE users DROP COLUMN password;

-- 添加 cognito_sub 列
ALTER TABLE users ADD COLUMN cognito_sub VARCHAR(255) UNIQUE;
```

**或者重新创建表**（如果数据可以清空）：
```bash
# 停止应用
docker-compose down

# 删除数据库卷
docker volume rm doordash_postgres_data

# 重新启动（Hibernate 会自动创建新表结构）
docker-compose up -d --build
```

---

## 📝 API 使用示例

### 1. 注册新用户

```bash
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "Pass@123",  # 会存储到 Cognito，不存储到本地数据库
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER"
}
```

**流程**：
1. UserController 接收请求
2. UserService 调用 CognitoService 在 Cognito 创建用户
3. 获取 Cognito Sub
4. 在本地数据库创建用户记录（不含密码，只有 cognitoSub）
5. 返回 UserDTO

### 2. 用户登录（由前端直接调用 Cognito）

```javascript
// 前端使用 AWS Amplify 或 Cognito SDK
import { CognitoUserPool, AuthenticationDetails, CognitoUser } from 'amazon-cognito-identity-js';

const authenticationData = {
  Username: 'john@example.com',
  Password: 'Pass@123',
};

const authenticationDetails = new AuthenticationDetails(authenticationData);

const userData = {
  Username: 'john@example.com',
  Pool: userPool
};

const cognitoUser = new CognitoUser(userData);

cognitoUser.authenticateUser(authenticationDetails, {
  onSuccess: (result) => {
    const accessToken = result.getAccessToken().getJwtToken();
    const idToken = result.getIdToken().getJwtToken();
    // 使用 token 调用后端 API
  },
  onFailure: (err) => {
    console.error(err);
  }
});
```

### 3. 调用受保护的 API

```bash
GET http://localhost:8080/api/users/1
Authorization: Bearer <Cognito_JWT_Token>
```

---

## 🗑️ 可以删除的代码/依赖

### 1. 删除 JWT 依赖（不再需要自己生成 JWT）

从 `pom.xml` 删除：
```xml
<!-- 删除这些，因为 Cognito 已经提供 JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
</dependency>
```

### 2. 删除密码相关的代码

**UserRepository.java** - 不需要这些方法：
```java
// 可以删除（不再需要）
// findByEmailAndPassword()
// updatePassword()
```

**UserDTO.java** - 确保不包含 password：
```java
// ✅ 正确 - 不包含 password
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    // ... 其他字段，但没有 password
}
```

---

## 🔐 安全配置

### Spring Security 配置（需要添加）

创建 `SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/register").permitAll()  // 允许注册
                .requestMatchers("/api/health").permitAll()          // 允许健康检查
                .anyRequest().authenticated()                        // 其他需要认证
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())  // 验证 Cognito JWT
                )
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = String.format(
            "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
            "us-east-1",  // 替换为你的 region
            "YOUR_USER_POOL_ID"  // 替换为你的 User Pool ID
        );
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

---

## 📊 架构对比

### 之前（本地密码管理）：
```
前端 → Spring Boot → 数据库
      ↓
    验证密码（BCrypt）
    生成 JWT
    管理会话
```

### 现在（Cognito）：
```
前端 → AWS Cognito（认证）→ 获取 JWT
       ↓
前端 → Spring Boot（带 JWT）→ 数据库
       ↓
     验证 JWT（Cognito 签发）
     获取用户信息
```

**优势**：
- ✅ 密码安全由 AWS 管理
- ✅ 内置 MFA、密码重置等功能
- ✅ JWT 由 Cognito 签发和验证
- ✅ 符合 AWS 最佳实践
- ✅ 可扩展性强

---

## 🚀 下一步

1. **配置 AWS Cognito**
   - 创建 User Pool
   - 创建 App Client
   - 获取配置信息

2. **更新 application.yml**
   - 添加 Cognito 配置

3. **重新构建应用**
   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

4. **测试注册功能**
   ```bash
   POST /api/users
   ```

5. **前端集成**
   - 使用 AWS Amplify
   - 或者 amazon-cognito-identity-js

---

## 📖 参考文档

- [AWS Cognito Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/)
- [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Spring Security OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

---

**需要帮助？**
- AWS Cognito 创建问题
- Spring Security 配置
- JWT 验证配置
- 前端集成

随时问我！ 🎉
