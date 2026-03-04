#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@doordash.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin123!}"
AUTO_CLOSE_WAIT_SEC="${AUTO_CLOSE_WAIT_SEC:-}"

print_step() {
  echo -e "\n==> $1"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_cmd curl
require_cmd python3

print_step "Login and obtain token"
TOKEN=$(curl -s "${BASE_URL}/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}" | \
  python3 -c 'import sys, json; print(json.load(sys.stdin)["accessToken"])')

if [[ -z "${TOKEN}" ]]; then
  echo "Failed to retrieve access token" >&2
  exit 1
fi

auth_get() {
  local path="$1"
  curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}${path}"
}

auth_post() {
  local path="$1"
  local payload="$2"
  curl -s -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
    -d "${payload}" "${BASE_URL}${path}"
}

fetch_json() {
  local path="$1"
  local tmp
  tmp=$(mktemp)
  local code
  code=$(curl -s -o "${tmp}" -w "%{http_code}" -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}${path}")
  FETCH_BODY=$(cat "${tmp}")
  rm -f "${tmp}"
  FETCH_CODE="${code}"
}


print_step "Check core endpoints"
TICKETS_CODE=$(auth_get "/tickets")
AUDIT_CODE=$(auth_get "/tickets/actions/logs")
AUTH_TEST_CODE=$(auth_get "/auth/test")

echo "tickets: ${TICKETS_CODE}"
echo "audit:   ${AUDIT_CODE}"
echo "auth:    ${AUTH_TEST_CODE}"

if [[ "${TICKETS_CODE}" != "200" || "${AUDIT_CODE}" != "200" || "${AUTH_TEST_CODE}" != "200" ]]; then
  echo "Core endpoint check failed" >&2
  exit 1
fi

print_step "Fetch a ticket (if available)"
TICKET_ID=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets" | \
  python3 -c 'import sys, json; data=json.load(sys.stdin); print(data[0]["id"] if data else "")')

if [[ -z "${TICKET_ID}" || ! "${TICKET_ID}" =~ ^[0-9]+$ ]]; then
  print_step "Create a test ticket"
  CREATE_PAYLOAD=$(cat <<EOF
{"title":"Auto Regression Ticket","description":"Smoke test ticket","category":"PAYMENT_ISSUE","priority":"HIGH","source":"MANUAL","assignedRole":"OPERATIONS"}
EOF
  )
  CREATE_RESPONSE=$(curl -s -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
    -d "${CREATE_PAYLOAD}" -w "\n%{http_code}" "${BASE_URL}/tickets")
  CREATE_BODY=$(printf "%s" "${CREATE_RESPONSE}" | sed '$d')
  CREATE_CODE=$(printf "%s" "${CREATE_RESPONSE}" | tail -n 1)
  if [[ "${CREATE_CODE}" != "200" ]]; then
    echo "Create ticket failed (${CREATE_CODE}): ${CREATE_BODY}" >&2
    exit 1
  fi
  TICKET_ID=$(python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
print(data.get("id",""))
PY
  <<<"${CREATE_BODY}")
fi

if [[ -z "${TICKET_ID}" ]]; then
  echo "Failed to create ticket for regression flow" >&2
  exit 1
fi

echo "Using ticket ID: ${TICKET_ID}"

DETAIL_CODE=$(auth_get "/tickets/${TICKET_ID}")
SAMPLE_CODE=$(auth_get "/tickets/${TICKET_ID}/samples")
SUMMARY_CODE=$(auth_get "/tickets/${TICKET_ID}/summary")
ACTIONS_CODE=$(auth_get "/tickets/${TICKET_ID}/actions/logs")

echo "detail:  ${DETAIL_CODE}"
echo "samples: ${SAMPLE_CODE}"
echo "summary: ${SUMMARY_CODE}"
echo "actions: ${ACTIONS_CODE}"

