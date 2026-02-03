# Food Delivery Platform Simulator

Full-stack food delivery platform with Spring Boot + React. Think DoorDash/Uber Eats but simpler.

Started as practice, turned into something more complete. Three-sided marketplace with real-time updates.

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

**Deploy:** Docker Compose

## Features by Role

| Customer | Restaurant Owner | Driver |
|----------|------------------|--------|
| Browse restaurants | View orders | See available deliveries |
| Shopping cart | Update order status | Update delivery status |
| Place orders | Statistics dashboard | Earnings tracking |
| Real-time notifications | Menu management* | Delivery history* |

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

**Last updated:** January 2026
