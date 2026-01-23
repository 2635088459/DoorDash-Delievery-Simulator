# AWS Cognito 集成更改总结

## ✅ 已完成的代码更改

### 1. **User.java** - 删除密码字段，添加 Cognito Sub
```java
// 删除
private String password;

// 添加
@Column(name = "cognito_sub", unique = true, length = 255)
private String cognitoSub;
```

### 2. **pom.xml** - 添加 AWS SDK 依赖
```xml
<!-- 添加 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>cognitoidentityprovider</artifactId>
    <version>2.21.0</version>
</dependency>

<!-- 删除（不再需要） -->
<!-- JWT dependencies (jjwt-api, jjwt-impl, jjwt-jackson) -->
```

### 3. **新文件**：
- ✅ `CognitoConfig.java` - AWS Cognito 客户端配置
- ✅ `CognitoService.java` - Cognito 用户管理服务
- ✅ `AWS-Cognito-Integration-Guide.md` - 完整集成文档

### 4. **UserService.java** - 更新为使用 Cognito
- `createUser()` - 现在调用 Cognito 创建用户
- `deleteUser()` - 同时删除 Cognito 和本地用户
- `toggleUserStatus()` - 同步更新 Cognito 状态

---

## 🔧 你需要做的配置

### 步骤 1: 在 AWS Console 创建 Cognito User Pool

1. 登录 AWS Console
2. 进入 Cognito 服务
3. 创建 User Pool：
   - 名称：`doordash-user-pool`
   - 登录方式：Email
   - 密码策略：最少 6 位
4. 添加自定义属性：`custom:role`
5. 创建 App Client：`doordash-app-client`
6. 记录：
   - **User Pool ID**: `us-east-1_xxxxxxx`
   - **App Client ID**: `xxxxxxxxxx`

### 步骤 2: 配置 application.yml

在 `src/main/resources/application.yml` 添加：

```yaml
aws:
  cognito:
    region: us-east-1
    userPoolId: us-east-1_xxxxxxx  # 替换为你的
    clientId: xxxxxxxxxxxxxxxxxx    # 替换为你的
```

### 步骤 3: 配置 AWS 凭证

**方式 1: 环境变量**（推荐）
```bash
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

**方式 2: AWS CLI**
```bash
aws configure
```

### 步骤 4: 更新数据库

**选项 A: 清空数据库重建**（推荐，如果没有重要数据）
```bash
docker-compose down
docker volume rm doordash_postgres_data
docker-compose up -d --build
```

**选项 B: 手动迁移**
```sql
ALTER TABLE users DROP COLUMN password;
ALTER TABLE users ADD COLUMN cognito_sub VARCHAR(255) UNIQUE;
```

### 步骤 5: 重新构建应用
```bash
docker-compose up -d --build
```

---

## 📝 使用方式

### 注册用户
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Pass@123",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER"
  }'
```

**流程**：
1. 密码存储到 Cognito（不存本地数据库）
2. 获取 Cognito Sub
3. 本地数据库保存用户信息（不含密码）

### 登录（前端直接调用 Cognito）
前端需要集成 AWS Amplify 或 Cognito SDK

---

## 🗑️ 可以删除的代码

### 从 pom.xml 删除：
```xml
<!-- 不再需要自己管理 JWT -->
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

### 从 UserRepository.java 删除（如果有的话）：
```java
// 不再需要密码相关查询
// findByEmailAndPassword()
// updatePassword()
```

---

## 🎯 好处

1. **安全性提升**
   - 密码由 AWS 管理，符合安全最佳实践
   - 自动哈希、加密
   - 防暴力破解

2. **功能丰富**
   - 内置 MFA
   - 邮箱验证
   - 密码重置
   - 账号锁定

3. **可扩展性**
   - 支持社交登录（Google、Facebook）
   - 企业 SSO（SAML）
   - 自定义认证流程

4. **成本**
   - 前 50,000 MAU 免费
   - 之后每 MAU $0.0055/month

---

## ⚠️ 注意事项

1. **AWS 凭证安全**
   - 不要提交到 Git
   - 使用环境变量
   - 生产环境使用 IAM Role

2. **数据迁移**
   - 现有用户需要重新注册
   - 或者手动迁移到 Cognito

3. **前端更新**
   - 需要集成 Cognito SDK
   - 登录流程改为直接调用 Cognito
   - 获取 JWT token 后调用后端 API

---

## 📚 参考文档

详细步骤请查看：
- `docs/development/AWS-Cognito-Integration-Guide.md`

AWS 文档：
- https://docs.aws.amazon.com/cognito/latest/developerguide/
- https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/

---

## 🤔 常见问题

**Q: 还需要 Spring Security 吗？**
A: 需要，用于验证 Cognito 签发的 JWT token

**Q: 密码验证在哪里？**
A: 在 Cognito，前端直接调用 Cognito API

**Q: 如何测试？**
A: 
1. 先配置好 Cognito User Pool
2. 更新 application.yml
3. 重新构建应用
4. 使用 Postman 测试注册

**Q: 本地开发怎么办？**
A: 
- 创建开发用的 Cognito User Pool
- 或者使用 LocalStack 模拟 Cognito

---

需要帮助吗？随时问我！ 🚀