if [[ "${DETAIL_CODE}" != "200" || "${SAMPLE_CODE}" != "200" || "${SUMMARY_CODE}" != "200" || "${ACTIONS_CODE}" != "200" ]]; then
  echo "Ticket detail flow failed" >&2
  exit 1
fi

print_step "Validate SLA fields"
fetch_json "/tickets/${TICKET_ID}"
SLA_BODY="${FETCH_BODY}"
SLA_CODE="${FETCH_CODE}"
if [[ "${SLA_CODE}" != "200" ]]; then
  echo "SLA fetch failed (${SLA_CODE}): ${SLA_BODY}" >&2
  exit 1
fi
SLA_DEADLINE_PRESENT=$(echo "${SLA_BODY}" | grep -o '"slaDeadline"' || true)
SLA_OVERDUE_VALUE=$(echo "${SLA_BODY}" | grep -o '"slaOverdue"[^,}]*' | head -n 1 | sed 's/.*://')
echo "deadline ${SLA_DEADLINE_PRESENT:+true}"
echo "overdue ${SLA_OVERDUE_VALUE:-unknown}"

print_step "Execute action + writeback result"
ACTION_RESPONSE=$(auth_post "/tickets/${TICKET_ID}/actions" '{"actionType":"NOTIFY_RESTAURANT","note":"Smoke test action","markResolved":false}')
fetch_json "/tickets/${TICKET_ID}/actions/logs"
ACTION_BODY="${FETCH_BODY}"
ACTION_CODE="${FETCH_CODE}"
if [[ "${ACTION_CODE}" != "200" ]]; then
  echo "Fetch action logs failed (${ACTION_CODE}): ${ACTION_BODY}" >&2
  exit 1
fi
ACTION_LOG_ID=$(echo "${ACTION_BODY}" | grep -o '"id"\s*:\s*[0-9]*' | head -n 1 | grep -o '[0-9]*')

if [[ -z "${ACTION_LOG_ID}" ]]; then
  echo "Failed to create action log" >&2
  exit 1
fi

ACTION_RESULT_RESPONSE=$(curl -s -X PATCH -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' \
  -d '{"status":"SUCCESS","resultMessage":"Smoke test success"}' -w "\n%{http_code}" "${BASE_URL}/tickets/${TICKET_ID}/actions/${ACTION_LOG_ID}")
ACTION_RESULT_BODY=$(printf "%s" "${ACTION_RESULT_RESPONSE}" | sed '$d')
ACTION_RESULT_CODE=$(printf "%s" "${ACTION_RESULT_RESPONSE}" | tail -n 1)
if [[ "${ACTION_RESULT_CODE}" != "200" ]]; then
  echo "Action result writeback failed (${ACTION_RESULT_CODE}): ${ACTION_RESULT_BODY}" >&2
  exit 1
fi

UPDATED_STATUS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets/${TICKET_ID}" | \
  grep -o '"status"\s*:\s*"[^"]*"' | head -n 1 | sed 's/.*:"//;s/"//')
echo "status_after_action: ${UPDATED_STATUS}"

if [[ "${UPDATED_STATUS}" != "RESOLVED" && "${UPDATED_STATUS}" != "CLOSED" ]]; then
  echo "Expected ticket to resolve after action result" >&2
  exit 1
fi

if [[ -n "${AUTO_CLOSE_WAIT_SEC}" ]]; then
  print_step "Wait for auto-close (${AUTO_CLOSE_WAIT_SEC}s)"
  sleep "${AUTO_CLOSE_WAIT_SEC}"
  FINAL_STATUS=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${BASE_URL}/tickets/${TICKET_ID}" | \
    python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
print(data.get("status"))
PY
  )
  echo "status_after_wait: ${FINAL_STATUS}"
  if [[ "${FINAL_STATUS}" != "CLOSED" ]]; then
    echo "Auto-close validation failed" >&2
    exit 1
  fi
else
  echo "Auto-close validation skipped (set AUTO_CLOSE_WAIT_SEC to enable)"
fi

echo -e "\nSmoke regression checks passed ✅"
