#!/bin/bash

# 测试用户注册和登录

echo "🧪 测试用户注册..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "phone": "+1234567891",
    "role": "CUSTOMER"
  }')

echo "注册响应:"
echo "$REGISTER_RESPONSE" | jq '.'

# 提取 access token
ACCESS_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.accessToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
  echo "✅ 注册成功！"
  echo "Access Token: ${ACCESS_TOKEN:0:50}..."
else
  echo "❌ 注册失败"
  exit 1
fi

echo ""
echo "🧪 测试用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "password123"
  }')

echo "登录响应:"
echo "$LOGIN_RESPONSE" | jq '.'

LOGIN_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')

if [ "$LOGIN_TOKEN" != "null" ] && [ -n "$LOGIN_TOKEN" ]; then
  echo "✅ 登录成功！"
  echo "Access Token: ${LOGIN_TOKEN:0:50}..."
else
  echo "❌ 登录失败"
  exit 1
fi

echo ""
echo "🧪 测试获取当前用户信息..."
ME_RESPONSE=$(curl -s -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $LOGIN_TOKEN")

echo "当前用户响应:"
echo "$ME_RESPONSE" | jq '.'

USER_EMAIL=$(echo "$ME_RESPONSE" | jq -r '.email')

if [ "$USER_EMAIL" == "test2@example.com" ]; then
  echo "✅ 获取用户信息成功！"
else
  echo "❌ 获取用户信息失败"
  exit 1
fi

echo ""
echo "🎉 所有测试通过！"
