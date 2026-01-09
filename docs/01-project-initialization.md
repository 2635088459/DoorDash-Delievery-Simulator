# Project Initialization Configuration

## 1. Create pom.xml (Maven Configuration File)

**Location**: Project root directory

**Dependencies**:
- Spring Boot Starter Parent (3.2.1)
- Spring Boot Web
- Spring Data JPA
- Spring Security
- Spring Validation
- PostgreSQL Driver (or MySQL)
- Lombok
- Swagger/OpenAPI (springdoc-openapi-starter-webmvc-ui 2.3.0)
- JWT (jjwt-api, jjwt-impl, jjwt-jackson 0.12.3)
- ModelMapper (3.2.0)
- Spring Boot DevTools
- Spring Boot Test
- Spring Security Test


## 2. Create application.yml Configuration File

**Location**: `src/main/resources/application.yml`

**Main Configuration Items**:

### Database Configuration
- URL: `jdbc:postgresql://localhost:5432/doordash_db`
- Username: postgres
- Password: Set your password
- Driver: org.postgresql.Driver

### JPA/Hibernate Configuration
- ddl-auto: update (development) / validate (production)
- show-sql: true
- dialect: PostgreSQLDialect
- format_sql: true
- open-in-view: false

### Server Configuration
- Port: 8080
- Context-path: /api

### JWT Configuration
- Secret: At least 256-bit key
- Expiration: 86400000 (24 hours)

### Swagger Configuration
- API Docs path: /api-docs
- Swagger UI path: /swagger-ui.html

### Logging Configuration
- Root level: INFO
- Application level: DEBUG
- SQL logging: DEBUG

### File Upload
- Max file size: 10MB
- Max request size: 10MB

## 5. Create Main Application Class

**Location**: `src/main/java/com/yourname/doordashclone/DoorDashCloneApplication.java`

**Key Points**:
- Use `@SpringBootApplication` annotation
- Include main method
- Call `SpringApplication.run()`

## 6. Create Database

**Database Name**: doordash_db  
**Character Set**: utf8mb4  
**Collation**: utf8mb4_unicode_ci

## 7. Structure Confirmation

**Needed**:
- `src/main/java/com/yourname/doordashclone/` - Main code directory
  - `config/` - Spring configuration, CORS, Swagger
  - `controller/` - REST controllers
  - `dto/` - Data Transfer Objects
  - `entity/` - JPA entity classes
  - `exception/` - Custom exceptions
  - `repository/` - JPA repository interfaces
  - `security/` - Security configuration, filters
  - `service/` - Business logic interfaces
  - `service/impl/` - Business logic implementation
  - `util/` - Utility classes
  - `DoorDashCloneApplication.java` - Main startup class
- `src/main/resources/` - Resource files
  - `application.yml` or `application.properties`
  - `static/` - Static resources
- `src/test/java/` - Test code
- `pom.xml` or `build.gradle` - Build configuration
- `docs/` - Documentation directory

## 8. Build and Run

**Maven commands**:
- Build: `mvn clean install`
- Run: `mvn spring-boot:run`
**Gradle commands**:
- Build: `gradle build`
- Run: `gradle bootRun`

**Access URLs**:
- Application: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html

## 9. Next Steps
1. Design the database ER diagram
2. Create entity classes
3. Create repository interfaces
4. Create DTO classes
5. Implement the service layer
6. Implement the controller layer

