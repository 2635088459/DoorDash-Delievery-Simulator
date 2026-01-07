# Spring Boot 启动类和基础配置说明

## 已创建的文件

### 1. 主启动类
**文件**: `src/main/java/com/shydelivery/doordashsimulator/DoorDashSimulatorApplication.java`

**作用**: 
- Spring Boot 应用的入口点
- 启动嵌入式服务器（Tomcat）
- 初始化 Spring 容器

**包名说明**:
- 根据你的 pom.xml: `com.shy-delievery`
- 我使用了: `com.shydelivery.doordashsimulator`
- 如果需要修改，确保与 pom.xml 的 `groupId` 一致

---

### 2. 健康检查控制器
**文件**: `src/main/java/com/shydelivery/doordashsimulator/controller/HealthController.java`

**作用**:
- 测试应用是否正常运行
- 提供简单的 REST API

**测试端点**:
- `GET /api/health` - 健康检查
- `GET /api/health/welcome` - 欢迎页面

---

### 3. CORS 跨域配置
**文件**: `src/main/java/com/shydelivery/doordashsimulator/config/CorsConfig.java`

**作用**:
- 允许前端跨域访问 API
- 配置允许的域名、方法、请求头

---

### 4. Swagger API 文档配置
**文件**: `src/main/java/com/shydelivery/doordashsimulator/config/SwaggerConfig.java`

**作用**:
- 自动生成 API 文档
- 提供交互式 API 测试界面

**访问地址**:
- http://localhost:8080/api/swagger-ui.html

---

### 5. Spring Security 配置
**文件**: `src/main/java/com/shydelivery/doordashsimulator/config/SecurityConfig.java`

**作用**:
- 暂时禁用认证（便于开发测试）
- 后续可以添加 JWT 认证

---

## 🚀 现在可以启动应用了！

### 方法1: 使用 Docker（推荐）

```bash
# 在项目根目录
docker-compose up -d

# 查看日志
docker-compose logs -f app
```

### 方法2: 本地运行（需要先安装 PostgreSQL）

```bash
# 使用 Maven
mvn spring-boot:run

# 或者先打包再运行
mvn clean package
java -jar target/doordash-simulator-1.0.0.jar
```

---

## 📍 测试应用

### 1. 健康检查

```bash
curl http://localhost:8080/api/health
```

应该返回：
```json
{
  "status": "UP",
  "application": "DoorDash Simulator",
  "timestamp": "2026-01-06T...",
  "message": "Application is running successfully!"
}
```

### 2. 访问 Swagger UI

浏览器打开: http://localhost:8080/api/swagger-ui.html

---

## 📂 当前项目结构

```
DoorDash/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── shydelivery/
│       │           └── doordashsimulator/
│       │               ├── DoorDashSimulatorApplication.java  ← 启动类
│       │               ├── config/
│       │               │   ├── CorsConfig.java
│       │               │   ├── SecurityConfig.java
│       │               │   └── SwaggerConfig.java
│       │               └── controller/
│       │                   └── HealthController.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── pom.xml
└── docs/
    ├── 01-project-initialization.md
    ├── 02-postgresql-config.md
    └── 03-docker-guide.md
```

---

## ⚠️ 可能需要修改的地方

### 包名不一致

如果启动报错，检查包名：

1. **pom.xml** 中的 `groupId`:
   ```xml
   <groupId>com.shy-delievery</groupId>
   ```

2. **Java 文件**的包名应该匹配:
   ```java
   package com.shydelivery.doordashsimulator;
   ```

3. **application.yml** 中的日志配置:
   ```yaml
   logging:
     level:
       com.shydelivery: DEBUG
   ```

---

## 🎯 下一步

现在你的项目已经可以启动了！接下来可以：

1. ✅ 测试应用启动
2. ✅ 访问 Swagger 查看 API 文档
3. ⏭️ 开始创建实体类（Entity）
4. ⏭️ 创建 Repository
5. ⏭️ 实现业务逻辑

准备好开始开发实际功能了吗？
