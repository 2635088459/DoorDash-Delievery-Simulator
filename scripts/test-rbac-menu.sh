#!/bin/bash

# RBAC 测试脚本 - Menu 模块测试
# 这个脚本演示了菜单项的基于角色的访问控制 (RBAC)

API_BASE="http://localhost:8080/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   DoorDash RBAC 测试 - Menu 模块                         ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

# 测试结果计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# ========================================
# 准备工作：获取测试用户 Token 和创建餐厅
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}准备工作：登录用户并创建测试餐厅${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 登录 RESTAURANT_OWNER
echo -e "${BLUE}🔐 登录 RESTAURANT_OWNER 用户...${NC}"
OWNER_LOGIN=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "password": "Test@123"
  }')

OWNER_TOKEN=$(echo "$OWNER_LOGIN" | jq -r '.idToken')
if [ "$OWNER_TOKEN" != "null" ] && [ "$OWNER_TOKEN" != "" ]; then
    echo -e "${GREEN}   ✅ OWNER 登录成功${NC}"
else
    echo -e "${RED}   ❌ OWNER 登录失败${NC}"
    exit 1
fi

# 登录 CUSTOMER
echo -e "${BLUE}🔐 登录 CUSTOMER 用户...${NC}"
CUSTOMER_LOGIN=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "Test@123"
  }')

CUSTOMER_TOKEN=$(echo "$CUSTOMER_LOGIN" | jq -r '.idToken')
if [ "$CUSTOMER_TOKEN" != "null" ] && [ "$CUSTOMER_TOKEN" != "" ]; then
    echo -e "${GREEN}   ✅ CUSTOMER 登录成功${NC}"
else
    echo -e "${RED}   ❌ CUSTOMER 登录失败${NC}"
    exit 1
fi

# 登录另一个 RESTAURANT_OWNER
echo -e "${BLUE}🔐 登录第二个 RESTAURANT_OWNER 用户...${NC}"
OWNER2_LOGIN=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner2@example.com",
    "password": "Test@123"
  }')

OWNER2_TOKEN=$(echo "$OWNER2_LOGIN" | jq -r '.idToken')
if [ "$OWNER2_TOKEN" != "null" ] && [ "$OWNER2_TOKEN" != "" ]; then
    echo -e "${GREEN}   ✅ OWNER2 登录成功${NC}"
else
    echo -e "${RED}   ❌ OWNER2 登录失败${NC}"
    exit 1
fi

# 创建第一个餐厅（owner@example.com）
echo ""
echo -e "${BLUE}🏪 创建第一个测试餐厅...${NC}"
RESTAURANT_1=$(curl -s -X POST "$API_BASE/restaurants" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试中餐厅",
    "description": "用于测试菜单项的中餐厅",
    "cuisineType": "中餐",
    "streetAddress": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94102",
    "phoneNumber": "+14155551234"
  }')

RESTAURANT_1_ID=$(echo "$RESTAURANT_1" | jq -r '.id')
if [ "$RESTAURANT_1_ID" != "null" ] && [ "$RESTAURANT_1_ID" != "" ]; then
    echo -e "${GREEN}   ✅ 餐厅1创建成功，ID: $RESTAURANT_1_ID${NC}"
else
    echo -e "${RED}   ❌ 餐厅1创建失败${NC}"
    exit 1
fi

# 创建第二个餐厅（owner2@example.com）
echo -e "${BLUE}🏪 创建第二个测试餐厅...${NC}"
RESTAURANT_2=$(curl -s -X POST "$API_BASE/restaurants" \
  -H "Authorization: Bearer $OWNER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试日料店",
    "description": "用于测试所有权的日料店",
    "cuisineType": "日料",
    "streetAddress": "456 Oak Ave",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94103",
    "phoneNumber": "+14155555678"
  }')

RESTAURANT_2_ID=$(echo "$RESTAURANT_2" | jq -r '.id')
if [ "$RESTAURANT_2_ID" != "null" ] && [ "$RESTAURANT_2_ID" != "" ]; then
    echo -e "${GREEN}   ✅ 餐厅2创建成功，ID: $RESTAURANT_2_ID${NC}"
else
    echo -e "${RED}   ❌ 餐厅2创建失败${NC}"
    exit 1
fi

echo ""

# ========================================
# 测试 1: 公开接口测试
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 1: 公开接口 - 查看菜单（无需登录）${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}📋 测试 1.1: 获取餐厅可用菜单（公开接口）${NC}"
echo -e "   请求: GET /api/menu-items/restaurant/$RESTAURANT_1_ID/available"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/menu-items/restaurant/$RESTAURANT_1_ID/available")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}   ✅ 成功 (HTTP $HTTP_CODE)${NC}"
    echo -e "   ${GREEN}RBAC 正确：公开接口可以访问${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试 2: CUSTOMER 角色测试（不能创建菜单项）
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 2: CUSTOMER 角色测试${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}❌ 测试 2.1: CUSTOMER 尝试创建菜单项（应被拒绝）${NC}"
echo -e "   请求: POST /api/menu-items"
echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/menu-items" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_1_ID,
    \"name\": \"测试菜品\",
    \"description\": \"CUSTOMER不应该能创建\",
    \"category\": \"主菜\",
    \"price\": 15.99
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：CUSTOMER 无权创建菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试 3: RESTAURANT_OWNER 创建菜单项
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 3: RESTAURANT_OWNER 创建菜单项${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}✅ 测试 3.1: RESTAURANT_OWNER 创建菜单项${NC}"
echo -e "   请求: POST /api/menu-items"
echo -e "   权限验证: 第二层 @PreAuthorize + 第三层 AuthorizationService"
MENU_ITEM_1=$(curl -s -X POST "$API_BASE/menu-items" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_1_ID,
    \"name\": \"宫保鸡丁\",
    \"description\": \"经典川菜，辣而不燥\",
    \"category\": \"主菜\",
    \"price\": 18.99,
    \"isVegetarian\": false,
    \"isVegan\": false,
    \"spicyLevel\": 3
  }")

