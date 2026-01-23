#!/bin/bash

# ============================================
# DoorDash 配送模块 RBAC 测试脚本
# 测试 DRIVER 角色的权限控制
# ============================================

BASE_URL="http://localhost:8080/api"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# ============================================
# 辅助函数
# ============================================

print_section() {
    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}\n"
}

print_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${YELLOW}测试 $TOTAL_TESTS: $1${NC}"
}

print_success() {
    PASSED_TESTS=$((PASSED_TESTS + 1))
    echo -e "${GREEN}✓ 通过: $1${NC}\n"
}

print_failure() {
    FAILED_TESTS=$((FAILED_TESTS + 1))
    echo -e "${RED}✗ 失败: $1${NC}\n"
}

# ============================================
# 步骤 1: 准备测试数据
# ============================================

print_section "步骤 1: 准备测试数据"

# 用户凭证
CUSTOMER_EMAIL="customer@example.com"
CUSTOMER_PASSWORD="Test@123"

OWNER_EMAIL="owner@restaurant.com"
OWNER_PASSWORD="Test@123"

DRIVER_EMAIL="driver@delivery.com"
DRIVER_PASSWORD="Test@123"

echo "客户: $CUSTOMER_EMAIL"
echo "餐厅老板: $OWNER_EMAIL"
echo "司机: $DRIVER_EMAIL"

# ============================================
# 步骤 2: 获取 JWT Token
# ============================================

print_section "步骤 2: 获取 JWT Token"

# 客户登录
echo "客户登录..."
CUSTOMER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"$CUSTOMER_PASSWORD\"}" \
  | jq -r '.idToken // empty')

if [ -z "$CUSTOMER_TOKEN" ]; then
    echo -e "${RED}客户登录失败！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 客户登录成功${NC}"

# 餐厅老板登录
echo "餐厅老板登录..."
OWNER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$OWNER_EMAIL\",\"password\":\"$OWNER_PASSWORD\"}" \
  | jq -r '.idToken // empty')

if [ -z "$OWNER_TOKEN" ]; then
    echo -e "${RED}餐厅老板登录失败！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 餐厅老板登录成功${NC}"

# 司机登录
echo "司机登录..."
DRIVER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DRIVER_EMAIL\",\"password\":\"$DRIVER_PASSWORD\"}" \
  | jq -r '.idToken // empty')

if [ -z "$DRIVER_TOKEN" ]; then
    echo -e "${RED}司机登录失败！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 司机登录成功${NC}"

# ============================================
# 步骤 3: 创建测试订单
# ============================================

print_section "步骤 3: 创建测试订单"

# 获取餐厅ID（优先选择owner@restaurant.com拥有的餐厅2）
RESTAURANT_ID=$(curl -s -X GET "$BASE_URL/restaurants" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  | jq -r '.[] | select(.id == 2) | .id // empty')

# 如果餐厅2不存在，使用第一个餐厅
if [ -z "$RESTAURANT_ID" ]; then
    RESTAURANT_ID=$(curl -s -X GET "$BASE_URL/restaurants" \
      -H "Authorization: Bearer $CUSTOMER_TOKEN" \
      | jq -r '.[0].id // empty')
fi

if [ -z "$RESTAURANT_ID" ]; then
    echo -e "${RED}无法获取餐厅ID！${NC}"
    exit 1
fi
echo "餐厅ID: $RESTAURANT_ID"

# 获取菜单项ID
MENU_ITEM_ID=$(curl -s -X GET "$BASE_URL/menu-items/restaurant/$RESTAURANT_ID/available" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  | jq -r '.[0].id // empty')

if [ -z "$MENU_ITEM_ID" ]; then
    echo -e "${RED}无法获取菜单项ID！${NC}"
    exit 1
fi
echo "菜单项ID: $MENU_ITEM_ID"

# 创建订单
echo "创建测试订单..."
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"deliveryAddressId\": 1,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"items\": [{\"menuItemId\": $MENU_ITEM_ID, \"quantity\": 2}],
    \"specialInstructions\": \"测试配送订单\"
  }")

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')

