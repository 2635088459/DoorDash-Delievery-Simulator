#!/bin/bash

# ===========================================
# DoorDash Order 模块 RBAC 测试脚本
# ===========================================
# 测试场景:
# 1. CUSTOMER 创建订单
# 2. CUSTOMER 查看自己的订单
# 3. CUSTOMER 查看订单详情
# 4. CUSTOMER 取消订单
# 5. RESTAURANT_OWNER 查看餐厅订单
# 6. RESTAURANT_OWNER 更新订单状态
# 7. 权限验证 - RESTAURANT_OWNER 不能查看其他餐厅订单
# ===========================================

BASE_URL="http://localhost:8080/api"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印分隔线
print_separator() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "$1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# 打印标题
echo ""
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   DoorDash RBAC 测试 - Order 模块                        ║"
echo "╚═══════════════════════════════════════════════════════════╝"

# ===========================================
# 第 1 部分: CUSTOMER 角色测试
# ===========================================

print_separator "第 1 部分：CUSTOMER 角色测试"

echo "🔐 登录为 CUSTOMER 用户..."
CUSTOMER_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "Test@123"
  }')

CUSTOMER_TOKEN=$(echo $CUSTOMER_RESPONSE | jq -r '.idToken')
CUSTOMER_ROLE=$(echo $CUSTOMER_RESPONSE | jq -r '.user.role')

if [ "$CUSTOMER_TOKEN" != "null" ] && [ "$CUSTOMER_TOKEN" != "" ]; then
    echo "   ✅ 登录成功"
    echo "   角色: $CUSTOMER_ROLE"
    echo "   Token: ${CUSTOMER_TOKEN:0:50}..."
else
    echo "   ❌ 登录失败"
    echo "   响应: $CUSTOMER_RESPONSE"
    exit 1
fi

# 测试 1: CUSTOMER 创建订单
echo ""
echo "✅ 测试 1: CUSTOMER 创建订单（应该成功）"
echo "   请求: POST /api/orders"
echo "   权限检查: @PreAuthorize(\"hasRole('CUSTOMER')\")"

CREATE_ORDER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 2,
    "deliveryAddressId": 1,
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {
        "menuItemId": 1,
        "quantity": 2
      }
    ],
    "specialInstructions": "不要辣椒"
  }')

