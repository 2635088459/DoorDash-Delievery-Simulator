# Database Schema Design - DoorDash Simulator
S

## Entity Relationship Diagram (Text Format)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│    User     │────────>│   Address    │         │ Restaurant  │
│             │ 1     N │              │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
      │                                                  │
      │ 1                                                │ 1
      │                                                  │
      │ N                                                │ N
      │                  ┌──────────────┐               │
      └─────────────────>│    Order     │<──────────────┘
                         │              │
                         └──────────────┘
                               │ 1
                               │
                               │ N
                         ┌──────────────┐
                         │  OrderItem   │
                         │              │
                         └──────────────┘
                               │ N
                               │
                               │ 1
                         ┌──────────────┐
                         │  MenuItem    │<────────┐
                         │              │         │
                         └──────────────┘         │
                               │                  │ N
                               │                  │
                               └──────────────────┘
                                      1 (belongs to Restaurant)

┌─────────────┐         ┌──────────────┐
│   Review    │────────>│    Order     │
│             │ N     1 │              │
└─────────────┘         └──────────────┘
      │ N
      │
      │ 1
┌─────────────┐
│    User     │
│             │
└─────────────┘

┌─────────────┐         ┌──────────────┐
│DeliveryInfo │────────>│    Order     │
│             │ 1     1 │              │
└─────────────┘         └──────────────┘
      │ N
      │
      │ 1
