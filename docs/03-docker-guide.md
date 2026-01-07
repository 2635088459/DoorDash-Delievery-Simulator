# Docker 使用指南

## 📋 前置要求

确保你已经安装：
- ✅ Docker Desktop (包含 Docker 和 Docker Compose)
- ✅ 版本确认：
  ```bash
  docker --version
  docker-compose --version
  ```

---

## 🚀 快速启动

### 1. 启动所有服务

```bash
# 在项目根目录执行
docker-compose up -d
```

**说明**：
- `up`: 启动服务
- `-d`: 后台运行（detached mode）

**第一次运行会**：
1. 下载 PostgreSQL 镜像
2. 下载 Java 镜像
3. 使用 Maven 构建你的应用
4. 创建并启动容器

⏱️ 首次启动约需 3-5 分钟（下载依赖）

### 2. 查看服务状态

```bash
docker-compose ps
```

应该看到三个服务都在运行：
- `doordash-postgres` - 数据库
- `doordash-app` - 应用
- `doordash-pgadmin` - 数据库管理工具

### 3. 访问应用

- **应用 API**: http://localhost:8080/api
- **Swagger 文档**: http://localhost:8080/api/swagger-ui.html
- **pgAdmin**: http://localhost:5050
  - Email: `admin@doordash.com`
  - Password: `admin123`

---

## 📊 日志查看

### 查看所有服务日志

```bash
docker-compose logs -f
```

### 只查看应用日志

```bash
docker-compose logs -f app
```

### 只查看数据库日志

```bash
docker-compose logs -f postgres
```

**说明**：
- `-f`: 跟踪日志（follow），实时显示
- 按 `Ctrl+C` 退出日志查看

---

## 🔧 常用操作

### 停止所有服务

```bash
docker-compose down
```

**保留数据**：数据库数据不会被删除

### 停止并删除所有数据

```bash
docker-compose down -v
```

**⚠️ 警告**：会删除数据库中的所有数据！

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 只重启应用
docker-compose restart app
```

### 重新构建应用

代码修改后需要重新构建：

```bash
docker-compose up -d --build
```

或者：

```bash
docker-compose build
docker-compose up -d
```

---

## 🐛 调试技巧

### 进入容器内部

```bash
# 进入应用容器
docker exec -it doordash-app sh

# 进入数据库容器
docker exec -it doordash-postgres bash
```

### 在数据库容器中执行 SQL

```bash
docker exec -it doordash-postgres psql -U postgres -d doordash_db
```

常用 SQL 命令：
```sql
\l                    -- 列出所有数据库
\dt                   -- 列出所有表
\d table_name         -- 查看表结构
SELECT * FROM users;  -- 查询数据
\q                    -- 退出
```

### 查看容器资源使用

```bash
docker stats
```

---

## 🔄 开发工作流

### 典型的开发流程

```bash
# 1. 修改代码
# 编辑 Java 文件

# 2. 重新构建并启动
docker-compose up -d --build

# 3. 查看日志确认启动成功
docker-compose logs -f app

# 4. 测试 API
# 访问 http://localhost:8080/api/swagger-ui.html
```

### 只修改代码（不改依赖）

如果只是修改业务逻辑，没有改 `pom.xml`：

```bash
# 方法1: 使用 docker-compose
docker-compose up -d --build

# 方法2: 手动构建
mvn clean package -DskipTests
docker-compose up -d --build
```

---

## 🗄️ 数据管理

### 数据持久化

数据保存在 Docker Volume 中：
```bash
# 查看所有 volumes
docker volume ls

# 查看 volume 详情
docker volume inspect doordash_postgres_data
```

### 备份数据库

```bash
docker exec doordash-postgres pg_dump -U postgres doordash_db > backup.sql
```

### 恢复数据库

```bash
docker exec -i doordash-postgres psql -U postgres doordash_db < backup.sql
```

---

## 🌐 环境配置

### 修改数据库密码

编辑 `docker-compose.yml`：

```yaml
environment:
  POSTGRES_PASSWORD: your_new_password
```

同时修改应用的环境变量：

```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: your_new_password
```

### 修改端口

编辑 `docker-compose.yml`：

```yaml
ports:
  - "9090:8080"  # 宿主机9090端口映射到容器8080端口
```

---

## 🧹 清理资源

### 删除所有停止的容器

```bash
docker container prune
```

### 删除未使用的镜像

```bash
docker image prune -a
```

### 删除未使用的 volumes

```bash
docker volume prune
```

### 完全清理（谨慎使用）

```bash
docker system prune -a --volumes
```

---

## ❌ 常见问题

### 问题1: 端口已被占用

```
Error: Bind for 0.0.0.0:5432 failed: port is already allocated
```

**解决方案**：
- 停止本地的 PostgreSQL 服务
- 或修改 docker-compose.yml 中的端口映射

### 问题2: 构建失败

```
Error: Could not find or load main class
```

**解决方案**：
```bash
# 清理并重新构建
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 问题3: 应用无法连接数据库

**解决方案**：
- 确保 `depends_on` 配置正确
- 检查环境变量中的数据库连接信息
- 查看日志：`docker-compose logs postgres`

### 问题4: 数据库数据丢失

**原因**：使用了 `docker-compose down -v`

**预防**：
- 定期备份数据
- 不要使用 `-v` 参数

---

## 📚 进阶使用

### 使用环境文件

创建 `.env` 文件：

```env
POSTGRES_PASSWORD=secret123
JWT_SECRET=my-secret-key
```

在 `docker-compose.yml` 中引用：

```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

### 添加新服务（如 Redis）

编辑 `docker-compose.yml`：

```yaml
redis:
  image: redis:7-alpine
  container_name: doordash-redis
  ports:
    - "6379:6379"
  networks:
    - doordash-network
```

---

## 🎯 生产环境注意事项

⚠️ **不要在生产环境直接使用此配置！**

生产环境需要：
1. 使用 Docker Secrets 管理敏感信息
2. 配置资源限制（CPU、内存）
3. 使用专业的数据库服务（如 AWS RDS）
4. 配置日志聚合和监控
5. 使用负载均衡和多副本部署

---

## 📖 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker 镜像](https://hub.docker.com/_/postgres)
