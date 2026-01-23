# AWS 服务使用指南 - DoorDash 项目

## 📋 AWS 服务创建时机和顺序

根据项目发展阶段，逐步引入 AWS 服务，避免一开始就配置所有服务（浪费钱💰）。

---

## 🎯 项目发展阶段 vs AWS 服务

### 阶段 1️⃣：MVP 原型阶段（当前）
**目标**：快速验证想法，展示基本功能

#### 需要创建的服务：

1. **EC2 - 计算服务** ⭐⭐⭐⭐⭐ **必须，现在创建**
   - **什么时候创建**：立即（你已经有了完整的应用代码）
   - **用途**：运行 Spring Boot 应用 + PostgreSQL + Docker
   - **推荐配置**：
     - 实例类型：t2.micro（免费套餐）或 t3.small（$15/月）
     - 存储：20-30GB gp3
   - **费用**：$0-15/月

2. **Route 53 - DNS 服务** ⭐⭐⭐ **可选，有域名时创建**
   - **什么时候创建**：当你购买了域名后（如 doordash-demo.com）
   - **用途**：域名解析，将域名指向 EC2
   - **费用**：$0.5/月 + $0.4/百万次查询

#### 不需要的服务：
- ❌ S3：暂时不需要（没有文件上传功能）
- ❌ Cognito：暂时不需要（先用简单的用户表）
- ❌ RDS：暂时不需要（用 Docker PostgreSQL 就够了）
- ❌ ELB：暂时不需要（流量小）
- ❌ CloudFront：暂时不需要（没有静态资源）

---

### 阶段 2️⃣：功能完善阶段
**目标**：添加文件上传、用户认证等功能

#### 新增服务：

3. **S3 - 对象存储** ⭐⭐⭐⭐ **有文件上传时创建**
   - **什么时候创建**：当你需要以下功能时：
     - ✅ 用户头像上传
     - ✅ 餐厅图片上传
     - ✅ 菜品图片上传
     - ✅ 评价照片上传
   - **用途**：存储图片、视频等文件
   - **为什么用 S3**：
     - 不占用 EC2 磁盘空间
     - CDN 加速（配合 CloudFront）
     - 高可用性（99.999999999% 持久性）
   - **费用**：
     - 存储：$0.023/GB/月（前 50TB）
     - 请求：$0.0004/千次 PUT，$0.00004/千次 GET
     - 示例：1000 张图片（5GB）≈ $0.12/月

4. **Cognito - 用户认证** ⭐⭐⭐⭐ **需要 OAuth 时创建**
   - **什么时候创建**：当你需要以下功能时：
     - ✅ 社交登录（Google、Facebook、Apple 登录）
     - ✅ 多因素认证（MFA）
     - ✅ 忘记密码、邮箱验证
     - ✅ JWT Token 管理
   - **用途**：完整的用户认证解决方案
   - **为什么用 Cognito**：
     - 不需要自己写认证逻辑
     - 自动处理密码加密、Token 刷新
     - 符合安全标准（GDPR、HIPAA）
   - **费用**：
     - 前 50,000 活跃用户免费
     - 超出后：$0.0055/MAU（月活跃用户）

#### 暂时不需要：
- ❌ RDS：数据量小于 20GB，Docker PostgreSQL 够用
- ❌ ElastiCache：没有缓存需求
- ❌ SES：暂时不发邮件

---

### 阶段 3️⃣：生产环境阶段
**目标**：高可用、高性能、安全

#### 新增服务：

5. **RDS - 托管数据库** ⭐⭐⭐⭐⭐ **数据重要时创建**
   - **什么时候创建**：当出现以下情况时：
     - ✅ 数据库 > 20GB
     - ✅ 需要自动备份和恢复
     - ✅ 需要读写分离（主从复制）
     - ✅ EC2 磁盘空间不足
   - **用途**：替代 Docker PostgreSQL
   - **优势**：
     - 自动备份（每天）
     - 一键恢复到任意时间点
     - 自动故障转移
     - 性能监控
   - **费用**：
     - db.t3.micro：$15/月
     - db.t3.small：$30/月
     - 存储：$0.115/GB/月

