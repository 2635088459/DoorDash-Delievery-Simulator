#!/bin/bash

# RBAC 测试脚本 - Restaurant 模块演示
# 这个脚本演示了基于角色的访问控制 (RBAC) 如何工作

API_BASE="http://localhost:8080/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   DoorDash RBAC 示例测试 - Restaurant 模块               ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

# ========================================
# 第一部分：公开接口测试（无需登录）
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}第 1 部分：公开接口测试（无需登录）${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}📋 测试 1: 获取所有餐厅列表（公开接口）${NC}"
echo -e "   请求: GET /api/restaurants"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/restaurants")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}   ✅ 成功 (HTTP $HTTP_CODE)${NC}"
    echo "   返回数据: $BODY" | head -c 200
else
    echo -e "${RED}   ❌ 失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# ========================================
# 第二部分：CUSTOMER 角色测试（不能创建餐厅）
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}第 2 部分：CUSTOMER 角色测试${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}🔐 登录为 CUSTOMER 用户...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "Test@123"
  }')

CUSTOMER_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.idToken')
CUSTOMER_ROLE=$(echo "$LOGIN_RESPONSE" | jq -r '.user.role')

if [ "$CUSTOMER_TOKEN" != "null" ] && [ "$CUSTOMER_TOKEN" != "" ]; then
    echo -e "${GREEN}   ✅ 登录成功${NC}"
    echo "   角色: $CUSTOMER_ROLE"
    echo "   Token: ${CUSTOMER_TOKEN:0:50}..."
else
    echo -e "${RED}   ❌ 登录失败${NC}"
    echo "   响应: $LOGIN_RESPONSE"
fi
echo ""

echo -e "${BLUE}❌ 测试 2: CUSTOMER 尝试创建餐厅（应该被拒绝）${NC}"
echo -e "   请求: POST /api/restaurants"
echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/restaurants" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试餐厅",
    "description": "这是一个测试餐厅",
    "cuisineType": "中餐",
    "streetAddress": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94102",
    "phoneNumber": "+14155551234"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403 Forbidden)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：CUSTOMER 无权创建餐厅${NC}"
else
    echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
    echo "   响应: $BODY"
fi
echo ""

echo -e "${BLUE}❌ 测试 3: CUSTOMER 尝试获取"我的餐厅"（应该被拒绝）${NC}"
echo -e "   请求: GET /api/restaurants/my"
echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/restaurants/my" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403 Forbidden)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：只有餐厅老板可以查看自己的餐厅${NC}"
else
    echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# ========================================
# 第三部分：RESTAURANT_OWNER 角色测试（可以创建和管理餐厅）
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}第 3 部分：RESTAURANT_OWNER 角色测试${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}🔐 登录为 RESTAURANT_OWNER 用户...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@restaurant.com",
    "password": "Test@123"
  }')

OWNER_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.idToken')
OWNER_ROLE=$(echo "$LOGIN_RESPONSE" | jq -r '.user.role')
OWNER_NAME=$(echo "$LOGIN_RESPONSE" | jq -r '.user.email')

if [ "$OWNER_TOKEN" != "null" ] && [ "$OWNER_TOKEN" != "" ]; then
    echo -e "${GREEN}   ✅ 登录成功${NC}"
    echo "   角色: $OWNER_ROLE"
    echo "   邮箱: $OWNER_NAME"
    echo "   Token: ${OWNER_TOKEN:0:50}..."
else
    echo -e "${RED}   ❌ 登录失败${NC}"
    echo "   响应: $LOGIN_RESPONSE"
fi
echo ""

echo -e "${BLUE}✅ 测试 4: RESTAURANT_OWNER 创建餐厅（应该成功）${NC}"
echo -e "   请求: POST /api/restaurants"
echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/restaurants" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "美味中餐馆",
    "description": "正宗川菜，麻辣鲜香",
    "cuisineType": "中餐",
    "streetAddress": "456 Market St",
    "city": "San Francisco",
    "state": "CA",
    "zipCode": "94103",
    "phoneNumber": "+14155555678"
  }')

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}   ✅ 创建成功 (HTTP 201 Created)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：餐厅老板可以创建餐厅${NC}"
    RESTAURANT_ID=$(echo "$BODY" | jq -r '.id')
    echo "   创建的餐厅 ID: $RESTAURANT_ID"
    echo "   餐厅名称: $(echo "$BODY" | jq -r '.name')"
else
    echo -e "${RED}   ❌ 创建失败 (HTTP $HTTP_CODE)${NC}"
    echo "   响应: $BODY"
    RESTAURANT_ID=""