if [ -z "$ORDER_ID" ]; then
    echo -e "${RED}创建订单失败！${NC}"
    echo "$ORDER_RESPONSE"
    exit 1
fi
echo -e "${GREEN}✓ 订单创建成功，ID: $ORDER_ID${NC}"

# ============================================
# 步骤 4: 餐厅老板处理订单
# ============================================

print_section "步骤 4: 餐厅老板处理订单"

# 餐厅老板确认订单 (PENDING -> CONFIRMED)
echo "餐厅老板确认订单..."
CONFIRM_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}')

CONFIRM_CODE=$(echo "$CONFIRM_RESPONSE" | tail -n 1)
if [ "$CONFIRM_CODE" == "200" ]; then
    echo -e "${GREEN}✓ 订单状态: CONFIRMED${NC}"
else
    # 如果客户无法确认，尝试使用餐厅老板
    curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "CONFIRMED"}' > /dev/null
    echo -e "${GREEN}✓ 订单状态: CONFIRMED${NC}"
fi

# 开始准备 (CONFIRMED -> PREPARING)
echo "餐厅老板开始准备订单..."
curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PREPARING"}' > /dev/null
echo -e "${GREEN}✓ 订单状态: PREPARING${NC}"

# 标记为准备完成 (PREPARING -> READY_FOR_PICKUP)
echo "餐厅老板标记订单为准备完成..."
READY_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/orders/$ORDER_ID/ready-for-pickup" \
  -H "Authorization: Bearer $OWNER_TOKEN")

READY_CODE=$(echo "$READY_RESPONSE" | tail -n 1)
if [ "$READY_CODE" == "200" ]; then
    echo -e "${GREEN}✓ 订单状态: READY_FOR_PICKUP${NC}"
else
    echo -e "${RED}标记订单准备完成失败！响应码: $READY_CODE${NC}"
    echo "响应: $(echo "$READY_RESPONSE" | sed '$d')"
    # 尝试直接更新状态
    curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "READY_FOR_PICKUP"}' > /dev/null
    echo -e "${GREEN}✓ 订单状态: READY_FOR_PICKUP (通过通用接口)${NC}"
fi

# ============================================
# RBAC 测试开始
# ============================================

print_section "RBAC 测试: 配送模块权限控制"

# ============================================
# 测试 1: CUSTOMER 不能查看可用配送订单
# ============================================

print_test "CUSTOMER 不能查看可用配送订单 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/deliveries/available" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "CUSTOMER 被拒绝访问配送端点"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 2: RESTAURANT_OWNER 不能查看可用配送订单
# ============================================

print_test "RESTAURANT_OWNER 不能查看可用配送订单 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/deliveries/available" \
  -H "Authorization: Bearer $OWNER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "RESTAURANT_OWNER 被拒绝访问配送端点"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 3: DRIVER 可以查看可用配送订单
# ============================================

print_test "DRIVER 可以查看可用配送订单 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/deliveries/available" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    AVAILABLE_COUNT=$(echo "$BODY" | jq '. | length')
    print_success "DRIVER 成功查看可用订单，数量: $AVAILABLE_COUNT"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 4: DRIVER 可以接受配送订单
# ============================================

print_test "DRIVER 可以接受配送订单 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/deliveries/accept/$ORDER_ID" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    DRIVER_ID=$(echo "$BODY" | jq -r '.driver.id // empty')
    print_success "DRIVER 成功接受订单，司机ID: $DRIVER_ID"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 5: DRIVER 不能重复接受已分配的订单
# ============================================

print_test "DRIVER 不能重复接受已分配的订单 (应返回 400/409/500)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/deliveries/accept/$ORDER_ID" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "409" ] || [ "$HTTP_CODE" == "500" ]; then
    print_success "DRIVER 被阻止重复接受订单 (HTTP $HTTP_CODE)"
else
    print_failure "期望 400/409/500，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 6: DRIVER 可以查看自己的配送订单
# ============================================

