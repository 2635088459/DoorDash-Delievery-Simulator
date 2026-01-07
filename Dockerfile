# ============================================
# 多阶段构建 Dockerfile for Spring Boot
# ============================================

# ----------------------------------------
# 阶段 1: 构建阶段 (Build Stage)
# ----------------------------------------
# 使用Maven镜像来编译和打包Java应用
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 和源代码
# 分两步复制是为了利用Docker缓存：pom.xml不变时不重新下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 打包应用（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests

# ----------------------------------------
# 阶段 2: 运行阶段 (Runtime Stage)
# ----------------------------------------
# 使用JRE镜像来运行应用（兼容多平台）
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 从构建阶段复制打包好的JAR文件
# 根据你的pom.xml配置，JAR文件名格式为: artifactId-version.jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露应用端口（与application.yml中的server.port一致）
EXPOSE 8080

# 设置JVM参数（可根据需要调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 容器启动时执行的命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ============================================
# 构建说明:
# docker build -t doordash-simulator .
#
# 运行说明:
# docker run -p 8080:8080 doordash-simulator
# ============================================