MENU_ITEM_1_ID=$(echo "$MENU_ITEM_1" | jq -r '.id')
if [ "$MENU_ITEM_1_ID" != "null" ] && [ "$MENU_ITEM_1_ID" != "" ]; then
    echo -e "${GREEN}   ✅ 菜单项创建成功，ID: $MENU_ITEM_1_ID${NC}"
    echo -e "   ${GREEN}RBAC 正确：OWNER 可以为自己的餐厅创建菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 创建失败${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# 创建更多菜单项用于测试
echo -e "${BLUE}✅ 创建更多测试菜单项...${NC}"
MENU_ITEM_2=$(curl -s -X POST "$API_BASE/menu-items" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_1_ID,
    \"name\": \"麻婆豆腐\",
    \"description\": \"素食川菜\",
    \"category\": \"主菜\",
    \"price\": 12.99,
    \"isVegetarian\": true,
    \"isVegan\": true,
    \"spicyLevel\": 4
  }")

MENU_ITEM_2_ID=$(echo "$MENU_ITEM_2" | jq -r '.id')
echo -e "${GREEN}   ✅ 素菜创建成功，ID: $MENU_ITEM_2_ID${NC}"
echo ""

# ========================================
# 测试 4: 查询菜单项
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 4: 查询菜单项${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}📋 测试 4.1: 公开查看菜单项详情${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/menu-items/$MENU_ITEM_1_ID")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    ITEM_NAME=$(echo "$BODY" | jq -r '.name')
    echo -e "${GREEN}   ✅ 成功获取菜单项: $ITEM_NAME (HTTP $HTTP_CODE)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}📋 测试 4.2: OWNER 查看自己餐厅的所有菜单项${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/menu-items/restaurant/$RESTAURANT_1_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    ITEM_COUNT=$(echo "$BODY" | jq '. | length')
    echo -e "${GREEN}   ✅ 成功获取 $ITEM_COUNT 个菜单项 (HTTP $HTTP_CODE)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试 5: RESTAURANT_OWNER 更新菜单项
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 5: RESTAURANT_OWNER 更新菜单项${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}✏️  测试 5.1: OWNER 更新自己的菜单项${NC}"
echo -e "   权限验证: 第三层 verifyMenuItemOwnership()"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API_BASE/menu-items/$MENU_ITEM_1_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 19.99,
    "description": "经典川菜，辣而不燥（特价）"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    NEW_PRICE=$(echo "$BODY" | jq -r '.price')
    echo -e "${GREEN}   ✅ 更新成功，新价格: \$$NEW_PRICE (HTTP $HTTP_CODE)${NC}"
    echo -e "   ${GREEN}RBAC 正确：OWNER 可以更新自己的菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 更新失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试 6: 所有权验证（跨餐厅）
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 6: 所有权验证（跨餐厅操作）${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}❌ 测试 6.1: OWNER2 尝试更新 OWNER1 的菜单项（应被拒绝）${NC}"
echo -e "   权限验证: verifyMenuItemOwnership() -> verifyRestaurantOwnership()"
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API_BASE/menu-items/$MENU_ITEM_1_ID" \
  -H "Authorization: Bearer $OWNER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 5.00
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：不能修改其他人的菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}❌ 测试 6.2: OWNER2 尝试删除 OWNER1 的菜单项（应被拒绝）${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$API_BASE/menu-items/$MENU_ITEM_1_ID" \
  -H "Authorization: Bearer $OWNER2_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：不能删除其他人的菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试 7: RESTAURANT_OWNER 删除菜单项
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试 7: RESTAURANT_OWNER 删除菜单项${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo -e "${BLUE}🗑️  测试 7.1: OWNER 删除自己的菜单项${NC}"
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$API_BASE/menu-items/$MENU_ITEM_2_ID" \
  -H "Authorization: Bearer $OWNER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}   ✅ 删除成功 (HTTP 204)${NC}"
    echo -e "   ${GREEN}RBAC 正确：OWNER 可以删除自己的菜单项${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}   ❌ 删除失败 (HTTP $HTTP_CODE)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
echo ""

# ========================================
# 测试总结
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}测试总结${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}总测试数: $TOTAL_TESTS${NC}"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   🎉 所有测试通过！Menu 模块 RBAC 工作正常！             ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║   ❌ 有测试失败，请检查 RBAC 配置                        ║${NC}"
    echo -e "${RED}╚═══════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
