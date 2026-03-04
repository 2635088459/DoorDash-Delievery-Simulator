# Regression Smoke Script

Quick automated regression checks for the admin/agent workflow.

## What it tests
- Admin login and token retrieval
- Core endpoints: `/tickets`, `/tickets/actions/logs`, `/auth/test`
- Create a test ticket if none exist
- Ticket detail flow + SLA fields
- Execute action + result writeback (auto-resolve)
- Optional auto-close validation

## Usage
```bash
chmod +x ./scripts/regression-smoke.sh
./scripts/regression-smoke.sh
```

## Optional environment overrides
```bash
BASE_URL=http://localhost:8081/api \
ADMIN_EMAIL=admin@doordash.local \
ADMIN_PASSWORD=Admin123! \
AUTO_CLOSE_WAIT_SEC=75 \
./scripts/regression-smoke.sh
```

## Notes
- Auto-close validation is optional. Set `AUTO_CLOSE_WAIT_SEC` after lowering `APP_TICKET_AUTO_CLOSE_HOURS` (e.g., 0.02 ~ 72s).
- Use this after backend restarts or auth changes.