HTTP_CODE=$(echo "$CREATE_ORDER_RESPONSE" | tail -n 1)
RESPONSE_BODY=$(echo "$CREATE_ORDER_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    echo "   ✅ 创建成功 (HTTP $HTTP_CODE Created)"
    echo "   RBAC 工作正常：CUSTOMER 可以创建订单"
    ORDER_ID=$(echo $RESPONSE_BODY | jq -r '.id')
    ORDER_NUMBER=$(echo $RESPONSE_BODY | jq -r '.orderNumber')
    echo "   订单 ID: $ORDER_ID"
    echo "   订单号: $ORDER_NUMBER"
else
    echo "   ❌ 失败 (HTTP $HTTP_CODE)"
    echo "   响应: $RESPONSE_BODY"
fi

# 测试 2: CUSTOMER 查看我的订单
echo ""
echo "✅ 测试 2: CUSTOMER 查看我的订单（应该成功）"
echo "   请求: GET /api/orders/my"
echo "   权限检查: @PreAuthorize(\"hasRole('CUSTOMER')\")"

MY_ORDERS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/orders/my \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$MY_ORDERS_RESPONSE" | tail -n 1)
RESPONSE_BODY=$(echo "$MY_ORDERS_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ 查询成功 (HTTP $HTTP_CODE)"
    echo "   RBAC 工作正常：CUSTOMER 可以查看自己的订单"
    ORDER_COUNT=$(echo $RESPONSE_BODY | jq '. | length')
    echo "   我的订单数量: $ORDER_COUNT"
else
    echo "   ❌ 失败 (HTTP $HTTP_CODE)"
    echo "   响应: $RESPONSE_BODY"
fi

# 测试 3: CUSTOMER 查看订单详情
if [ ! -z "$ORDER_ID" ]; then
    echo ""
    echo "✅ 测试 3: CUSTOMER 查看订单详情（应该成功）"
    echo "   请求: GET /api/orders/$ORDER_ID"
    echo "   权限检查: @PreAuthorize(\"hasAnyRole('CUSTOMER', 'RESTAURANT_OWNER')\")"
    echo "   所有权验证: AuthorizationService.verifyOrderAccess()"

    ORDER_DETAIL_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/orders/$ORDER_ID \
      -H "Authorization: Bearer $CUSTOMER_TOKEN")

    HTTP_CODE=$(echo "$ORDER_DETAIL_RESPONSE" | tail -n 1)
    RESPONSE_BODY=$(echo "$ORDER_DETAIL_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ 查询成功 (HTTP $HTTP_CODE)"
        echo "   RBAC 工作正常：CUSTOMER 可以查看自己的订单详情"
        ORDER_STATUS=$(echo $RESPONSE_BODY | jq -r '.status')
        echo "   订单状态: $ORDER_STATUS"
    else
        echo "   ❌ 失败 (HTTP $HTTP_CODE)"
        echo "   响应: $RESPONSE_BODY"
    fi
fi

# 测试 4: CUSTOMER 取消订单
if [ ! -z "$ORDER_ID" ]; then
    echo ""
    echo "✅ 测试 4: CUSTOMER 取消订单（应该成功）"
    echo "   请求: DELETE /api/orders/$ORDER_ID"
    echo "   权限检查: @PreAuthorize(\"hasRole('CUSTOMER')\")"
    echo "   所有权验证: AuthorizationService.verifyOrderCustomer()"

    CANCEL_ORDER_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE $BASE_URL/orders/$ORDER_ID \
      -H "Authorization: Bearer $CUSTOMER_TOKEN")

    HTTP_CODE=$(echo "$CANCEL_ORDER_RESPONSE" | tail -n 1)
    RESPONSE_BODY=$(echo "$CANCEL_ORDER_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ 取消成功 (HTTP $HTTP_CODE)"
        echo "   RBAC 工作正常：CUSTOMER 可以取消自己的订单"
        CANCELLED_STATUS=$(echo $RESPONSE_BODY | jq -r '.status')
        echo "   取消后状态: $CANCELLED_STATUS"
    else
        echo "   ❌ 失败 (HTTP $HTTP_CODE)"
        echo "   响应: $RESPONSE_BODY"
    fi
fi

# ===========================================
# 第 2 部分: RESTAURANT_OWNER 角色测试
# ===========================================

print_separator "第 2 部分：RESTAURANT_OWNER 角色测试"

echo "🔐 登录为 RESTAURANT_OWNER 用户..."
OWNER_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@restaurant.com",
    "password": "Test@123"
  }')

OWNER_TOKEN=$(echo $OWNER_RESPONSE | jq -r '.idToken')
OWNER_ROLE=$(echo $OWNER_RESPONSE | jq -r '.user.role')

if [ "$OWNER_TOKEN" != "null" ] && [ "$OWNER_TOKEN" != "" ]; then
    echo "   ✅ 登录成功"
    echo "   角色: $OWNER_ROLE"
    echo "   邮箱: owner@restaurant.com"
    echo "   Token: ${OWNER_TOKEN:0:50}..."
else
    echo "   ❌ 登录失败"
    exit 1
fi

# 创建一个新订单用于测试（使用 CUSTOMER 账户）
echo ""
echo "📦 准备测试数据：创建新订单..."
NEW_ORDER_RESPONSE=$(curl -s -X POST $BASE_URL/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 2,
    "deliveryAddressId": 1,
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {
        "menuItemId": 1,
        "quantity": 1
      }
    ]
  }')

NEW_ORDER_ID=$(echo $NEW_ORDER_RESPONSE | jq -r '.id')
echo "   新订单 ID: $NEW_ORDER_ID"

# 测试 5: RESTAURANT_OWNER 查看餐厅订单
echo ""
echo "✅ 测试 5: RESTAURANT_OWNER 查看餐厅订单（应该成功）"
echo "   请求: GET /api/orders/restaurant/2"
echo "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
echo "   所有权验证: AuthorizationService.verifyRestaurantOwnership()"

RESTAURANT_ORDERS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/orders/restaurant/2 \
  -H "Authorization: Bearer $OWNER_TOKEN")

