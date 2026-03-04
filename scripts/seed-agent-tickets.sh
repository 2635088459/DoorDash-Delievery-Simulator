#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@doordash.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!}"
COUNT="${COUNT:-12}"
FIXED_CATEGORY="${FIXED_CATEGORY:-}"
FIXED_RESTAURANT_ID="${FIXED_RESTAURANT_ID:-}"
WITH_EVIDENCE="${WITH_EVIDENCE:-true}"
OVERDUE_COUNT="${OVERDUE_COUNT:-0}"
INJECT_SLA_ALERT="${INJECT_SLA_ALERT:-false}"
EXECUTE_ACTIONS="${EXECUTE_ACTIONS:-false}"
AUTO_CLOSE_WAIT_SEC="${AUTO_CLOSE_WAIT_SEC:-}"
AUTO_CLOSE_HOURS="${AUTO_CLOSE_HOURS:-}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd curl
require_cmd python3

TOKEN=$(curl -s "${BASE_URL}/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}" | \
  python3 -c 'import sys, json; print(json.load(sys.stdin)["accessToken"])')

if [[ -z "${TOKEN}" ]]; then
  echo "Failed to retrieve access token" >&2
  exit 1
fi

CATEGORIES=(
  "RESTAURANT_CANCEL_SPIKE"
  "DELIVERY_DELAY_SPIKE"
  "DELIVERY_TIMEOUT_SPIKE"
  "PAYMENT_REFUND_SPIKE"
  "PAYMENT_ISSUE"
  "DRIVER_ISSUE"
  "OTHER"
)

PRIORITIES=("LOW" "NORMAL" "HIGH" "URGENT")

TITLE_PREFIX="Agent Seed"

create_ticket() {
  local title="$1"
  local desc="$2"
  local category="$3"
  local priority="$4"
  local restaurantId="$5"
  local assignedRole="$6"
  local evidenceJson="$7"

  curl -s -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
    -d "{\"title\":\"${title}\",\"description\":\"${desc}\",\"category\":\"${category}\",\"priority\":\"${priority}\",\"source\":\"MANUAL\",\"restaurantId\":${restaurantId},\"assignedRole\":\"${assignedRole}\",\"evidenceJson\":${evidenceJson}}" \
    "${BASE_URL}/tickets" >/dev/null
}

auth_get() {
  local path="$1"
  curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}${path}"
}

auth_patch() {
  local path="$1"
  local payload="$2"
  curl -s -X PATCH -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
    -d "${payload}" "${BASE_URL}${path}" >/dev/null
}

build_evidence() {
  if [[ "${WITH_EVIDENCE}" != "true" ]]; then
    echo "null"
    return
  fi
  local cancelRate
  local delayRate
  local timeoutRate
  local refundRate
  cancelRate=$(python3 - <<'PY'
import random
print(round(random.uniform(0.05, 0.25), 4))
PY
  )
  delayRate=$(python3 - <<'PY'
import random
print(round(random.uniform(0.03, 0.2), 4))
PY
  )
  timeoutRate=$(python3 - <<'PY'
import random
print(round(random.uniform(0.01, 0.12), 4))
PY
  )
  refundRate=$(python3 - <<'PY'
import random
print(round(random.uniform(0.02, 0.18), 4))
PY
  )
  python3 - <<PY
import json
payload={
  "cancelRate": ${cancelRate},
  "delayRate": ${delayRate},
  "timeoutRate": ${timeoutRate},
  "refundRate": ${refundRate},
  "totalOrders": 120,
  "cancelledOrders": 18,
  "previousCancelRate": 0.04,
  "previousCancelledOrders": 5,
  "previousTotalOrders": 100
}
print(json.dumps(payload))
PY
}