6. **CloudFront - CDN** ⭐⭐⭐⭐ **有全球用户时创建**
   - **什么时候创建**：
     - ✅ 用户分布在多个国家/地区
     - ✅ 有大量静态资源（图片、视频）
     - ✅ 需要加速 API 响应
   - **用途**：内容分发网络，加速访问
   - **费用**：
     - 前 1TB 免费（12个月）
     - $0.085/GB（美国）

7. **ELB - 负载均衡器** ⭐⭐⭐⭐ **需要高可用时创建**
   - **什么时候创建**：
     - ✅ 需要运行多个 EC2 实例
     - ✅ 需要自动故障转移
     - ✅ 需要 SSL 终止
   - **用途**：分发流量到多个服务器
   - **费用**：$16/月 + 数据传输费

8. **CloudWatch - 监控和日志** ⭐⭐⭐⭐⭐ **立即创建**
   - **什么时候创建**：部署到生产环境时
   - **用途**：
     - 监控 CPU、内存、磁盘
     - 设置告警（CPU > 80%）
     - 收集应用日志
   - **费用**：
     - 基础监控：免费
     - 自定义指标：$0.30/指标/月
     - 日志存储：$0.50/GB

---

### 阶段 4️⃣：大规模运营阶段
**目标**：处理大量用户和数据

#### 新增服务：

9. **ElastiCache (Redis)** - 缓存
   - **什么时候创建**：
     - ✅ 数据库查询慢
     - ✅ 需要会话管理
     - ✅ 需要实时排行榜、计数器
   - **费用**：$15/月起

10. **SQS - 消息队列**
    - **什么时候创建**：
      - ✅ 需要异步处理（订单通知、邮件发送）
      - ✅ 需要削峰填谷
    - **费用**：前 100万次请求免费

11. **Lambda - 无服务器计算**
    - **什么时候创建**：
      - ✅ 定时任务（每日报表）
      - ✅ 事件触发（图片上传后自动压缩）
    - **费用**：前 100万次请求免费

12. **SES - 邮件服务**
    - **什么时候创建**：
      - ✅ 需要发送订单通知邮件
      - ✅ 需要发送营销邮件
    - **费用**：前 62,000 封免费

---

## 📅 创建时间表（推荐）

### 第 1 周：基础设施
```
Day 1-2: 创建 EC2 实例，部署应用
Day 3-4: 配置 Nginx，设置 HTTPS
Day 5-7: 测试，修复 Bug
```

**需要创建的服务：**
- ✅ EC2
- ✅ Route 53（如果有域名）
- ✅ CloudWatch（基础监控）

**预算：** $0-20/月

---

### 第 2-4 周：功能开发
```
Week 2: 添加用户头像上传功能
Week 3: 添加餐厅图片上传功能
Week 4: 添加社交登录功能
```

**需要创建的服务：**
- ✅ S3（图片上传后）
- ✅ Cognito（需要 OAuth 时）

**预算：** $20-40/月

---

### 第 2-3 个月：生产环境优化
```
Month 2: 数据库迁移到 RDS
Month 3: 配置 CDN，添加缓存
```

**需要创建的服务：**
- ✅ RDS
- ✅ CloudFront
- ✅ ElastiCache

**预算：** $80-150/月

---

### 第 4-6 个月：扩展和优化
```
Month 4: 负载均衡，多实例
Month 5: 异步处理，消息队列
Month 6: 监控告警，自动扩展
```

**需要创建的服务：**
- ✅ ELB
- ✅ SQS
- ✅ Lambda
- ✅ Auto Scaling

**预算：** $200-400/月

---

## 🎯 具体创建指南

