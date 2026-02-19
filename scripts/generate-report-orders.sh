#!/bin/bash
set -uo pipefail

BASE_URL=${BASE_URL:-http://localhost:8081/api}

OWNER_EMAIL="report-owner@example.com"
OWNER_PASS="Password123!"
CUSTOMER_EMAIL="report-customer@example.com"
CUSTOMER_PASS="Password123!"

json_field() {
  python3 -c "import sys, json
data = sys.stdin.read().strip()
if not data:
    print('')
else:
    try:
        payload = json.loads(data)
        print(payload.get('$1', ''))
    except json.JSONDecodeError:
        print('')
"
}

login() {
  local email="$1"
  local password="$2"
  curl -s -X POST "${BASE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${email}\",\"password\":\"${password}\"}"
}

register_user() {
  local email="$1"
  local password="$2"
  local role="$3"
  local first="$4"
  local last="$5"
  local phone="$6"

  curl -s -X POST "${BASE_URL}/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${email}\",\"password\":\"${password}\",\"firstName\":\"${first}\",\"lastName\":\"${last}\",\"phone\":\"${phone}\",\"role\":\"${role}\"}"
}

ensure_user() {
  local email="$1"
  local password="$2"
  local role="$3"
  local first="$4"
  local last="$5"
  local phone="$6"

  local login_response
  login_response=$(login "$email" "$password")
  local token
  token=$(echo "$login_response" | json_field accessToken)

  if [ -z "$token" ]; then
    echo "Registering ${role} user ${email}..."
    register_user "$email" "$password" "$role" "$first" "$last" "$phone" > /dev/null
    login_response=$(login "$email" "$password")
    token=$(echo "$login_response" | json_field accessToken)
  fi

  if [ -z "$token" ]; then
    echo "❌ Unable to login ${email}"
    exit 1
  fi

  echo "$token"
}

OWNER_TOKEN=$(ensure_user "$OWNER_EMAIL" "$OWNER_PASS" "RESTAURANT_OWNER" "Report" "Owner" "+15550100001")
CUSTOMER_TOKEN=$(ensure_user "$CUSTOMER_EMAIL" "$CUSTOMER_PASS" "CUSTOMER" "Report" "Customer" "+15550100002")

echo "✅ Logged in owner/customer"

OWNER_RESPONSE=$(curl -s -H "Authorization: Bearer ${OWNER_TOKEN}" -w "HTTP_STATUS:%{http_code}" "${BASE_URL}/restaurants/owner")
OWNER_BODY=$(echo "$OWNER_RESPONSE" | sed -e 's/HTTP_STATUS:.*//g')
OWNER_STATUS=$(echo "$OWNER_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTP_STATUS://')
RESTAURANT_ID=$(echo "$OWNER_BODY" | json_field id)

if [ -z "$RESTAURANT_ID" ] || [ "$OWNER_STATUS" != "200" ]; then
  echo "Creating restaurant..."
  RESTAURANT_RESPONSE=$(curl -s -X POST "${BASE_URL}/restaurants" \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "报表测试餐厅",
      "description": "用于报表测试的餐厅",
      "cuisineType": "中餐",
      "streetAddress": "123 Report St",
      "city": "San Francisco",
      "state": "CA",
      "zipCode": "94102",
      "phoneNumber": "+14155550000"
    }')
  RESTAURANT_ID=$(echo "$RESTAURANT_RESPONSE" | json_field id)
fi

if [ -z "$RESTAURANT_ID" ]; then
  echo "❌ Unable to create restaurant"
  exit 1
fi

echo "Restaurant ID: ${RESTAURANT_ID}"

MENU_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${OWNER_TOKEN}" "${BASE_URL}/menu-items/restaurant/${RESTAURANT_ID}")
MENU_BODY=$(curl -s -H "Authorization: Bearer ${OWNER_TOKEN}" "${BASE_URL}/menu-items/restaurant/${RESTAURANT_ID}")
MENU_ITEM_COUNT=$(echo "$MENU_BODY" | python3 -c "import sys, json; data=sys.stdin.read().strip();
print(len(json.loads(data)) if data else 0)")

if [ "$MENU_ITEM_COUNT" -lt 3 ]; then
  echo "Creating menu items..."
  curl -s -X POST "${BASE_URL}/menu-items" \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"restaurantId\":${RESTAURANT_ID},\"name\":\"招牌牛肉面\",\"description\":\"浓汤牛肉面\",\"price\":18.8,\"category\":\"主食\",\"isAvailable\":true}" > /dev/null || true

  curl -s -X POST "${BASE_URL}/menu-items" \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"restaurantId\":${RESTAURANT_ID},\"name\":\"鸡丝凉面\",\"description\":\"清爽凉面\",\"price\":12.5,\"category\":\"主食\",\"isAvailable\":true}" > /dev/null || true

  curl -s -X POST "${BASE_URL}/menu-items" \
    -H "Authorization: Bearer ${OWNER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"restaurantId\":${RESTAURANT_ID},\"name\":\"酸梅汤\",\"description\":\"解腻饮品\",\"price\":5.0,\"category\":\"饮品\",\"isAvailable\":true}" > /dev/null || true
fi

MENU_RESPONSE=$(curl -s -H "Authorization: Bearer ${OWNER_TOKEN}" -w "HTTP_STATUS:%{http_code}" "${BASE_URL}/menu-items/restaurant/${RESTAURANT_ID}")
MENU_BODY=$(echo "$MENU_RESPONSE" | sed -e 's/HTTP_STATUS:.*//g')
MENU_STATUS=$(echo "$MENU_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTP_STATUS://')

if [ "$MENU_STATUS" != "200" ]; then
  echo "❌ Unable to fetch menu items (status $MENU_STATUS)"
  echo "$MENU_BODY"
  exit 1
fi

MENU_IDS=$(curl -s -H "Authorization: Bearer ${OWNER_TOKEN}" "${BASE_URL}/menu-items/restaurant/${RESTAURANT_ID}" | python3 -c "import sys, json; data=sys.stdin.read().strip();
items=json.loads(data) if data else [];
print(' '.join(str(item['id']) for item in items[:3]))")

set -- $MENU_IDS
ITEM1=${1:-}
ITEM2=${2:-}
ITEM3=${3:-}

if [ -z "$ITEM1" ] || [ -z "$ITEM2" ]; then
  echo "❌ Unable to fetch menu items"
  echo "Status: $MENU_STATUS"
  echo "$MENU_BODY"
  exit 1
fi

echo "Creating orders..."
for i in {1..3}; do
  curl -s -X POST "${BASE_URL}/orders" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"restaurantId\":${RESTAURANT_ID},\"deliveryAddress\":{\"streetAddress\":\"500 Market St\",\"city\":\"San Francisco\",\"state\":\"CA\",\"zipCode\":\"94105\"},\"items\":[{\"menuItemId\":${ITEM1},\"quantity\":1},{\"menuItemId\":${ITEM2},\"quantity\":2}],\"paymentMethod\":\"CREDIT_CARD\"}" > /dev/null

done

if [ -n "$ITEM3" ]; then
  curl -s -X POST "${BASE_URL}/orders" \
    -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"restaurantId\":${RESTAURANT_ID},\"deliveryAddress\":{\"streetAddress\":\"500 Market St\",\"city\":\"San Francisco\",\"state\":\"CA\",\"zipCode\":\"94105\"},\"items\":[{\"menuItemId\":${ITEM3},\"quantity\":3}],\"paymentMethod\":\"CREDIT_CARD\"}" > /dev/null
fi

echo "✅ Orders generated for reports"
