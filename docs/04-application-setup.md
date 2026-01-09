# Spring Boot Main Application Class and Configuration Guide

## Created Files

### 1. Main Application Class
**File**: `src/main/java/com/shydelivery/doordashsimulator/DoorDashSimulatorApplication.java`

**Purpose**: 
- Entry point for Spring Boot application
- Starts embedded server (Tomcat)
- Initializes Spring container

**Package Name Notes**:
- According to your pom.xml: `com.shy-delievery`
- We are using: `com.shydelivery.doordashsimulator`


---

### 2. Health Check Controller
**File**: `src/main/java/com/shydelivery/doordashsimulator/controller/HealthController.java`

**Purpose**:
- Test if the application is running properly
- Provide simple REST API

**Test Endpoints**:
- `GET /api/health` - Health check
- `GET /api/health/welcome` - Welcome page

---

### 3. CORS Configuration
**File**: `src/main/java/com/shydelivery/doordashsimulator/config/CorsConfig.java`

**Purpose**:
- Allow frontend to access API from different origins
- Configure allowed domains, methods, and headers

---

### 4. Swagger API Documentation Configuration
**File**: `src/main/java/com/shydelivery/doordashsimulator/config/SwaggerConfig.java`

**Purpose**:
- Auto-generate API documentation
- Provide interactive API testing interface

**Access URL**:
- http://localhost:8080/api/swagger-ui.html

---

### 5. Spring Security Configuration
**File**: `src/main/java/com/shydelivery/doordashsimulator/config/SecurityConfig.java`

**Purpose**:
- Temporarily disable authentication (for development and testing)
- JWT authentication can be added later

---



### Method 1: Using Docker (Recommended)

```bash
# In the project root directory
docker-compose up -d

# View logs
docker-compose logs -f app
```

### Method 2: Run Locally (Requires PostgreSQL installation first)

```bash
# Using Maven
mvn spring-boot:run

# Or package and run
mvn clean package
java -jar target/doordash-simulator-1.0.0.jar
```

---

## 📍 Test the Application

### 1. Health Check

```bash
curl http://localhost:8080/api/health
```

Should return:
```json
{
  "status": "UP",
  "application": "DoorDash Simulator",
  "timestamp": "2026-01-06T...",
  "message": "Application is running successfully!"
}
```

### 2. Access Swagger UI

Open in browser: http://localhost:8080/api/swagger-ui.html

---

## 📂 Current Project Structure

```
DoorDash/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── shydelivery/
│       │           └── doordashsimulator/
│       │               ├── DoorDashSimulatorApplication.java  ← Main Class
│       │               ├── config/
│       │               │   ├── CorsConfig.java
│       │               │   ├── SecurityConfig.java
│       │               │   └── SwaggerConfig.java
│       │               ├── controller/
│       │               │   └── HealthController.java
│       │               └── entity/
│       │                   └── User.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── pom.xml
└── docs/
    ├── 01-project-initialization.md
    ├── 02-postgresql-config.md
    ├── 03-docker-guide.md
    ├── 04-application-setup.md
    ├── 05-development-roadmap.md
    └── 06-database-schema.md
```

---


### Package Name Inconsistency

If startup fails, check the package names:

1. **pom.xml** `groupId`:
   ```xml
   <groupId>com.shy-delievery</groupId>
   ```

2. **Java files** package names should match:
   ```java
   package com.shydelivery.doordashsimulator;
   ```

3. **application.yml** logging configuration:
   ```yaml
   logging:
     level:
       com.shydelivery: DEBUG
   ```

---


