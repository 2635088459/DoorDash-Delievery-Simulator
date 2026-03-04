# Food Delivery Platform Simulator

Full-stack food delivery platform with Spring Boot + React. Think DoorDash/Uber Eats but simpler.

Three-sided marketplace with real-time updates, role-based portals, and an admin ticket center for anomaly review.

## System Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Customer   │         │ Restaurant  │         │   Driver    │
│   Portal    │         │    Portal   │         │   Portal    │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       └───────────────────────┼───────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Spring Boot API   │
                    │   (JWT + WebSocket) │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │    PostgreSQL DB    │
                    └─────────────────────┘
```

## Order Flow

```
Customer          Restaurant         Driver
   │                  │                │
   ├─── Order ───────>│                │
   │                  │                │
   │<── Confirmed ────┤                │
   │                  │                │
   │<── Preparing ────┤                │
   │                  │                │
   │<── Ready ────────┼──── Pickup ───>│
   │                  │                │
   │<────────── Picked Up ─────────────┤
   │                  │                │
   │<────────── In Transit ────────────┤
   │                  │                │
   │<────────── Delivered ─────────────┤
```

## Tech Stack

**Backend:** Spring Boot 3.2 | PostgreSQL | JWT Auth | WebSocket (STOMP)

**Frontend:** React 18 | Vite | Zustand | Tailwind CSS

**Deploy:** Docker Compose (backend), static hosting for frontend

## Features by Role

| Customer | Restaurant Owner | Driver | Admin |
|----------|------------------|--------|
| Browse restaurants | View orders | See available deliveries | Ticket board & audit logs |
| Shopping cart | Update order status | Update delivery status | Agent suggestions & evidence |
| Place orders | Statistics dashboard | Earnings tracking | Action execution write-back |
| Real-time notifications | Menu management* | Delivery history* | SLA monitoring |

*In progress

## Quick Start

```bash
# Clone repo
git clone https://github.com/2635088459/DoorDash-Delievery-Simulator.git
cd DoorDash-Delievery-Simulator

# Start backend (Docker)
docker-compose up -d

# Start frontend
cd frontend
npm install
npm run dev
```

Visit `http://localhost:3000`

### Test Accounts

```
Customer:     test@example.com / Password123!
Restaurant:   restaurantowner@test.com / Password123!
```

Or register new account (choose role during signup).

## Demo Workflow

1. Login as **customer** → Browse → Add to cart → Checkout
2. Login as **restaurant** (new window) → See order → Confirm → Prepare → Ready
3. Login as **driver** (new window) → Pick up → Deliver

Each action triggers real-time notifications across all users.

## Frontend Deployment (Bluehost subpath)

This project supports hosting the frontend under a subpath (e.g., `/livefood`).

1) Build the frontend:

```bash
cd frontend
npm run build
```

2) Upload `frontend/dist/` contents to `public_html/livefood/`.

3) Ensure the SPA `.htaccess` file exists at `public_html/livefood/.htaccess`.

4) If your main site has rewrite rules, add a bypass rule at the top of `public_html/.htaccess`:

```
RewriteRule ^livefood/ - [L]
```

### Backend API Base URL

The frontend calls `/api/*` by default. For production hosting, set the API base URL:

```bash
export VITE_API_BASE_URL="https://YOUR-BACKEND-DOMAIN"
npm run build
```

Or create `frontend/.env.production`:

```
VITE_API_BASE_URL=https://YOUR-BACKEND-DOMAIN
```

## Project Structure

```
├── src/main/java/com/shydelivery/
│   ├── controller/      # REST endpoints
│   ├── service/         # Business logic
│   ├── entity/          # Database models
│   └── security/        # JWT auth
├── frontend/src/
│   ├── pages/           # Route pages
│   ├── components/      # Reusable UI
│   └── services/        # API calls
└── docker-compose.yml   # Container config
```

## Roadmap

- [x] Multi-role auth system
- [x] Order management
- [x] Real-time notifications  
- [x] Driver delivery flow
- [ ] Menu CRUD for restaurants
- [ ] Coupon system
- [ ] Reviews & ratings
- [ ] Payment integration
- [ ] Map/GPS tracking

## Known Issues

- WebSocket reconnection needs improvement
- Menu management UI incomplete
- Driver order claiming (auto-assign vs manual)

## Notes

Work in progress. Code isn't perfect but it works. Feel free to use as reference or contribute.

---

**Last updated:** March 2026