### 现在就创建：EC2 + CloudWatch

#### 1. 创建 EC2（参考已有文档）
```bash
# 按照 AWS-EC2-Deployment-Guide.md 操作
```

#### 2. 设置 CloudWatch 基础监控
```bash
# SSH 到 EC2
ssh -i doordash-key.pem ubuntu@YOUR_IP

# 安装 CloudWatch Agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i amazon-cloudwatch-agent.deb

# 配置 CloudWatch（收集应用日志）
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

---

### 添加文件上传功能时：创建 S3

#### 什么时候需要？
- 用户上传头像
- 餐厅上传店铺图片
- 菜品上传图片

#### 创建步骤：

1. **创建 S3 Bucket**
   ```bash
   # AWS 控制台 → S3 → Create bucket
   Bucket name: doordash-uploads
   Region: us-east-1（与 EC2 同区域）
   Block all public access: ❌ 取消勾选（如果需要公开访问图片）
   Versioning: 启用（可以恢复删除的文件）
   ```

2. **配置 CORS（跨域）**
   ```json
   [
     {
       "AllowedHeaders": ["*"],
       "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
       "AllowedOrigins": ["*"],
       "ExposeHeaders": []
     }
   ]
   ```

3. **创建 IAM 用户（程序访问）**
   ```bash
   # IAM → Users → Add user
   User name: doordash-app
   Access type: Programmatic access
   
   # 附加策略
   AmazonS3FullAccess（或自定义最小权限）
   
   # 记录 Access Key ID 和 Secret Access Key
   ```

4. **Spring Boot 集成 S3**
   ```xml
   <!-- pom.xml 添加依赖 -->
   <dependency>
       <groupId>com.amazonaws</groupId>
       <artifactId>aws-java-sdk-s3</artifactId>
       <version>1.12.529</version>
   </dependency>
   ```

   ```yaml
   # application.yml
   aws:
     s3:
       bucket-name: doordash-uploads
       region: us-east-1
     credentials:
       access-key: YOUR_ACCESS_KEY
       secret-key: YOUR_SECRET_KEY
   ```

---

### 添加社交登录时：创建 Cognito

#### 什么时候需要？
- Google 登录
- Facebook 登录
- Apple 登录
- 邮箱验证、忘记密码

#### 创建步骤：

1. **创建 User Pool**
   ```bash
   # AWS 控制台 → Cognito → Create user pool
   Pool name: doordash-users
   Sign-in options: Email, Username
   Password policy: 默认
   MFA: Optional（可选多因素认证）
   ```

2. **配置 App Client**
   ```bash
   App client name: doordash-web
   Generate client secret: No（前端应用不需要）
   
   # 记录
   User Pool ID: us-east-1_XXXXXX
   App Client ID: xxxxxxxxxxxx
   ```

3. **配置身份提供商**
   ```bash
   # Identity providers → Add provider
   
   Google:
     Client ID: 从 Google Cloud Console 获取
     Client secret: 从 Google Cloud Console 获取
   
   Facebook:
     App ID: 从 Facebook Developers 获取
     App secret: 从 Facebook Developers 获取
   ```

4. **Spring Boot 集成 Cognito**
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-oauth2-client</artifactId>
   </dependency>
   ```

---

### 数据量增长时：迁移到 RDS

#### 什么时候需要？
- 数据库 > 20GB
- 需要自动备份
- 需要更好的性能

#### 创建步骤：

1. **创建 RDS 实例**
   ```bash
   # AWS 控制台 → RDS → Create database
   Engine: PostgreSQL 16
   Template: Free tier（测试）或 Production（生产）
   
   DB instance identifier: doordash-db
   Master username: postgres
   Master password: 强密码
   
   DB instance class: db.t3.micro（$15/月）
   Storage: 20GB gp3
   
   VPC: 与 EC2 相同
   Public access: No（安全考虑）
   ```

