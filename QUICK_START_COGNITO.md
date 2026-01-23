# 🚀 AWS Cognito 快速启动指南

## ✅ 已配置信息

你的 AWS Cognito User Pool 已经创建并配置好了：

- **User Pool Name**: User pool - m1aign
- **User Pool ID**: `us-east-1_a6gt5CsAi`
- **App Client ID**: `7fv4l4ftq2qriojlfcrmu5a2d9`
- **Region**: `us-east-1`
- **App Client Name**: california bear marketplace

配置文件 `application.yml` 已更新 ✅

---

## 🔧 下一步操作

### 步骤 1: 配置 AWS 凭证

**方式 1: 环境变量**（推荐用于本地开发）
```bash
export AWS_ACCESS_KEY_ID=your_access_key_id
export AWS_SECRET_ACCESS_KEY=your_secret_access_key
export AWS_DEFAULT_REGION=us-east-1
```

**方式 2: AWS CLI 配置**
```bash
aws configure
# 输入你的 Access Key ID
# 输入你的 Secret Access Key
# 默认区域: us-east-1
# 输出格式: json
```

**方式 3: Docker Compose（推荐用于容器化）**

创建 `.env` 文件：
```bash
AWS_ACCESS_KEY_ID=your_access_key_id
AWS_SECRET_ACCESS_KEY=your_secret_access_key
AWS_DEFAULT_REGION=us-east-1
```

然后更新 `docker-compose.yml`：
```yaml
services:
  app:
    environment:
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
      - AWS_DEFAULT_REGION=${AWS_DEFAULT_REGION}
```

### 步骤 2: 更新数据库（清除旧数据）

由于 `User` 表结构改变（删除了 password 字段，添加了 cognito_sub），需要重建数据库：

```bash
# 停止所有容器
docker-compose down

# 删除数据库卷（这会清空所有数据）
docker volume rm doordash_postgres_data

# 重新启动（会自动创建新表结构）
docker-compose up -d --build
```

### 步骤 3: 验证应用启动

```bash
# 查看日志
docker logs doordash-app -f

# 测试健康检查
curl http://localhost:8080/api/health
```

### 步骤 4: 测试用户注册

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+12345678901",
    "role": "CUSTOMER"
  }'
```

**预期结果**：
1. 用户在 Cognito 中创建（密码存储在 Cognito）
2. 本地数据库创建用户记录（不含密码，包含 cognitoSub）
3. 返回 UserDTO

---

## 🔍 验证 Cognito 用户

### 在 AWS Console 查看：
1. 进入 Cognito User Pool: `User pool - m1aign`
2. 点击左侧 "Users"
3. 应该能看到刚创建的用户

### 或者使用 AWS CLI：
```bash
aws cognito-idp list-users \
  --user-pool-id us-east-1_a6gt5CsAi \
  --region us-east-1
```

---

## 📝 用户认证流程（前端）

### 使用 AWS Amplify（推荐）

**安装**：
```bash
npm install aws-amplify @aws-amplify/ui-react
```

**配置**：
```javascript
import { Amplify } from 'aws-amplify';

Amplify.configure({
  Auth: {
    region: 'us-east-1',
    userPoolId: 'us-east-1_a6gt5CsAi',
    userPoolWebClientId: '7fv4l4ftq2qriojlfcrmu5a2d9',
  }
});
```

**登录**：
```javascript
import { Auth } from 'aws-amplify';

async function signIn(email, password) {
  try {
    const user = await Auth.signIn(email, password);
    const session = await Auth.currentSession();
    const accessToken = session.getAccessToken().getJwtToken();
    
    // 使用 token 调用后端 API
    const response = await fetch('http://localhost:8080/api/users/1', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });
    
    return user;
  } catch (error) {
    console.error('登录失败:', error);
  }
}
```

---

## ⚠️ 重要注意事项

### 1. AWS 凭证安全
- ❌ **永远不要**将 AWS 凭证提交到 Git
- ✅ 使用环境变量或 AWS IAM Role
- ✅ `.env` 文件应该添加到 `.gitignore`

### 2. Cognito 自定义属性

如果需要添加 `custom:role` 属性，需要在 Cognito User Pool 中配置：

1. 进入 User Pool: `User pool - m1aign`
2. 点击左侧 "Sign-up experience"
3. 找到 "Custom attributes"
4. 添加自定义属性：
   - 名称: `role`
   - 类型: String
   - 最小长度: 1
   - 最大长度: 50

### 3. 数据迁移

**如果你有现有用户数据**：
- 现有用户需要重新注册
- 或者使用 AWS CLI 批量导入到 Cognito
- 密码无法迁移（由于加密方式不同）

**如果是新项目**：
- 直接删除旧数据，重新开始 ✅

---

## 🧪 测试清单

- [ ] AWS 凭证配置正确
- [ ] 数据库重建完成
- [ ] 应用启动成功（无错误日志）
- [ ] 健康检查通过
- [ ] 用户注册成功
- [ ] Cognito 中能看到用户
- [ ] 本地数据库有用户记录（包含 cognito_sub）

---

## 🐛 常见问题

### Q: 启动时报错 "Unable to load credentials"
**A**: AWS 凭证未配置
```bash
# 设置环境变量
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
```

### Q: 用户注册失败 "User already exists"
**A**: 
1. 在 Cognito Console 中删除用户
2. 或使用不同的邮箱

### Q: 数据库表结构不正确
**A**: 
```bash
# 清空数据库重建
docker-compose down
docker volume rm doordash_postgres_data
docker-compose up -d --build
```

### Q: 如何在 Cognito 中删除用户？
**A**: 
```bash
aws cognito-idp admin-delete-user \
  --user-pool-id us-east-1_a6gt5CsAi \
  --username test@example.com \
  --region us-east-1
```

---

## 📚 相关文档

- **完整集成指南**: `docs/development/AWS-Cognito-Integration-Guide.md`
- **更改总结**: `COGNITO_CHANGES_SUMMARY.md`
- **AWS Cognito 文档**: https://docs.aws.amazon.com/cognito/

---

## 🎉 准备好了吗？

现在你可以：

1. ✅ 配置 AWS 凭证
2. ✅ 重建数据库
3. ✅ 重新启动应用
4. ✅ 测试用户注册

```bash
# 一键执行
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret
docker-compose down
docker volume rm doordash_postgres_data
docker-compose up -d --build

# 等待 10 秒后测试
sleep 10
curl http://localhost:8080/api/health
```

需要帮助吗？随时问我！ 🚀
