#!/bin/bash

# ============================================
# DoorDash 评价系统 RBAC 测试脚本
# 测试 CUSTOMER 角色的评价权限控制
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

CUSTOMER2_EMAIL="owner@restaurant.com"  # 使用另一个用户作为第二个客户
CUSTOMER2_PASSWORD="Test@123"

OWNER_EMAIL="owner@restaurant.com"
OWNER_PASSWORD="Test@123"

DRIVER_EMAIL="driver@delivery.com"
DRIVER_PASSWORD="Test@123"

echo "客户1: $CUSTOMER_EMAIL"
echo "客户2: $CUSTOMER2_EMAIL"
echo "餐厅老板: $OWNER_EMAIL"
echo "司机: $DRIVER_EMAIL"

# ============================================
# 步骤 2: 获取 JWT Token
# ============================================

print_section "步骤 2: 获取 JWT Token"

# 客户1登录
echo "客户1登录..."
CUSTOMER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"$CUSTOMER_PASSWORD\"}" \
  | jq -r '.idToken // empty')

if [ -z "$CUSTOMER_TOKEN" ]; then
    echo -e "${RED}客户1登录失败！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 客户1登录成功${NC}"

# 客户2登录
echo "客户2登录..."
CUSTOMER2_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CUSTOMER2_EMAIL\",\"password\":\"$CUSTOMER2_PASSWORD\"}" \
  | jq -r '.idToken // empty')

if [ -z "$CUSTOMER2_TOKEN" ]; then
    echo -e "${RED}客户2登录失败！${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 客户2登录成功${NC}"

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
# 步骤 3: 创建并完成测试订单
# ============================================

print_section "步骤 3: 创建并完成测试订单"

# 获取餐厅ID
RESTAURANT_ID=$(curl -s -X GET "$BASE_URL/restaurants" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  | jq -r '.[] | select(.id == 2) | .id // empty')

if [ -z "$RESTAURANT_ID" ]; then
    RESTAURANT_ID=$(curl -s -X GET "$BASE_URL/restaurants" \
      -H "Authorization: Bearer $CUSTOMER_TOKEN" \
      | jq -r '.[0].id // empty')
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
    \"specialInstructions\": \"测试评价订单\"
  }")

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')

if [ -z "$ORDER_ID" ]; then
    echo -e "${RED}创建订单失败！${NC}"
    echo "$ORDER_RESPONSE"
    exit 1
fi
echo -e "${GREEN}✓ 订单创建成功，ID: $ORDER_ID${NC}"

# 餐厅老板处理订单到 DELIVERED
echo "餐厅老板处理订单..."
curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}' > /dev/null

curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/status" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PREPARING"}' > /dev/null

curl -s -X PUT "$BASE_URL/orders/$ORDER_ID/ready-for-pickup" \
  -H "Authorization: Bearer $OWNER_TOKEN" > /dev/null

# 司机处理配送
echo "司机处理配送..."
curl -s -X POST "$BASE_URL/deliveries/accept/$ORDER_ID" \
  -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null

curl -s -X PUT "$BASE_URL/deliveries/$ORDER_ID/picked-up" \
  -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null

curl -s -X PUT "$BASE_URL/deliveries/$ORDER_ID/in-transit" \
  -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null

curl -s -X PUT "$BASE_URL/deliveries/$ORDER_ID/delivered" \
  -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null

echo -e "${GREEN}✓ 订单已完成配送，可以评价了${NC}"

# ============================================
# RBAC 测试开始
# ============================================

print_section "RBAC 测试: 评价系统权限控制"

# ============================================
# 测试 1: 未登录用户不能创建评价
# ============================================

print_test "未登录用户不能创建评价 (应返回 401或403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"foodRating\": 5,
    \"deliveryRating\": 5,
    \"comment\": \"测试评价\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    print_success "未登录用户被拒绝创建评价 (HTTP $HTTP_CODE)"
else
    print_failure "期望 401或403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 2: RESTAURANT_OWNER 不能创建评价
# ============================================

print_test "RESTAURANT_OWNER 不能创建评价 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"foodRating\": 5,
    \"deliveryRating\": 5,
    \"comment\": \"测试评价\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "RESTAURANT_OWNER 被拒绝创建评价"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 3: DRIVER 不能创建评价
# ============================================

print_test "DRIVER 不能创建评价 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
  -H "Authorization: Bearer $DRIVER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"foodRating\": 5,
    \"deliveryRating\": 5,
    \"comment\": \"测试评价\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "DRIVER 被拒绝创建评价"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 4: CUSTOMER 可以创建评价
# ============================================

print_test "CUSTOMER 可以创建评价 (应返回 201)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"foodRating\": 5,
    \"deliveryRating\": 4,
    \"comment\": \"非常好吃，配送也很快！\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "201" ]; then
    REVIEW_ID=$(echo "$BODY" | jq -r '.id // empty')
    OVERALL_RATING=$(echo "$BODY" | jq -r '.overallRating // empty')
    print_success "CUSTOMER 成功创建评价，ID: $REVIEW_ID，总分: $OVERALL_RATING"
else
    print_failure "期望 201，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 5: CUSTOMER 不能重复评价同一订单
# ============================================

print_test "CUSTOMER 不能重复评价同一订单 (应返回 400/500)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": $ORDER_ID,
    \"foodRating\": 3,
    \"deliveryRating\": 3,
    \"comment\": \"重复评价\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "500" ]; then
    print_success "CUSTOMER 被阻止重复评价 (HTTP $HTTP_CODE)"
else
    print_failure "期望 400或500，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 6: 所有人都可以查看评价详情
# ============================================