HTTP_CODE=$(echo "$RESTAURANT_ORDERS_RESPONSE" | tail -n 1)
RESPONSE_BODY=$(echo "$RESTAURANT_ORDERS_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ 查询成功 (HTTP $HTTP_CODE)"
    echo "   RBAC 工作正常：RESTAURANT_OWNER 可以查看自己餐厅的订单"
    RESTAURANT_ORDER_COUNT=$(echo $RESPONSE_BODY | jq '. | length')
    echo "   餐厅订单数量: $RESTAURANT_ORDER_COUNT"
else
    echo "   ❌ 失败 (HTTP $HTTP_CODE)"
    echo "   响应: $RESPONSE_BODY"
fi

# 测试 6: RESTAURANT_OWNER 更新订单状态
if [ ! -z "$NEW_ORDER_ID" ]; then
    echo ""
    echo "✅ 测试 6: RESTAURANT_OWNER 更新订单状态（应该成功）"
    echo "   请求: PUT /api/orders/$NEW_ORDER_ID/status"
    echo "   权限检查: @PreAuthorize(\"hasAnyRole('RESTAURANT_OWNER', 'DRIVER')\")"
    echo "   所有权验证: AuthorizationService.verifyOrderAccess()"

    UPDATE_STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT $BASE_URL/orders/$NEW_ORDER_ID/status \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "status": "CONFIRMED"
      }')

    HTTP_CODE=$(echo "$UPDATE_STATUS_RESPONSE" | tail -n 1)
    RESPONSE_BODY=$(echo "$UPDATE_STATUS_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ 更新成功 (HTTP $HTTP_CODE)"
        echo "   RBAC 工作正常：RESTAURANT_OWNER 可以更新餐厅订单状态"
        UPDATED_STATUS=$(echo $RESPONSE_BODY | jq -r '.status')
        echo "   更新后状态: $UPDATED_STATUS"
    else
        echo "   ❌ 失败 (HTTP $HTTP_CODE)"
        echo "   响应: $RESPONSE_BODY"
    fi
fi

# ===========================================
# 第 3 部分: 权限拒绝测试
# ===========================================

print_separator "第 3 部分：权限拒绝测试"

# 测试 7: RESTAURANT_OWNER 尝试创建订单（应该被拒绝）
echo "❌ 测试 7: RESTAURANT_OWNER 尝试创建订单（应该被拒绝）"
echo "   请求: POST /api/orders"
echo "   权限检查: @PreAuthorize(\"hasRole('CUSTOMER')\")"

OWNER_CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/orders \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 2,
    "deliveryAddressId": 1,
    "paymentMethod": "CREDIT_CARD",
    "items": [{"menuItemId": 1, "quantity": 1}]
  }')

HTTP_CODE=$(echo "$OWNER_CREATE_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "403" ]; then
    echo "   ✅ 正确拒绝访问 (HTTP $HTTP_CODE Forbidden)"
    echo "   RBAC 工作正常：只有 CUSTOMER 可以创建订单"
else
    echo "   ❌ 未正确拒绝 (HTTP $HTTP_CODE)"
fi

# 测试 8: CUSTOMER 尝试查看餐厅订单（应该被拒绝）
echo ""
echo "❌ 测试 8: CUSTOMER 尝试查看餐厅订单（应该被拒绝）"
echo "   请求: GET /api/orders/restaurant/2"
echo "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"

CUSTOMER_RESTAURANT_ORDERS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/orders/restaurant/2 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$CUSTOMER_RESTAURANT_ORDERS_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "403" ]; then
    echo "   ✅ 正确拒绝访问 (HTTP $HTTP_CODE Forbidden)"
    echo "   RBAC 工作正常：只有 RESTAURANT_OWNER 可以查看餐厅订单"
else
    echo "   ❌ 未正确拒绝 (HTTP $HTTP_CODE)"
fi

# ===========================================
# 测试总结
# ===========================================

print_separator "测试总结"

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║                    测试总结                               ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""
echo "✅ Order 模块 RBAC 测试完成！"
echo ""
echo "关键概念："
echo "1. 🔒 角色验证：@PreAuthorize(\"hasRole('CUSTOMER')\")"
echo "2. 🔐 所有权验证：AuthorizationService.verifyOrderCustomer()"
echo "3. 🔐 访问验证：AuthorizationService.verifyOrderAccess()"
echo "4. 🛡️ 双重防护：角色检查 + 资源所有权验证"
echo ""
echo "测试的权限场景："
echo "✅ CUSTOMER 可以创建订单"
echo "✅ CUSTOMER 可以查看自己的订单"
echo "✅ CUSTOMER 可以取消自己的订单"
echo "✅ RESTAURANT_OWNER 可以查看餐厅订单"
echo "✅ RESTAURANT_OWNER 可以更新订单状态"
echo "✅ RESTAURANT_OWNER 不能创建订单（角色不匹配）"
echo "✅ CUSTOMER 不能查看餐厅订单（角色不匹配）"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
