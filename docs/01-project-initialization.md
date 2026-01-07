# 项目初始化配置

## 1. 创建 pom.xml (Maven配置文件)

**位置**：项目根目录

**必需依赖**：
- Spring Boot Starter Parent (3.2.1)
- Spring Boot Web
- Spring Data JPA
- Spring Security
- Spring Validation
- MySQL Driver (或 PostgreSQL)
- Lombok
- Swagger/OpenAPI (springdoc-openapi-starter-webmvc-ui 2.3.0)
- JWT (jjwt-api, jjwt-impl, jjwt-jackson 0.12.3)
- ModelMapper (3.2.0)
- Spring Boot DevTools
- Spring Boot Test
- Spring Security Test

**配置要点**：
- Java版本：17
- GroupId: com.yourname
- ArtifactId: doordash-clone
- Version: 1.0.0

## 2. 或者创建 build.gradle (Gradle配置文件)

**位置**：项目根目录

**配置要点**：
- Spring Boot Plugin: 3.2.1
- Spring Dependency Management: 1.1.4
- Java 17
- 包含所有上述依赖的Gradle版本

## 3. 创建 application.yml 配置文件

**位置**：`src/main/resources/application.yml`

**主要配置项**：

### 数据库配置
- URL: `jdbc:mysql://localhost:3306/doordash_db`
- Username: root
- Password: 设置你的密码
- Driver: com.mysql.cj.jdbc.Driver

### JPA/Hibernate配置
- ddl-auto: update (开发阶段) / validate (生产环境)
- show-sql: true
- dialect: MySQL8Dialect
- format_sql: true
- open-in-view: false

### 服务器配置
- Port: 8080
- Context-path: /api

### JWT配置
- Secret: 至少256位的密钥
- Expiration: 86400000 (24小时)

### Swagger配置
- API Docs路径: /api-docs
- Swagger UI路径: /swagger-ui.html

### 日志配置
- Root级别: INFO
- 应用级别: DEBUG
- SQL日志: DEBUG

### 文件上传
- 最大文件大小: 10MB
- 最大请求大小: 10MB

## 4. 或者创建 application.properties 配置文件

**位置**：`src/main/resources/application.properties`

包含与application.yml相同的配置，使用properties格式

## 5. 创建主应用类

**位置**：`src/main/java/com/yourname/doordashclone/DoorDashCloneApplication.java`

**要点**：
- 使用 `@SpringBootApplication` 注解
- 包含main方法
- 调用 `SpringApplication.run()`

## 6. 创建数据库

**数据库名称**：doordash_db  
**字符集**：utf8mb4  
**排序规则**：utf8mb4_unicode_ci

## 7. 项目结构确认

**必需的目录结构**：
- `src/main/java/com/yourname/doordashclone/` - 主代码目录
  - `config/` - Spring配置、CORS、Swagger
  - `controller/` - REST控制器
  - `dto/` - 数据传输对象
  - `entity/` - JPA实体类
  - `exception/` - 自定义异常
  - `repository/` - JPA仓库接口
  - `security/` - 安全配置、过滤器
  - `service/` - 业务逻辑接口
  - `service/impl/` - 业务逻辑实现
  - `util/` - 工具类
  - `DoorDashCloneApplication.java` - 主启动类
- `src/main/resources/` - 资源文件
  - `application.yml` 或 `application.properties`
  - `static/` - 静态资源
- `src/test/java/` - 测试代码
- `pom.xml` 或 `build.gradle` - 构建配置
- `docs/` - 文档目录

## 8. 构建和运行

**Maven命令**：
- 构建：`mvn clean install`
- 运行：`mvn spring-boot:run`

**Gradle命令**：
- 构建：`gradle build`
- 运行：`gradle bootRun`

**访问地址**：
- 应用：http://localhost:8080/api
- Swagger UI：http://localhost:8080/api/swagger-ui.html

## 9. 下一步

1. 设计数据库ER图
2. 创建实体类（Entity）
3. 创建Repository接口
4. 创建DTO类
5. 实现Service层
6. 实现Controller层

## 10. 重要注意事项

⚠️ **安全配置**：
- 修改MySQL密码为实际密码
- JWT secret必须是强密码（至少256位）
- 生产环境敏感信息使用环境变量

⚠️ **数据库配置**：
- 开发环境：`ddl-auto=update`
- 生产环境：`ddl-auto=validate` 或 `none`

⚠️ **版本控制**：
- 不要提交 `application.yml` 中的密码
- 使用 `.gitignore` 忽略敏感配置
- 可以创建 `application-example.yml` 作为模板
