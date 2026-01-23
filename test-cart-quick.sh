#!/bin/bash

# 购物车系统简单测试

BASE_URL="http://localhost:8080/api"

echo "===== 购物车功能快速测试 ====="
echo ""

# 登录用户
echo "1. 登录顾客..."
CUSTOMER_TOKEN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carttest@example.com",
    "password": "Password123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$CUSTOMER_TOKEN" ]; then
    echo "❌ 顾客登录失败"
    exit 1
fi

echo "✅ 顾客登录成功"
echo "Token: ${CUSTOMER_TOKEN:0:20}..."

# 手动测试 - 需要替换这些ID
RESTAURANT_ID=2
MENU_ITEM_ID_1=100  # 需要替换为实际ID
MENU_ITEM_ID_2=101  # 需要替换为实际ID

# 直接测试添加到购物车
echo ""
echo "2. 测试添加商品到购物车..."
ADD_RESULT=$(curl -s -X POST "${BASE_URL}/cart/items/${MENU_ITEM_ID_1}" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 2,
    "specialInstructions": "不要辣椒"
  }')

echo "$ADD_RESULT" | jq '.'

# 测试获取购物车
echo ""
echo "3. 获取购物车..."
curl -s -X GET "${BASE_URL}/cart/${RESTAURANT_ID}" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}" | jq '.'

echo ""
echo "===== 测试完成 ====="