print_test "所有人都可以查看评价详情 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/reviews/$REVIEW_ID")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    COMMENT=$(echo "$BODY" | jq -r '.comment // empty')
    print_success "成功查看评价详情: $COMMENT"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 7: 所有人都可以查看餐厅评价列表
# ============================================

print_test "所有人都可以查看餐厅评价列表 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/reviews/restaurant/$RESTAURANT_ID")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    REVIEW_COUNT=$(echo "$BODY" | jq '. | length')
    print_success "成功查看餐厅评价列表，数量: $REVIEW_COUNT"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 8: 所有人都可以查看餐厅评分统计
# ============================================

print_test "所有人都可以查看餐厅评分统计 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/reviews/restaurant/$RESTAURANT_ID/rating")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    AVG_RATING=$(echo "$BODY" | jq -r '.averageRating // empty')
    TOTAL_REVIEWS=$(echo "$BODY" | jq -r '.totalReviews // empty')
    print_success "成功查看餐厅评分统计，平均分: $AVG_RATING，总评价数: $TOTAL_REVIEWS"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 9: CUSTOMER 可以查看自己的评价
# ============================================

print_test "CUSTOMER 可以查看自己的评价 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/reviews/my" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    MY_REVIEWS=$(echo "$BODY" | jq '. | length')
    print_success "CUSTOMER 成功查看自己的评价，数量: $MY_REVIEWS"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 10: CUSTOMER 可以更新自己的评价
# ============================================

print_test "CUSTOMER 可以更新自己的评价 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"foodRating\": 4,
    \"deliveryRating\": 5,
    \"comment\": \"更新后的评价：食物还是很好，配送更快了！\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    NEW_RATING=$(echo "$BODY" | jq -r '.overallRating // empty')
    print_success "CUSTOMER 成功更新评价，新总分: $NEW_RATING"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
    echo "$BODY"
fi

# ============================================
# 测试 11: 其他 CUSTOMER 不能更新别人的评价
# ============================================

print_test "其他 CUSTOMER 不能更新别人的评价 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $CUSTOMER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"foodRating\": 1,
    \"comment\": \"恶意修改\"
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "其他 CUSTOMER 被阻止更新别人的评价"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 12: 所有人都可以通过订单ID查看评价
# ============================================

print_test "所有人都可以通过订单ID查看评价 (应返回 200)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/reviews/order/$ORDER_ID")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" == "200" ]; then
    ORDER_NUMBER=$(echo "$BODY" | jq -r '.orderNumber // empty')
    print_success "成功通过订单ID查看评价，订单号: $ORDER_NUMBER"
else
    print_failure "期望 200，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 13: RESTAURANT_OWNER 不能删除评价
# ============================================

print_test "RESTAURANT_OWNER 不能删除评价 (应返回 403)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "403" ]; then
    print_success "RESTAURANT_OWNER 被拒绝删除评价"
else
    print_failure "期望 403，实际返回 $HTTP_CODE"
fi

# ============================================
# 测试 14: 评价评分验证 (无效评分应返回 400)
# ============================================

print_test "评分验证：评分超出范围应返回 400"

# 创建新订单用于测试
ORDER2_RESPONSE=$(curl -s -X POST "$BASE_URL/orders" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"deliveryAddressId\": 1,
    \"paymentMethod\": \"CREDIT_CARD\",
    \"items\": [{\"menuItemId\": $MENU_ITEM_ID, \"quantity\": 1}]
  }")

ORDER2_ID=$(echo "$ORDER2_RESPONSE" | jq -r '.id // empty')

if [ -n "$ORDER2_ID" ]; then
    # 快速完成订单
    curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "CONFIRMED"}' > /dev/null
    curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/status" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"status": "PREPARING"}' > /dev/null
    curl -s -X PUT "$BASE_URL/orders/$ORDER2_ID/ready-for-pickup" \
      -H "Authorization: Bearer $OWNER_TOKEN" > /dev/null
    curl -s -X POST "$BASE_URL/deliveries/accept/$ORDER2_ID" \
      -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null
    curl -s -X PUT "$BASE_URL/deliveries/$ORDER2_ID/picked-up" \
      -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null
    curl -s -X PUT "$BASE_URL/deliveries/$ORDER2_ID/in-transit" \
      -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null
    curl -s -X PUT "$BASE_URL/deliveries/$ORDER2_ID/delivered" \
      -H "Authorization: Bearer $DRIVER_TOKEN" > /dev/null
    
    # 尝试创建无效评分的评价
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/reviews" \
      -H "Authorization: Bearer $CUSTOMER_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"orderId\": $ORDER2_ID,
        \"foodRating\": 6,
        \"deliveryRating\": 0,
        \"comment\": \"无效评分\"
      }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    
    if [ "$HTTP_CODE" == "400" ]; then
        print_success "无效评分被正确拒绝 (HTTP 400)"
    else
        print_failure "期望 400，实际返回 $HTTP_CODE"
    fi
fi

# ============================================
# 测试 15: CUSTOMER 可以删除自己的评价
# ============================================

print_test "CUSTOMER 可以删除自己的评价 (应返回 204)"

RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/reviews/$REVIEW_ID" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "204" ]; then
    print_success "CUSTOMER 成功删除自己的评价"
else
    print_failure "期望 204，实际返回 $HTTP_CODE"
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
    echo -e "${GREEN}║   所有评价系统 RBAC 测试通过！ ✓   ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════╝${NC}\n"
    exit 0
else
    echo -e "\n${RED}╔══════════════════════════════════════╗${NC}"
    echo -e "${RED}║      有 $FAILED_TESTS 个测试失败！ ✗        ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════╝${NC}\n"
    exit 1
fi