2. **配置安全组**
   ```bash
   # 允许 EC2 访问 RDS
   Type: PostgreSQL
   Protocol: TCP
   Port: 5432
   Source: EC2 安全组 ID
   ```

3. **迁移数据**
   ```bash
   # 在 EC2 上导出当前数据
   docker exec doordash-postgres pg_dump -U postgres doordash_db > backup.sql
   
   # 导入到 RDS
   psql -h doordash-db.xxxxxx.us-east-1.rds.amazonaws.com \
        -U postgres -d doordash_db < backup.sql
   
   # 修改 application.yml
   spring:
     datasource:
       url: jdbc:postgresql://doordash-db.xxxxxx.us-east-1.rds.amazonaws.com:5432/doordash_db
   ```

---

## 💰 成本对比

### MVP 阶段（1-3 个月）
```
EC2 (t2.micro/免费套餐):     $0
或 EC2 (t3.small):           $15/月
Route 53:                    $1/月（可选）
CloudWatch:                  $0（基础监控免费）
────────────────────────────
总计:                        $0-16/月
```

### 功能完善阶段（3-6 个月）
```
EC2 (t3.small):              $15/月
S3 (5GB 图片):               $0.5/月
Cognito (1000 用户):         $0（免费额度）
Route 53:                    $1/月
CloudWatch:                  $5/月
────────────────────────────
总计:                        $21.5/月
```

### 生产环境阶段（6+ 个月）
```
EC2 (t3.medium):             $30/月
RDS (db.t3.small):           $30/月
S3 (50GB):                   $5/月
CloudFront:                  $10/月
ElastiCache (小型):          $15/月
ELB:                         $16/月
Cognito:                     $0-10/月
其他服务:                    $10/月
────────────────────────────
总计:                        $116-126/月
```

---

## ✅ 决策树

```
开始
  │
  ├─ 有完整应用代码？
  │   └─ Yes → 创建 EC2 ✅
  │
  ├─ 有域名？
  │   └─ Yes → 创建 Route 53 ✅
  │
  ├─ 需要上传文件（头像、图片）？
  │   └─ Yes → 创建 S3 ✅
  │
  ├─ 需要社交登录（Google/Facebook）？
  │   └─ Yes → 创建 Cognito ✅
  │
  ├─ 数据库 > 20GB 或需要自动备份？
  │   └─ Yes → 创建 RDS ✅
  │
  ├─ 有全球用户？
  │   └─ Yes → 创建 CloudFront ✅
  │
  ├─ 需要高可用（多服务器）？
  │   └─ Yes → 创建 ELB ✅
  │
  └─ 需要缓存？
      └─ Yes → 创建 ElastiCache ✅
```

---

## 🎯 你现在应该做什么？

### 立即行动（本周）：
1. ✅ **创建 EC2 实例**（按照已有文档）
2. ✅ **部署应用**
3. ✅ **配置基础监控**（CloudWatch）
4. ✅ 如果有域名 → 配置 Route 53

### 暂时不要创建：
- ❌ S3（等有文件上传功能时）
- ❌ Cognito（等需要社交登录时）
- ❌ RDS（数据量小，Docker PostgreSQL 够用）
- ❌ 其他所有服务

### 节省成本技巧：
1. 使用免费套餐（t2.micro 免费 750 小时/月）
2. 先用最小配置，按需升级
3. 设置预算告警（AWS Budgets）
4. 定期检查未使用的资源

---

## 🚀 总结

**现在就创建：**
- EC2（运行应用）
- Route 53（如果有域名）

**2-4 周后创建：**
- S3（文件上传功能）
- Cognito（社交登录功能）

**2-3 个月后创建：**
- RDS（数据量增长）
- CloudFront（性能优化）

**6 个月后创建：**
- ELB（高可用）
- ElastiCache（缓存）
- 其他服务

**记住：先把应用跑起来，再逐步优化！** 🎉
