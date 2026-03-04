# Seed Agent Tickets Script

Batch-create tickets with optional evidence, SLA overdue backdating, SLA alert injection, and action execution.

## Usage
```bash
chmod +x ./scripts/seed-agent-tickets.sh
COUNT=10 ./scripts/seed-agent-tickets.sh
```

## Options
- `COUNT` - number of tickets to create (default: 12)
- `FIXED_CATEGORY` - force category for all tickets
- `FIXED_RESTAURANT_ID` - force restaurant id for all tickets
- `WITH_EVIDENCE` - generate evidenceJson (true/false)
- `OVERDUE_COUNT` - backdate N tickets to simulate SLA overdue
- `INJECT_SLA_ALERT` - insert SLA_ALERT system notes (true/false)
- `EXECUTE_ACTIONS` - execute agent actions and write back success (true/false)
- `AUTO_CLOSE_WAIT_SEC` - wait N seconds to observe auto-close changes

## Examples
```bash
# fixed category + restaurant, with SLA overdue
COUNT=5 FIXED_CATEGORY=DELIVERY_TIMEOUT_SPIKE FIXED_RESTAURANT_ID=2 OVERDUE_COUNT=2 ./scripts/seed-agent-tickets.sh

# generate SLA alerts and action writeback
COUNT=3 INJECT_SLA_ALERT=true EXECUTE_ACTIONS=true ./scripts/seed-agent-tickets.sh

# wait for auto-close (set APP_TICKET_AUTO_CLOSE_HOURS before backend start)
COUNT=2 EXECUTE_ACTIONS=true AUTO_CLOSE_WAIT_SEC=75 ./scripts/seed-agent-tickets.sh
```

## Notes
- Auto-close uses `APP_TICKET_AUTO_CLOSE_HOURS` from backend environment. Set it when starting the backend container.
- SLA_ALERT injection writes system notes directly to `ticket_comments`.
