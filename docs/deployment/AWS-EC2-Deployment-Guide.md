# AWS EC2 部署指南 - DoorDash Simulator

## 📋 目录
1. [准备工作](#准备工作)
2. [创建 EC2 实例](#创建-ec2-实例)
3. [环境配置](#环境配置)
4. [部署应用](#部署应用)
5. [配置域名和 HTTPS](#配置域名和-https)
6. [监控和维护](#监控和维护)

---

## 🎯 部署架构

```
Internet
    ↓
AWS EC2 Instance (Ubuntu)
    ├── Docker
    │   ├── PostgreSQL Container (5432)
    │   ├── DoorDash App Container (8080)
    │   └── pgAdmin Container (5050)
    └── Nginx (80/443) → 反向代理到 App:8080
```

---

## 1️⃣ 准备工作

### 本地准备

#### 1.1 确保项目可以正常运行
```bash
# 在本地测试
cd ~/Desktop/DoorDash
docker-compose up -d --build

# 验证
curl http://localhost:8080/api/health
```

#### 1.2 创建生产环境配置
```bash
# 创建生产环境的 docker-compose 文件
cp docker-compose.yml docker-compose.prod.yml
```

---

## 2️⃣ 创建 EC2 实例

### 2.1 登录 AWS 控制台
1. 访问 [AWS Console](https://console.aws.amazon.com)
2. 选择区域（推荐：us-east-1 或离用户最近的区域）
3. 进入 EC2 服务

### 2.2 启动实例

#### 选择 AMI（操作系统镜像）
- **推荐**：Ubuntu Server 22.04 LTS
- 架构：64-bit (x86)

#### 选择实例类型

| 类型 | vCPU | 内存 | 适用场景 | 费用/月 |
|------|------|------|---------|---------|
| **t2.micro** | 1 | 1GB | 测试/学习（免费套餐） | $0 |
| **t3.small** | 2 | 2GB | 小型应用 | ~$15 |
| **t3.medium** | 2 | 4GB | 生产环境（推荐） | ~$30 |
| **t3.large** | 2 | 8GB | 高负载 | ~$60 |

**推荐：t3.medium（4GB 内存，足够运行 Docker 容器）**

#### 配置实例详细信息
- **网络**：默认 VPC
- **子网**：默认
- **自动分配公网 IP**：启用 ✅

#### 添加存储
- **大小**：20 GB（推荐 30GB）
- **卷类型**：通用型 SSD (gp3)

#### 配置安全组（重要！）

创建新的安全组，添加以下规则：

| 类型 | 协议 | 端口范围 | 来源 | 说明 |
|------|------|---------|------|------|
| SSH | TCP | 22 | 你的IP（My IP） | SSH 连接 |
| HTTP | TCP | 80 | 0.0.0.0/0 | 网站访问 |
| HTTPS | TCP | 443 | 0.0.0.0/0 | HTTPS 访问 |
| 自定义TCP | TCP | 8080 | 0.0.0.0/0 | 应用端口（可选，测试用） |
| 自定义TCP | TCP | 5050 | 你的IP | pgAdmin（可选） |

⚠️ **安全建议**：
- SSH (22) 只允许你的 IP
- 生产环境不要开放 5432 (PostgreSQL)
- 8080 端口测试后应关闭，只通过 Nginx 访问

#### 创建或选择密钥对
1. 创建新密钥对
2. 名称：`doordash-key`
3. 文件格式：`.pem`（Linux/Mac）或 `.ppk`（Windows PuTTY）
4. 下载并保存到安全位置

```bash
# 设置密钥权限（Mac/Linux）
chmod 400 ~/Downloads/doordash-key.pem
```

### 2.3 启动实例
- 点击"启动实例"
- 记录实例的**公网 IP 地址**（例如：54.123.45.67）

---

## 3️⃣ 环境配置

### 3.1 连接到 EC2

#### Mac/Linux
```bash
# 使用 SSH 连接
ssh -i ~/Downloads/doordash-key.pem ubuntu@54.123.45.67
# 替换为你的实例 IP
```

#### Windows
使用 PuTTY 或 Windows Terminal：
```bash
ssh -i C:\path\to\doordash-key.pem ubuntu@54.123.45.67
```

### 3.2 更新系统
```bash
# 更新包列表
sudo apt update

# 升级已安装的包
sudo apt upgrade -y
```

### 3.3 安装 Docker

```bash
# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 将当前用户添加到 docker 组（避免每次用 sudo）
sudo usermod -aG docker ubuntu

# 重新登录或执行
newgrp docker

# 验证安装
docker --version
# 应显示：Docker version 24.x.x
```

### 3.4 安装 Docker Compose

```bash
# 下载 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
# 应显示：Docker Compose version v2.x.x
```

### 3.5 安装 Git
```bash
sudo apt install git -y

# 验证
git --version
```

### 3.6 配置防火墙（可选，EC2 安全组已控制）
```bash
# Ubuntu 使用 UFW
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

---

## 4️⃣ 部署应用

### 4.1 上传代码到 EC2

#### 方式 1：使用 Git（推荐）
```bash
# 在 EC2 上克隆仓库
cd ~
git clone https://github.com/2635088459/DoorDash-Delievery-Simulator.git
cd DoorDash-Delievery-Simulator
```

#### 方式 2：使用 SCP 上传
```bash
# 在本地执行（Mac/Linux）
cd ~/Desktop
scp -i ~/Downloads/doordash-key.pem -r DoorDash ubuntu@54.123.45.67:~/

# 然后在 EC2 上
cd ~/DoorDash
```

### 4.2 配置生产环境变量

创建生产环境配置文件：

```bash
# 在 EC2 上创建 .env.prod 文件
nano .env.prod
```

添加以下内容：
```env
# 数据库配置
POSTGRES_DB=doordash_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password_here  # ⚠️ 改成强密码

# 应用配置
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/doordash_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_secure_password_here

# JPA 配置（生产环境建议用 validate）
SPRING_JPA_HIBERNATE_DDL_AUTO=update

# 服务器配置
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# pgAdmin 配置
PGADMIN_EMAIL=admin@example.com
PGADMIN_PASSWORD=admin_password_here
```

保存并退出（Ctrl+X，然后 Y，然后 Enter）

### 4.3 修改生产环境 docker-compose

```bash
# 编辑配置文件
nano docker-compose.prod.yml
```

修改内容：
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: doordash-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"  # 生产环境可以移除，只内部访问
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped  # 自动重启

  doordash-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: doordash-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SERVER_PORT: 8080
    ports:
      - "8080:8080"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: doordash-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_EMAIL}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD}
    ports:
      - "5050:80"
    depends_on:
      - postgres
    restart: unless-stopped

volumes:
  postgres_data:
```

### 4.4 启动应用

```bash
# 加载环境变量
export $(cat .env.prod | xargs)

# 构建并启动
docker-compose -f docker-compose.prod.yml up -d --build

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f

# 等待应用启动（约30秒）
```

### 4.5 验证部署

```bash
# 检查容器状态
docker ps

# 测试应用
curl http://localhost:8080/api/health

# 应该返回：
# {"status":"UP","application":"DoorDash Simulator",...}
```

### 4.6 外部访问测试

在本地浏览器访问：
```
http://54.123.45.67:8080/api/health
# 替换为你的 EC2 公网 IP
```

---

## 5️⃣ 配置 Nginx 反向代理

### 5.1 安装 Nginx
```bash
sudo apt install nginx -y

# 启动 Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 5.2 配置反向代理

```bash
# 创建配置文件
sudo nano /etc/nginx/sites-available/doordash
```

添加以下内容：
```nginx
server {
    listen 80;
    server_name 54.123.45.67;  # 替换为你的 IP 或域名

    # 日志
    access_log /var/log/nginx/doordash_access.log;
    error_log /var/log/nginx/doordash_error.log;

    # 反向代理到 Docker 应用
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        
        # 代理头设置
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 健康检查端点
    location /health {
        proxy_pass http://localhost:8080/api/health;
        proxy_set_header Host $host;
    }

    # 根路径重定向
    location / {
        return 301 /api/health;
    }
}
```

### 5.3 启用配置并重启

```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/doordash /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

### 5.4 访问测试

在浏览器访问：
```
http://54.123.45.67/api/health
http://54.123.45.67/health
```

---

## 6️⃣ 配置域名和 HTTPS（可选但推荐）

### 6.1 配置域名

如果你有域名（例如：api.doordash-demo.com）：

1. 在域名注册商添加 A 记录：
   ```
   类型: A
   名称: api  (或 @)
   值: 54.123.45.67  (你的 EC2 IP)
   TTL: 300
   ```

2. 等待 DNS 传播（5-30分钟）

### 6.2 安装免费 SSL 证书（Let's Encrypt）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书（自动配置 Nginx）
sudo certbot --nginx -d api.doordash-demo.com

# 按提示输入邮箱，同意条款

# 测试自动续期
sudo certbot renew --dry-run
```

### 6.3 验证 HTTPS

访问：
```
https://api.doordash-demo.com/api/health
```

---

## 7️⃣ 监控和维护

### 7.1 查看应用日志

```bash
# 查看所有容器日志
docker-compose -f docker-compose.prod.yml logs -f

# 只看应用日志
docker logs -f doordash-app

# 最后 100 行
docker logs --tail 100 doordash-app
```

### 7.2 重启应用

```bash
# 重启所有服务
docker-compose -f docker-compose.prod.yml restart

# 只重启应用
docker-compose -f docker-compose.prod.yml restart doordash-app
```

### 7.3 更新应用

```bash
# 拉取最新代码
cd ~/DoorDash-Delievery-Simulator
git pull

# 重新构建并启动
docker-compose -f docker-compose.prod.yml up -d --build
```

### 7.4 备份数据库

```bash
# 创建备份脚本
nano ~/backup-db.sh
```

添加内容：
```bash
#!/bin/bash
BACKUP_DIR=~/backups
mkdir -p $BACKUP_DIR

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/doordash_db_$DATE.sql"

docker exec doordash-postgres pg_dump -U postgres doordash_db > $BACKUP_FILE

echo "Backup completed: $BACKUP_FILE"

# 只保留最近 7 天的备份
find $BACKUP_DIR -name "doordash_db_*.sql" -mtime +7 -delete
```

```bash
# 添加执行权限
chmod +x ~/backup-db.sh

# 测试备份
./backup-db.sh

# 设置定时任务（每天凌晨 2 点备份）
crontab -e
# 添加：
0 2 * * * /home/ubuntu/backup-db.sh
```

### 7.5 监控系统资源

```bash
# 查看 CPU、内存使用
htop

# 查看磁盘使用
df -h

# 查看 Docker 容器资源
docker stats
```

### 7.6 设置自动重启

```bash
# 确保 Docker 服务开机自启
sudo systemctl enable docker

# 容器配置已包含 restart: unless-stopped
# 服务器重启后会自动启动容器
```

---

## 8️⃣ 安全加固（推荐）

### 8.1 更改 SSH 端口

```bash
# 编辑 SSH 配置
sudo nano /etc/ssh/sshd_config

# 修改端口（例如改为 2222）
Port 2222

# 重启 SSH
sudo systemctl restart sshd
```

⚠️ **记得在 EC2 安全组添加新端口！**

### 8.2 禁用密码登录（只用密钥）

```bash
# 编辑 SSH 配置
sudo nano /etc/ssh/sshd_config

# 设置
PasswordAuthentication no

# 重启 SSH
sudo systemctl restart sshd
```

### 8.3 安装 Fail2Ban（防暴力破解）

```bash
sudo apt install fail2ban -y
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

---

## 📊 成本估算

### 月度费用（美元）

| 服务 | 配置 | 费用 |
|------|------|------|
| EC2 (t3.medium) | 2vCPU, 4GB | ~$30 |
| EBS 存储 | 30GB gp3 | ~$3 |
| 数据传输 | 前 1GB 免费 | ~$0-5 |
| **总计** | | **~$33-38/月** |

**免费套餐**（新账户 12 个月）：
- t2.micro 实例：750 小时/月
- 30GB EBS
- 15GB 数据传输

---

## 🔧 故障排查

### 问题 1：无法连接到 EC2
```bash
# 检查安全组规则
# 确保 SSH (22) 端口对你的 IP 开放

# 检查密钥权限
chmod 400 doordash-key.pem
```

### 问题 2：应用无法启动
```bash
# 查看日志
docker logs doordash-app

# 检查内存
free -h
# 如果内存不足，升级实例类型

# 检查端口占用
sudo lsof -i :8080
```

### 问题 3：数据库连接失败
```bash
# 检查容器网络
docker network ls
docker network inspect doordash_default

# 检查环境变量
docker exec doordash-app env | grep SPRING
```

---

## ✅ 部署检查清单

- [ ] EC2 实例已启动
- [ ] 安全组规则配置正确
- [ ] SSH 可以连接
- [ ] Docker 和 Docker Compose 已安装
- [ ] 应用代码已上传
- [ ] 环境变量已配置
- [ ] 容器成功启动
- [ ] 应用健康检查通过
- [ ] Nginx 反向代理配置
- [ ] 防火墙规则设置
- [ ] SSL 证书配置（如果有域名）
- [ ] 数据库备份脚本设置
- [ ] 监控和日志配置

---

## 🎉 恭喜！

你的 DoorDash Simulator 现在已经部署到 AWS EC2 上了！

**访问地址：**
- HTTP: `http://your-ec2-ip/api/health`
- HTTPS: `https://your-domain.com/api/health`（如果配置了域名）

**下一步：**
1. 开发前端页面
2. 添加更多 API 端点
3. 配置 CI/CD 自动部署
4. 设置监控告警（CloudWatch）
5. 配置负载均衡（ELB）

🚀 **你的应用现在在云端运行了！**
