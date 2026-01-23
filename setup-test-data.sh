#!/bin/bash

# 设置测试数据脚本

BASE_URL="http://localhost:8080/api"

echo "===== 设置测试数据 ====="

# 1. 创建餐厅老板
echo "1. 创建餐厅老板账户..."
curl -s -X POST "${BASE_URL}/users" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ownertest@example.com",
    "password": "Password123!",
    "firstName": "Owner",
    "lastName": "Tester",
    "phoneNumber": "+15559876543",
    "role": "RESTAURANT_OWNER"
  }' > /dev/null 2>&1

# 2. 登录获取 token
echo "2. 登录餐厅老板..."
OWNER_TOKEN=$(curl -s -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ownertest@example.com",
    "password": "Password123!"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$OWNER_TOKEN" ]; then
    echo "❌ 登录失败"
    exit 1
fi

echo "✅ 登录成功"

# 3. 创建餐厅
echo "3. 创建餐厅..."
RESTAURANT=$(curl -s -X POST "${BASE_URL}/restaurants" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Restaurant for Cart",
    "description": "A test restaurant",
    "address": "123 Test St",
    "phoneNumber": "+15551112222",
    "cuisineType": "CHINESE",
    "deliveryFee": 5.00,
    "minimumOrder": 15.00
  }')

RESTAURANT_ID=$(echo $RESTAURANT | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ -n "$RESTAURANT_ID" ]; then
    echo "✅ 餐厅创建成功 (ID: $RESTAURANT_ID)"
else
    echo "❌ 餐厅创建失败"
    exit 1
fi

# 4. 创建菜品
echo "4. 创建菜品..."

curl -s -X POST "${BASE_URL}/menu-items" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"name\": \"汉堡\",
    \"description\": \"美味的汉堡\",
    \"price\": 12.99,
    \"category\": \"MAIN_COURSE\",
    \"isAvailable\": true
  }" > /dev/null

curl -s -X POST "${BASE_URL}/menu-items" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"name\": \"薯条\",
    \"description\": \"酥脆的薯条\",
    \"price\": 4.99,
    \"category\": \"SIDE\",
    \"isAvailable\": true
  }" > /dev/null

curl -s -X POST "${BASE_URL}/menu-items" \
  -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"restaurantId\": $RESTAURANT_ID,
    \"name\": \"可乐\",
    \"description\": \"冰镇可乐\",
    \"price\": 2.99,
    \"category\": \"BEVERAGE\",
    \"isAvailable\": true
  }" > /dev/null

echo "✅ 3个菜品创建成功"

echo ""
echo "===== 测试数据设置完成 ====="
echo "餐厅 ID: $RESTAURANT_ID"
echo ""