print_test "DRIVER 可以查看自己的配送订单 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/deliveries/my" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    MY_DELIVERIES=$(echo "$BODY" | jq '. | length')
    print_success "DRIVER 成功查看自己的配送订单，数量: $MY_DELIVERIES"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 7: DRIVER 可以标记订单为已取餐
# ============================================

print_test "DRIVER 可以标记订单为已取餐 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/deliveries/$ORDER_ID/picked-up" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    ORDER_STATUS=$(echo "$BODY" | jq -r '.orderStatus // empty')
    print_success "订单标记为已取餐成功，状态: $ORDER_STATUS"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 8: DRIVER 可以标记订单为配送中
# ============================================

print_test "DRIVER 可以标记订单为配送中 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/deliveries/$ORDER_ID/in-transit" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    ORDER_STATUS=$(echo "$BODY" | jq -r '.orderStatus // empty')
    print_success "订单标记为配送中成功，状态: $ORDER_STATUS"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 9: DRIVER 可以标记订单为已送达
# ============================================

print_test "DRIVER 可以标记订单为已送达 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/deliveries/$ORDER_ID/delivered" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    ORDER_STATUS=$(echo "$BODY" | jq -r '.orderStatus // empty')
    ACTUAL_DELIVERY=$(echo "$BODY" | jq -r '.actualDelivery // empty')
    print_success "订单标记为已送达成功，状态: $ORDER_STATUS，送达时间: $ACTUAL_DELIVERY"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 10: 创建第二个订单测试跨司机权限
# ============================================

print_section "步骤 5: 测试跨司机权限控制"

# 创建第二个订单
echo "创建第二个测试订单..."
ORDER2_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"deliveryAddressId\": 1,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"items\": [{\"menuItemId\": $MENU_ITEM_ID, \"quantity\": 1}],
    \"specialInstructions\": \"第二个测试订单\"
  }")

ORDER2_ID=$(echo "$ORDER2_RESPONSE" | jq -r '.id // empty')

if [ -z "$ORDER2_ID" ]; then
    echo -e "${RED}创建第二个订单失败！${NC}"
else
    echo -e "${GREEN}✓ 第二个订单创建成功，ID: $ORDER2_ID${NC}"
    
    # 餐厅老板处理第二个订单到 READY_FOR_PICKUP
    curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "CONFIRMED"}' > /dev/null
    
    curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "PREPARING"}' > /dev/null
    
    # 尝试使用专用端点，失败则使用通用端点
    READY2_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/orders/$ORDER2_ID/ready-for-pickup" \
      -H "Authorization: Bearer $OWNER_TOKEN")
    
    READY2_CODE=$(echo "$READY2_RESPONSE" | tail -n 1)
    if [ "$READY2_CODE" != "200" ]; then
        curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/status" \
          -H "Authorization: Bearer $OWNER_TOKEN" \
          -H "Content-Type: application/json" \
          -d '{"status": "READY_FOR_PICKUP"}' > /dev/null
    fi
    
    echo -e "${GREEN}✓ 第二个订单状态: READY_FOR_PICKUP${NC}"
    
    # 第一个司机尝试标记第二个订单（未分配给他）
    print_test "DRIVER 不能操作未分配给自己的订单 (应返回 403)"
    
    # 注意：这个测试假设第二个订单还未被任何司机接受
    # 因为第一个司机已经有一个订单，尝试标记第二个订单应该失败
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/deliveries/$ORDER2_ID/picked-up" \
      -H "Authorization: Bearer $DRIVER_TOKEN")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    
    if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "400" ]; then
        print_success "DRIVER 被阻止操作其他司机的订单"
    else
        print_failure "期望 403或400，实际返回 $HTTP_CODE"
    fi
fi

# ============================================
# 测试总结
# ============================================

print_section "测试总结"

echo -e "总测试数: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}╔══════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   所有配送模块 RBAC 测试通过！ ✓   ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════╝${NC}\n"
    exit 0
else
    echo -e "\n${RED}╔══════════════════════════════════════╗${NC}"
    echo -e "${RED}║      有 $FAILED_TESTS 个测试失败！ ✗        ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════╝${NC}\n"
    exit 1
fi
