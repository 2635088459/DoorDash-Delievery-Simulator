# PostgreSQL 配置说明

## 为什么选择 PostgreSQL

✅ **更强大的数据类型** - JSON、数组、UUID等  
✅ **更好的并发控制** - MVCC机制成熟  
✅ **地理位置功能** - PostGIS扩展（外卖项目可能用到）  
✅ **更严格的SQL标准** - 学习更规范的SQL  
✅ **企业级特性** - 适合未来扩展  

---

## application.yml 配置

**位置**: `src/main/resources/application.yml`

### PostgreSQL 数据源配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/doordash_db
    username: postgres
    password: your_password
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false
```

### 与 MySQL 的主要区别

| 配置项 | PostgreSQL | MySQL |
|--------|------------|-------|
| URL | `jdbc:postgresql://localhost:5432/` | `jdbc:mysql://localhost:3306/` |
| Driver | `org.postgresql.Driver` | `com.mysql.cj.jdbc.Driver` |
| Dialect | `PostgreSQLDialect` | `MySQL8Dialect` |
| 默认端口 | 5432 | 3306 |
| 默认用户 | postgres | root |

---

## 创建数据库

### 方法1: 使用 psql 命令行

```sql
-- 连接到PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE doordash_db;

-- 创建用户（可选）
CREATE USER doordash_user WITH PASSWORD 'your_password';

-- 授权
GRANT ALL PRIVILEGES ON DATABASE doordash_db TO doordash_user;
```

### 方法2: 使用 pgAdmin

1. 打开 pgAdmin（PostgreSQL图形化工具）
2. 右键 Databases → Create → Database
3. 输入数据库名: `doordash_db`
4. 保存

---

## 安装 PostgreSQL

### macOS (使用 Homebrew)

```bash
# 安装PostgreSQL
brew install postgresql@16

# 启动PostgreSQL服务
brew services start postgresql@16

# 验证安装
psql --version
```

### 使用 Docker (推荐)

```bash
# 拉取PostgreSQL镜像
docker pull postgres:16

# 运行PostgreSQL容器
docker run --name postgres-doordash \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=doordash_db \
  -p 5432:5432 \
  -d postgres:16
```

### 使用 docker-compose (最推荐)

在 `docker-compose.yml` 中配置（后续会创建）

---

## PostgreSQL 特有功能（外卖项目可用）

### 1. **JSON/JSONB 类型**
存储菜单、订单详情等灵活数据

```java
@Column(columnDefinition = "jsonb")
private String menuDetails;
```

### 2. **数组类型**
存储标签、图片URL等

```java
@Column(columnDefinition = "text[]")
private String[] tags;
```

### 3. **PostGIS 扩展**
计算餐厅距离、配送范围

```sql
-- 启用PostGIS
CREATE EXTENSION postgis;

-- 计算两点距离
SELECT ST_Distance(
  ST_MakePoint(lon1, lat1)::geography,
  ST_MakePoint(lon2, lat2)::geography
);
```

### 4. **全文搜索**
搜索餐厅、菜品

```sql
-- 创建全文搜索索引
CREATE INDEX idx_restaurant_search 
ON restaurants 
USING GIN(to_tsvector('english', name || ' ' || description));
```

---

## 常用 PostgreSQL 命令

```sql
-- 查看所有数据库
\l

-- 连接到数据库
\c doordash_db

-- 查看所有表
\dt

-- 查看表结构
\d table_name

-- 退出
\q
```

---

## pom.xml 已配置

✅ PostgreSQL驱动已启用  
✅ MySQL驱动已注释（如需要可随时切换）

---

## 下一步

1. ✅ pom.xml - PostgreSQL驱动已配置
2. ⏭️ 创建 application.yml
3. ⏭️ 创建 Dockerfile 和 docker-compose.yml
4. ⏭️ 启动 PostgreSQL
5. ⏭️ 开始编写实体类

---

## 注意事项

⚠️ **端口冲突**: 确保5432端口未被占用  
⚠️ **密码安全**: 不要在代码中硬编码密码  
⚠️ **字符集**: PostgreSQL默认UTF-8，无需特别配置  
⚠️ **大小写敏感**: PostgreSQL对标识符大小写敏感（建议用小写）