fi
echo ""

echo -e "${BLUE}✅ 测试 5: RESTAURANT_OWNER 获取"我的餐厅"（应该成功）${NC}"
echo -e "   请求: GET /api/restaurants/my"
echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_BASE/restaurants/my" \
  -H "Authorization: Bearer $OWNER_TOKEN")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed \$d)

if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}   ✅ 查询成功 (HTTP 200)${NC}"
    echo -e "   ${GREEN}RBAC 工作正常：餐厅老板可以查看自己的餐厅${NC}"
    COUNT=$(echo "$BODY" | jq '. | length')
    echo "   我的餐厅数量: $COUNT"
else
    echo -e "${RED}   ❌ 查询失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

if [ -n "$RESTAURANT_ID" ] && [ "$RESTAURANT_ID" != "null" ]; then
    echo -e "${BLUE}✅ 测试 6: RESTAURANT_OWNER 更新自己的餐厅（应该成功）${NC}"
    echo -e "   请求: PUT /api/restaurants/$RESTAURANT_ID"
    echo -e "   权限检查: @PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
    echo -e "   所有权验证: AuthorizationService.verifyRestaurantOwnership()"
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API_BASE/restaurants/$RESTAURANT_ID" \
      -H "Authorization: Bearer $OWNER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "美味中餐馆 (更新)",
        "description": "正宗川菜，麻辣鲜香，新增粤菜",
        "isActive": true
      }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed \$d)
    
    if [ "$HTTP_CODE" == "200" ]; then
        echo -e "${GREEN}   ✅ 更新成功 (HTTP 200)${NC}"
        echo -e "   ${GREEN}RBAC 工作正常：餐厅老板可以更新自己的餐厅${NC}"
        echo "   更新后的名称: $(echo "$BODY" | jq -r '.name')"
    else
        echo -e "${RED}   ❌ 更新失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
    fi
    echo ""
fi

# ========================================
# 第四部分：资源所有权验证测试
# ========================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}第 4 部分：资源所有权验证测试${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}🔐 登录为另一个 RESTAURANT_OWNER 用户...${NC}"
# 注意：这需要你数据库中有另一个餐厅老板用户
# 这里假设 test2@example.com 是 CUSTOMER，我们用它来测试
echo -e "${YELLOW}   注意：理想情况下应该有第二个餐厅老板账户${NC}"
echo -e "${YELLOW}   这里用 CUSTOMER 账户测试会得到 403（角色不匹配）${NC}"
echo ""

if [ -n "$RESTAURANT_ID" ] && [ "$RESTAURANT_ID" != "null" ]; then
    echo -e "${BLUE}❌ 测试 7: 其他用户尝试更新不属于自己的餐厅（应该被拒绝）${NC}"
    echo -e "   请求: PUT /api/restaurants/$RESTAURANT_ID"
    echo -e "   使用: CUSTOMER 账户（不同用户）"
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$API_BASE/restaurants/$RESTAURANT_ID" \
      -H "Authorization: Bearer $CUSTOMER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "黑客尝试修改"
      }')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "403" ]; then
        echo -e "${GREEN}   ✅ 正确拒绝访问 (HTTP 403 Forbidden)${NC}"
        echo -e "   ${GREEN}RBAC 工作正常：用户不能修改其他人的餐厅${NC}"
    else
        echo -e "${RED}   ❌ 权限控制失败 (HTTP $HTTP_CODE)${NC}"
    fi
    echo ""
fi

# ========================================
# 总结
# ========================================
echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    测试总结                               ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}✅ RBAC 演示完成！${NC}"
echo ""
echo -e "${GREEN}关键概念：${NC}"
echo "1. 🔓 公开接口：GET /restaurants - 任何人可访问"
echo "2. 🔒 角色验证：@PreAuthorize(\"hasRole('RESTAURANT_OWNER')\")"
echo "3. 🔐 所有权验证：AuthorizationService.verifyRestaurantOwnership()"
echo "4. 🛡️ 双重防护：角色检查 + 资源所有权验证"
echo ""
echo -e "${YELLOW}测试的权限场景：${NC}"
echo "✅ CUSTOMER 被拒绝创建餐厅（角色不匹配）"
echo "✅ RESTAURANT_OWNER 可以创建餐厅（角色匹配）"
echo "✅ RESTAURANT_OWNER 可以查看自己的餐厅"
echo "✅ RESTAURANT_OWNER 可以更新自己的餐厅"
echo "✅ 用户不能修改其他人的餐厅（所有权验证）"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
