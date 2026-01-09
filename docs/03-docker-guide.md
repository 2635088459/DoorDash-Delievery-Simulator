# Docker Usage Guide
---


### 1. Start All Services 

```bash
# Run in the project root directory
docker-compose up -d
```

**Notes**:
- `up`: Start services
- `-d`: Run in detached mode

**First run will**:
1. Download PostgreSQL image
2. Download Java image
3. Build your application using Maven
4. Create and start containers

### 2. Check Service Status

```bash
docker-compose ps
```

You should see three running services:
- `doordash-postgres` - Database
- `doordash-app` - Application
- `doordash-pgadmin` - Database Management Tool

### 3. Access the Application

- **Application API**: http://localhost:8080/api
- **Swagger Documentation**: http://localhost:8080/api/swagger-ui.html
- **pgAdmin**: http://localhost:5050
  - Email: `admin@doordash.com`
  - Password: `admin123`

---

## 📊 Log Viewing

### View Logs of All Services

```bash
docker-compose logs -f
```

### View Logs of Application Only

```bash
docker-compose logs -f app
```

### View Logs of Database Only

```bash
docker-compose logs -f postgres
```

**Notes**:
- `-f`: Follow logs in real-time
- Press `Ctrl+C` to exit log viewing

---

## 🔧 Common Operations

### Stop All Services

```bash
docker-compose down
```

**Data Retention**: Database data will not be deleted

### Stop and Remove All Data

```bash
docker-compose down -v
```

**⚠️ Warning**: This will delete all data in the database!

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart application only
docker-compose restart app
```

### Rebuild Application
After modifying the code, you need to rebuild:

```bash
docker-compose up -d --build
```

Or:

```bash
docker-compose build
docker-compose up -d
```

---

## 🐛 Debugging Tips

### Enter Container

```bash
# Enter application container
docker exec -it doordash-app sh

# Enter database container
docker exec -it doordash-postgres bash
```

### Execute SQL in Database Container
```bash
docker exec -it doordash-postgres psql -U postgres -d doordash_db
```

Common SQL commands:
```sql
\l                    -- List all databases
\dt                   -- List all tables
\d table_name         -- Describe table structure
SELECT * FROM users;  -- Query data
\q                    -- Quit
```

### View Container Resource Usage
```bash
docker stats
```


## Data Management

### Data Persistence

Data is stored in Docker Volumes:
```bash
# List all volumes
docker volume ls

# Inspect volume details
docker volume inspect doordash_postgres_data
```

### Backup Database
```bash
docker exec doordash-postgres pg_dump -U postgres doordash_db > backup.sql
```

### Restore Database

```bash
docker exec -i doordash-postgres psql -U postgres doordash_db < backup.sql
```

---

## Environment Configuration

### Change Database Password

Edit `docker-compose.yml`:
```yaml
environment:
  POSTGRES_PASSWORD: your_new_password
```

Also update the application's environment variables:

```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: your_new_password
```

### Change Ports

Edit `docker-compose.yml`:

```yaml
ports:
  - "9090:8080"  # Host port 9090 mapped to container port 8080
```

---

## 🧹 Resource Cleanup

### Remove All Stopped Containers

```bash
docker container prune
```

### Remove Unused Images

```bash
docker image prune -a
```

### Remove Unused Volumes

```bash
docker volume prune
```

### Complete Cleanup (Use with Caution)

```bash
docker system prune -a --volumes
```

---

## ❌ Common Issues

### Issue 1: Port Already in Use

```
Error: Bind for 0.0.0.0:5432 failed: port is already allocated
```

**Solutions**：
- Stop the local PostgreSQL service
- Or change the port mapping in docker-compose.yml

### Issue 2: Build Failure
```
Error: Could not find or load main class
```

**Solutions**:
```bash
# Clean up and rebuild
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

## Advanced Usage

### Using Environment Files

Create a `.env` file:
```env
POSTGRES_PASSWORD=secret123
JWT_SECRET=my-secret-key
```

在 `docker-compose.yml` 中引用：

```yaml
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

### Add New Service (e.g., Redis)

Edit `docker-compose.yml`:

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

## References
- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/topicals/spring-boot-docker/)
- [PostgreSQL Docker 镜像](https://hub.docker.com/_/postgres)
