# Development Roadmap for DoorDash Simulator

## 📊 Current Status

✅ **Completed:**
- Project structure setup
- Docker configuration (PostgreSQL + Application + pgAdmin)
- Spring Boot basic configuration
- Health check API endpoints
- Swagger API documentation

---

## 🎯 Development Stages

### Stage 1: Database Design (Current Stage)

**Goal:** Design the complete database schema for the food delivery system

**Tasks:**
1. Design ER (Entity-Relationship) Diagram
2. Define all required tables and fields
3. Establish relationships between tables
4. Document the design

**Recommended Approach:**
- ✅ Design ER diagram first (using tools or paper)
- ✅ Define Entity classes in code
- ✅ Let Hibernate auto-generate database tables
- ✅ Verify tables in pgAdmin

**Why NOT manually create tables in pgAdmin:**
- ✅ JPA/Hibernate automates this process
- ✅ Code is the single source of truth

---

### Stage 2: Backend Development (Next Priority)

**Goal:** Build complete REST API for the application

**2.1 Entity Layer**
- Create JPA Entity classes (corresponding to database tables)
- Define fields, data types, and annotations
- Establish entity relationships (@OneToMany, @ManyToOne, etc.)
- Add validation constraints

**2.2 Repository Layer**
- Create Repository interfaces for each entity
- Extend JpaRepository
- Define custom query methods if needed

**2.3 DTO Layer**
- Create Request DTOs (for receiving frontend data)
- Create Response DTOs (for returning to frontend)
- Avoid directly exposing entity classes

**2.4 Service Layer**
- Define Service interfaces
- Implement business logic
- Handle data conversion (Entity ↔ DTO)
- Implement transaction management

**2.5 Controller Layer**
- Define RESTful API endpoints
- Use proper HTTP methods (GET, POST, PUT, DELETE)
- Call Service layer methods
- Handle request/response

**2.6 Testing**
- Test APIs with Swagger UI
- Write unit tests
- Write integration tests

---

### Stage 3: Frontend Development (Final Stage)

**Goal:** Build user interface to interact with backend APIs

**3.1 Choose Frontend Framework**
- React (Recommended - most popular)
- Vue.js (Easier for beginners)
- Angular (Enterprise-level)

**3.2 Setup Frontend Project**
- Initialize project with create-react-app / vue-cli
- Configure API base URL
- Setup routing

**3.3 Implement Features**
- User authentication pages
- Restaurant browsing
- Menu display
- Cart management
- Order placement
- Order tracking

**3.4 Integration**
- Connect to backend APIs
- Handle authentication (JWT tokens)
- Implement error handling
- Add loading states

---

### Road map Summary:

```
1. Design ER Diagram
   ↓
2. Create Entity Classes in Code
   ↓
3. Run Application (Hibernate auto-creates tables)
   ↓
4. Verify in pgAdmin
   ↓
5. Develop APIs
   ↓
6. Test with Swagger/Postman
   ↓
7. Build Frontend
```

---

## Required Database Tables

### Core Tables for DoorDash System:

1. **users** - User accounts
   - Customer, Restaurant Owner, Delivery Driver roles
   
2. **restaurants** - Restaurant information
   - Name, address, cuisine type, rating
   
3. **menu_items** - Food items
   - Name, price, description, category
   
4. **orders** - Order records
   - Customer, restaurant, total price, status
   
5. **order_items** - Order line items
   - Links orders to menu items with quantities
   
6. **addresses** - Delivery addresses
   - Customer addresses for delivery
   
7. **reviews** - Customer reviews
   - Rating and comments for restaurants/orders
   
8. **delivery_drivers** - Driver information
   - Driver profiles and availability

### Relationships:

- User (1) → (N) Orders
- User (1) → (N) Addresses
- User (1) → (N) Reviews
- Restaurant (1) → (N) Menu Items
- Restaurant (1) → (N) Orders
- Order (1) → (N) Order Items
- Menu Item (1) → (N) Order Items
- Delivery Driver (1) → (N) Orders

---

## 💡 Next Immediate Steps

### Option A: Design ER Diagram First
Create complete database design document with:
- All tables and fields
- Data types and constraints
- Relationships and foreign keys
- Save as documentation in `docs/`

### Option B: Start Creating Entity Classes
Begin with core entities:
1. User entity
2. Restaurant entity
3. MenuItem entity
4. See Hibernate auto-generate tables

### Option C: Both (Recommended)
1. Design ER diagram as documentation
2. Implement Entity classes based on design
3. Verify auto-generated tables in pgAdmin

---

## 🎓 Learning Benefits

**By following this approach, you'll learn:**
- JPA/Hibernate ORM concepts
- Database relationship mapping
- RESTful API design
- Service layer architecture
- DTO pattern
- Testing strategies
- Frontend-backend integration

---

##  Notes

- Current configuration has `ddl-auto: update` which auto-creates/updates tables
- Use pgAdmin to **view** and **verify** tables, not to create them
- Keep database password secure (use environment variables in production)
- Frontend development should start after backend APIs are stable

---