┌─────────────┐
│   Driver    │
│             │
└─────────────┘
```

---

## Table Definitions

### 1. User Table

**Purpose:** Stores all user accounts (customers, restaurant owners, delivery drivers)

| Field Name    | Data Type      | Constraints                    | Description                          |
|---------------|----------------|--------------------------------|--------------------------------------|
| id            | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique user identifier               |
| email         | VARCHAR(255)   | UNIQUE, NOT NULL               | User's email address                 |
| password      | VARCHAR(255)   | NOT NULL                       | Encrypted password                   |
| first_name    | VARCHAR(100)   | NOT NULL                       | User's first name                    |
| last_name     | VARCHAR(100)   | NOT NULL                       | User's last name                     |
| phone_number  | VARCHAR(20)    | UNIQUE, NOT NULL               | Contact phone number                 |
| role          | VARCHAR(50)    | NOT NULL                       | CUSTOMER, RESTAURANT_OWNER, DRIVER   |
| is_active     | BOOLEAN        | DEFAULT TRUE                   | Account active status                |
| created_at    | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Account creation time                |
| updated_at    | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                     |

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `email`
- UNIQUE INDEX on `phone_number`
- INDEX on `role`

---

### 2. Address Table

**Purpose:** Stores delivery addresses for users

| Field Name     | Data Type      | Constraints                    | Description                      |
|----------------|----------------|--------------------------------|----------------------------------|
| id             | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique address identifier        |
| user_id        | BIGINT         | FOREIGN KEY → User(id), NOT NULL | Owner of this address           |
| street_address | VARCHAR(255)   | NOT NULL                       | Street address                   |
| city           | VARCHAR(100)   | NOT NULL                       | City name                        |
| state          | VARCHAR(50)    | NOT NULL                       | State/Province                   |
| zip_code       | VARCHAR(20)    | NOT NULL                       | Postal code                      |
| latitude       | DECIMAL(10,8)  | NULL                           | GPS latitude                     |
| longitude      | DECIMAL(11,8)  | NULL                           | GPS longitude                    |
| is_default     | BOOLEAN        | DEFAULT FALSE                  | Default delivery address         |
| created_at     | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Creation time                    |

**Relationships:**
- Many addresses belong to one user (N:1)

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `user_id`
- INDEX on `user_id`

---

### 3. Restaurant Table

**Purpose:** Stores restaurant information

| Field Name      | Data Type      | Constraints                    | Description                      |
|-----------------|----------------|--------------------------------|----------------------------------|
| id              | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique restaurant identifier     |
| owner_id        | BIGINT         | FOREIGN KEY → User(id), NOT NULL | Restaurant owner               |
| name            | VARCHAR(200)   | NOT NULL                       | Restaurant name                  |
| description     | TEXT           | NULL                           | Restaurant description           |
| cuisine_type    | VARCHAR(100)   | NOT NULL                       | Type of cuisine                  |
| street_address  | VARCHAR(255)   | NOT NULL                       | Street address                   |
| city            | VARCHAR(100)   | NOT NULL                       | City name                        |
| state           | VARCHAR(50)    | NOT NULL                       | State/Province                   |
| zip_code        | VARCHAR(20)    | NOT NULL                       | Postal code                      |
| latitude        | DECIMAL(10,8)  | NOT NULL                       | GPS latitude                     |
| longitude       | DECIMAL(11,8)  | NOT NULL                       | GPS longitude                    |
| phone_number    | VARCHAR(20)    | NOT NULL                       | Restaurant phone                 |
| email           | VARCHAR(255)   | NULL                           | Restaurant email                 |
| opening_time    | TIME           | NOT NULL                       | Opening time                     |
| closing_time    | TIME           | NOT NULL                       | Closing time                     |
| is_active       | BOOLEAN        | DEFAULT TRUE                   | Restaurant active status         |
| rating          | DECIMAL(3,2)   | DEFAULT 0.00                   | Average rating (0-5.00)          |
| delivery_fee    | DECIMAL(10,2)  | NOT NULL                       | Base delivery fee                |
| minimum_order   | DECIMAL(10,2)  | DEFAULT 0.00                   | Minimum order amount             |
| created_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Creation time                    |
| updated_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                 |

**Relationships:**
- One restaurant belongs to one user (owner) (N:1)
- One restaurant has many menu items (1:N)
- One restaurant has many orders (1:N)

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `owner_id`
- INDEX on `cuisine_type`
- INDEX on `is_active`
- INDEX on `rating`

---

### 4. MenuItem Table

**Purpose:** Stores menu items for each restaurant

| Field Name      | Data Type      | Constraints                    | Description                      |
|-----------------|----------------|--------------------------------|----------------------------------|
| id              | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique menu item identifier      |
| restaurant_id   | BIGINT         | FOREIGN KEY → Restaurant(id), NOT NULL | Restaurant this item belongs to |
| name            | VARCHAR(200)   | NOT NULL                       | Item name                        |
| description     | TEXT           | NULL                           | Item description                 |
| category        | VARCHAR(100)   | NOT NULL                       | Category (Appetizer, Main, etc.) |
| price           | DECIMAL(10,2)  | NOT NULL                       | Item price                       |
| image_url       | VARCHAR(500)   | NULL                           | Image URL                        |
| is_available    | BOOLEAN        | DEFAULT TRUE                   | Availability status              |
| is_vegetarian   | BOOLEAN        | DEFAULT FALSE                  | Vegetarian option                |
| is_vegan        | BOOLEAN        | DEFAULT FALSE                  | Vegan option                     |
| spice_level     | INT            | DEFAULT 0                      | Spice level (0-5)                |
| preparation_time| INT            | DEFAULT 15                     | Prep time in minutes             |
| created_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Creation time                    |
| updated_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                 |

**Relationships:**
- Many menu items belong to one restaurant (N:1)
- One menu item appears in many order items (1:N)

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `restaurant_id`
- INDEX on `restaurant_id`
- INDEX on `category`
- INDEX on `is_available`

---

### 5. Order Table

**Purpose:** Stores customer orders

| Field Name           | Data Type      | Constraints                    | Description                      |
|---------------------|----------------|--------------------------------|----------------------------------|
| id                  | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique order identifier          |
| customer_id         | BIGINT         | FOREIGN KEY → User(id), NOT NULL | Customer who placed the order  |
| restaurant_id       | BIGINT         | FOREIGN KEY → Restaurant(id), NOT NULL | Restaurant for this order   |
| delivery_address_id | BIGINT         | FOREIGN KEY → Address(id), NOT NULL | Delivery address           |
| order_number        | VARCHAR(50)    | UNIQUE, NOT NULL               | Human-readable order number      |
| status              | VARCHAR(50)    | NOT NULL                       | PENDING, CONFIRMED, PREPARING, etc. |
| subtotal            | DECIMAL(10,2)  | NOT NULL                       | Items total                      |
| delivery_fee        | DECIMAL(10,2)  | NOT NULL                       | Delivery charge                  |
| tax                 | DECIMAL(10,2)  | NOT NULL                       | Tax amount                       |
| total_amount        | DECIMAL(10,2)  | NOT NULL                       | Final total                      |
| payment_method      | VARCHAR(50)    | NOT NULL                       | CARD, CASH, etc.                 |
| payment_status      | VARCHAR(50)    | DEFAULT 'PENDING'              | Payment status                   |
| special_instructions| TEXT           | NULL                           | Delivery instructions            |
| estimated_delivery  | TIMESTAMP      | NULL                           | Estimated delivery time          |
| actual_delivery     | TIMESTAMP      | NULL                           | Actual delivery time             |
| created_at          | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Order creation time              |
| updated_at          | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                 |

**Relationships:**
- Many orders belong to one customer (N:1)
- Many orders belong to one restaurant (N:1)
- Many orders use one address (N:1)
- One order has many order items (1:N)
- One order has one delivery info (1:1)
- One order can have one review (1:1)

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `order_number`
- FOREIGN KEY on `customer_id`
- FOREIGN KEY on `restaurant_id`
- FOREIGN KEY on `delivery_address_id`
- INDEX on `status`
- INDEX on `created_at`

---

### 6. OrderItem Table

**Purpose:** Stores individual items within an order (junction table)

| Field Name      | Data Type      | Constraints                    | Description                      |
|-----------------|----------------|--------------------------------|----------------------------------|
| id              | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique order item identifier     |
| order_id        | BIGINT         | FOREIGN KEY → Order(id), NOT NULL | Order this item belongs to    |
| menu_item_id    | BIGINT         | FOREIGN KEY → MenuItem(id), NOT NULL | Menu item ordered          |
| quantity        | INT            | NOT NULL                       | Number of items                  |
| unit_price      | DECIMAL(10,2)  | NOT NULL                       | Price per item at order time     |
| subtotal        | DECIMAL(10,2)  | NOT NULL                       | quantity × unit_price            |
| special_requests| TEXT           | NULL                           | Special requests for this item   |

**Relationships:**
- Many order items belong to one order (N:1)
- Many order items reference one menu item (N:1)

**Indexes:**
- PRIMARY KEY on `id`
- FOREIGN KEY on `order_id`
- FOREIGN KEY on `menu_item_id`
- INDEX on `order_id`

---

### 7. Driver Table

**Purpose:** Stores delivery driver information

| Field Name          | Data Type      | Constraints                    | Description                      |
|--------------------|----------------|--------------------------------|----------------------------------|
| id                 | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique driver identifier         |
| user_id            | BIGINT         | FOREIGN KEY → User(id), UNIQUE, NOT NULL | Associated user account  |
| license_number     | VARCHAR(50)    | UNIQUE, NOT NULL               | Driver's license number          |
| vehicle_type       | VARCHAR(50)    | NOT NULL                       | CAR, BIKE, SCOOTER, etc.         |
| vehicle_plate      | VARCHAR(20)    | NOT NULL                       | Vehicle license plate            |
| is_available       | BOOLEAN        | DEFAULT TRUE                   | Currently available for deliveries |
| current_latitude   | DECIMAL(10,8)  | NULL                           | Current GPS latitude             |
| current_longitude  | DECIMAL(11,8)  | NULL                           | Current GPS longitude            |
| rating             | DECIMAL(3,2)   | DEFAULT 0.00                   | Driver rating (0-5.00)           |
| total_deliveries   | INT            | DEFAULT 0                      | Total completed deliveries       |
| created_at         | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Registration time                |
| updated_at         | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                 |

**Relationships:**
- One driver has one user account (1:1)
- One driver has many delivery info records (1:N)

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `user_id`
- UNIQUE INDEX on `license_number`
- INDEX on `is_available`

---

### 8. DeliveryInfo Table

**Purpose:** Stores delivery tracking information for orders

| Field Name      | Data Type      | Constraints                    | Description                      |
|-----------------|----------------|--------------------------------|----------------------------------|
| id              | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique delivery info identifier  |
| order_id        | BIGINT         | FOREIGN KEY → Order(id), UNIQUE, NOT NULL | Associated order        |
| driver_id       | BIGINT         | FOREIGN KEY → Driver(id), NULL | Assigned driver                  |
| pickup_time     | TIMESTAMP      | NULL                           | When driver picked up order      |
| delivery_time   | TIMESTAMP      | NULL                           | When order was delivered         |
| status          | VARCHAR(50)    | DEFAULT 'PENDING'              | PENDING, ASSIGNED, PICKED_UP, etc. |
| tracking_notes  | TEXT           | NULL                           | Delivery notes/updates           |
| created_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Creation time                    |
| updated_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Last update time                 |

**Relationships:**
- One delivery info belongs to one order (1:1)
- Many delivery info records belong to one driver (N:1)

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `order_id`
- FOREIGN KEY on `order_id`
- FOREIGN KEY on `driver_id`
- INDEX on `status`

---

### 9. Review Table

**Purpose:** Stores customer reviews and ratings

| Field Name      | Data Type      | Constraints                    | Description                      |
|-----------------|----------------|--------------------------------|----------------------------------|
| id              | BIGINT         | PRIMARY KEY, AUTO_INCREMENT    | Unique review identifier         |
| order_id        | BIGINT         | FOREIGN KEY → Order(id), UNIQUE, NOT NULL | Reviewed order          |
| customer_id     | BIGINT         | FOREIGN KEY → User(id), NOT NULL | Customer who wrote review       |
| restaurant_id   | BIGINT         | FOREIGN KEY → Restaurant(id), NOT NULL | Reviewed restaurant      |
| food_rating     | INT            | NOT NULL                       | Food rating (1-5)                |
| delivery_rating | INT            | NOT NULL                       | Delivery rating (1-5)            |
| overall_rating  | DECIMAL(3,2)   | NOT NULL                       | Overall rating (calculated)      |
| comment         | TEXT           | NULL                           | Review text                      |
| created_at      | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP      | Review creation time             |

**Relationships:**
- One review belongs to one order (1:1)
- Many reviews belong to one customer (N:1)
- Many reviews belong to one restaurant (N:1)

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `order_id`
- FOREIGN KEY on `order_id`
- FOREIGN KEY on `customer_id`
- FOREIGN KEY on `restaurant_id`
- INDEX on `restaurant_id`

---

## Relationship Summary

### One-to-Many (1:N) Relationships

1. **User → Address**: One user can have multiple addresses
2. **User → Order (as customer)**: One user can place multiple orders
3. **User → Restaurant (as owner)**: One user can own multiple restaurants
4. **Restaurant → MenuItem**: One restaurant has multiple menu items
5. **Restaurant → Order**: One restaurant receives multiple orders
6. **Order → OrderItem**: One order contains multiple items
7. **MenuItem → OrderItem**: One menu item appears in multiple orders
8. **Driver → DeliveryInfo**: One driver handles multiple deliveries

### One-to-One (1:1) Relationships

1. **User ↔ Driver**: One user account per driver
2. **Order ↔ DeliveryInfo**: One delivery info per order
3. **Order ↔ Review**: One review per order

### Many-to-Many (M:N) Relationships

1. **Order ↔ MenuItem** (through OrderItem junction table)

---

## Data Type Explanations

- **BIGINT**: Large integer for IDs (supports billions of records)
- **VARCHAR(n)**: Variable-length string with max length n
- **TEXT**: Long text content
- **DECIMAL(p,s)**: Decimal number with p total digits and s decimal places
- **BOOLEAN**: True/false value
- **TIMESTAMP**: Date and time
- **TIME**: Time only
- **INT**: Standard integer

---

## Naming Conventions

- **Table names**: Singular, PascalCase (e.g., `User`, `MenuItem`)
- **Column names**: snake_case (e.g., `first_name`, `created_at`)
- **Foreign keys**: `{table}_id` (e.g., `user_id`, `restaurant_id`)
- **Junction tables**: Combination of related entities (e.g., `OrderItem`)

---

## Status Enums

### Order Status
- `PENDING` - Order placed, waiting for restaurant confirmation
- `CONFIRMED` - Restaurant confirmed the order
- `PREPARING` - Food is being prepared
- `READY_FOR_PICKUP` - Food ready, waiting for driver
- `PICKED_UP` - Driver picked up the order
- `IN_TRANSIT` - Order is being delivered
- `DELIVERED` - Order successfully delivered
- `CANCELLED` - Order was cancelled

### Payment Status
- `PENDING` - Payment not yet processed
- `COMPLETED` - Payment successful
- `FAILED` - Payment failed
- `REFUNDED` - Payment was refunded

### Delivery Status
- `PENDING` - Waiting for driver assignment
- `ASSIGNED` - Driver assigned
- `PICKED_UP` - Driver picked up order
- `IN_TRANSIT` - On the way to customer
- `DELIVERED` - Successfully delivered

---

## Next Steps

1. Create Entity classes based on this schema
2. Add JPA annotations (@Entity, @Table, @Column, etc.)
3. Define relationships (@OneToMany, @ManyToOne, @OneToOne)
4. Run the application - Hibernate will auto-create tables
5. Verify tables in pgAdmin
6. Start building Repository and Service layers

---