for i in $(seq 1 "${COUNT}"); do
  if [[ -n "${FIXED_CATEGORY}" ]]; then
    category="${FIXED_CATEGORY}"
  else
    category=${CATEGORIES[$((RANDOM % ${#CATEGORIES[@]}))]}
  fi
  priority=${PRIORITIES[$((RANDOM % ${#PRIORITIES[@]}))]}
  if [[ -n "${FIXED_RESTAURANT_ID}" ]]; then
    restaurantId="${FIXED_RESTAURANT_ID}"
  else
    restaurantId=$((RANDOM % 10 + 1))
  fi
  assignedRole="OPERATIONS"
  if [[ "${category}" == "PAYMENT_REFUND_SPIKE" || "${category}" == "PAYMENT_ISSUE" ]]; then
    assignedRole="SUPPORT"
  fi
  if [[ "${category}" == "OTHER" ]]; then
    assignedRole="ENGINEERING"
  fi

  title="${TITLE_PREFIX} #${i} - ${category}"
  desc="自动生成测试工单 ${i}，用于验证 Agent 建议与 SLA 流程"
  evidenceJson=$(build_evidence)

  create_ticket "${title}" "${desc}" "${category}" "${priority}" "${restaurantId}" "${assignedRole}" "${evidenceJson}"
  echo "Created: ${title} (priority ${priority}, restaurant ${restaurantId})"
  sleep 0.2
done

if [[ "${OVERDUE_COUNT}" -gt 0 ]]; then
  echo "Backdating ${OVERDUE_COUNT} tickets to simulate SLA overdue..."
  IDS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets" | \
    grep -o '"id"\s*:\s*[0-9]*' | head -n "${OVERDUE_COUNT}" | grep -o '[0-9]*' | tr '\n' ' ')
  IDS=$(echo "${IDS}" | xargs)
  if [[ -n "${IDS}" ]]; then
    docker exec -i doordash-postgres psql -U postgres -d doordash_db \
      -c "UPDATE tickets SET created_at = now() - interval '30 hours', updated_at = now() - interval '30 hours' WHERE id IN (${IDS// /,});"
  fi
fi

if [[ "${INJECT_SLA_ALERT}" == "true" ]]; then
  echo "Injecting SLA_ALERT system notes..."
  IDS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets" | \
    grep -o '"id"\s*:\s*[0-9]*' | head -n 3 | grep -o '[0-9]*' | tr '\n' ' ')
  IDS=$(echo "${IDS}" | xargs)
  if [[ -n "${IDS}" ]]; then
    for id in ${IDS}; do
      docker exec -i doordash-postgres psql -U postgres -d doordash_db \
        -c "INSERT INTO ticket_comments (ticket_id, author, author_role, comment_type, content, evidence_json, created_at) VALUES (${id}, 'SYSTEM', 'SYSTEM', 'SYSTEM_NOTE', 'SLA 超时提醒: 已超时 3.5 小时', '{\"summaryType\":\"SLA_ALERT\",\"slaHours\":4,\"overdueMinutes\":210,\"status\":\"IN_PROGRESS\"}', now());"
    done
  fi
fi

if [[ "${EXECUTE_ACTIONS}" == "true" ]]; then
  echo "Executing actions and writing back results..."
  IDS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets" | \
    grep -o '"id"\s*:\s*[0-9]*' | head -n 3 | grep -o '[0-9]*' | tr '\n' ' ')
  IDS=$(echo "${IDS}" | xargs)
  if [[ -n "${IDS}" ]]; then
    for id in ${IDS}; do
      curl -s -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
        -d '{"actionType":"NOTIFY_RESTAURANT","note":"Seed action","markResolved":false}' "${BASE_URL}/tickets/${id}/actions" >/dev/null
      ACTION_ID=$(auth_get "/tickets/${id}/actions/logs" | grep -o '"id"\s*:\s*[0-9]*' | head -n 1 | grep -o '[0-9]*')
      if [[ -n "${ACTION_ID}" ]]; then
        auth_patch "/tickets/${id}/actions/${ACTION_ID}" '{"status":"SUCCESS","resultMessage":"Seed success"}'
      fi
    done
  fi
fi

if [[ -n "${AUTO_CLOSE_HOURS}" ]]; then
  docker exec -i doordash-app printenv >/dev/null 2>&1
  echo "Note: AUTO_CLOSE_HOURS configured in env only for new containers. Use APP_TICKET_AUTO_CLOSE_HOURS when starting backend."
fi

if [[ -n "${AUTO_CLOSE_WAIT_SEC}" ]]; then
  echo "Waiting ${AUTO_CLOSE_WAIT_SEC}s for auto-close to run..."
  sleep "${AUTO_CLOSE_WAIT_SEC}"
  IDS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets" | \
    grep -o '"id"\s*:\s*[0-9]*' | head -n 3 | grep -o '[0-9]*' | tr '\n' ' ')
  IDS=$(echo "${IDS}" | xargs)
  if [[ -n "${IDS}" ]]; then
    echo "Auto-close status check:"
    for id in ${IDS}; do
      status=$(auth_get "/tickets/${id}" | grep -o '"status"\s*:\s*"[^"]*"' | head -n 1 | sed 's/.*:"//;s/"//')
      echo "- ticket ${id}: ${status}"
    done
  fi
fi
