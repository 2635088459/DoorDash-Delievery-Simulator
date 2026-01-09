# ============================================
# Multi-stage Dockerfile for Spring Boot
# ============================================

# ----------------------------------------
# Stage 1: Build Stage
# ----------------------------------------
# Use Maven image to compile and package the Java application
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
# Two-step copy to leverage Docker cache: avoid re-downloading dependencies when pom.xml doesn't change
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Package application (skip tests to speed up build)
RUN mvn clean package -DskipTests

# ----------------------------------------
# Stage 2: Runtime Stage
# ----------------------------------------
# Use JRE image to run the application (multi-platform compatible)
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy packaged JAR file from build stage
# Based on your pom.xml configuration, JAR filename format is: artifactId-version.jar
COPY --from=builder /app/target/*.jar app.jar

# Expose application port (matches server.port in application.yml)
EXPOSE 8080

# Set JVM parameters (adjust as needed)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Command to execute when container starts
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ============================================
# Build instructions:
# docker build -t doordash-simulator .
#
# Run instructions:
# docker run -p 8080:8080 doordash-simulator
# ============================================
