# PostgreSQL Setting Guide for DoorDash Clone Project

## Why Choose PostgreSQL

✅ **More Powerful Data Types** - JSON, arrays, UUID, etc.  
✅ **Better Concurrency Control** - Mature MVCC mechanism  
✅ **Geospatial Features** - PostGIS extension (useful for delivery projects)  
✅ **Stricter SQL Standards** - Learn more standardized SQL  
✅ **Enterprise Features** - Suitable for future expansion  

---

## application.yml Configuration
**Location**: `src/main/resources/application.yml`

### PostgreSQL Data Source Configuration

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

## Create Database

### Method 1: Using psql Command Line
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE doordash_db;

-- Create user (optional)
CREATE USER doordash_user WITH PASSWORD 'your_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE doordash_db TO doordash_user;
```

### Method 2: Using pgAdmin
1. Open pgAdmin (PostgreSQL graphical tool)
2. Right-click Databases → Create → Database
3. Enter database name: `doordash_db`
4. Save
---

## Install PostgreSQL

### macOS (Using Homebrew)

```bash
# Install PostgreSQL
brew install postgresql@16

# Start PostgreSQL service
brew services start postgresql@16

# Verify installation
psql --version
```

### Using Docker (Recommended)
```bash
# Pull PostgreSQL image
docker pull postgres:16

# Run PostgreSQL container
docker run --name postgres-doordash \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=doordash_db \
  -p 5432:5432 \
  -d postgres:16
```



## PostgreSQL Advanced Data Types Examples

### 1. **JSON/JSONB Type**
save menu details and order info

```java
@Column(columnDefinition = "jsonb")
private String menuDetails;
```

### 2. **Array Type**
store tags, image URLs, etc.

```java
@Column(columnDefinition = "text[]")
private String[] tags;
```

### 3. **PostGIS Extension**
calculate restaurant distance, delivery range

```sql
-- Enable PostGIS
CREATE EXTENSION postgis;

-- Calculate distance between two points
SELECT ST_Distance(
  ST_MakePoint(lon1, lat1)::geography,
  ST_MakePoint(lon2, lat2)::geography
);
```


## Common PostgreSQL Commands

```sql
-- List all databases
\l

-- Connect to a database
\c doordash_db

-- List all tables
\dt

-- Describe table structure
\d table_name

-- Exit
\q
```

---


---

## Next Steps
1. ✅ pom.xml - PostgreSQL driver enabled
2. ⏭️ Create application.yml
3. ⏭️ Create Dockerfile and docker-compose.yml
4. ⏭️ Start PostgreSQL
5. ⏭️ Begin writing entity classes

---

