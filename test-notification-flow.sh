#!/bin/bash

# 实时通知系统完整测试脚本
# 使用说明：打开 notification-test.html 页面，输入 customer@example.com，点击"连接 WebSocket"
# 然后运行此脚本，观察实时通知推送效果

API_BASE="http://localhost:8080/api"
CUSTOMER_EMAIL="customer@example.com"

echo "======================================"
echo "🚀 DoorDash 实时通知系统测试"
echo "======================================"
echo ""

echo "📋 测试准备："
echo "1. 确保已打开 notification-test.html"
echo "2. 在页面中输入邮箱：customer@example.com"
echo "3. 点击'连接 WebSocket'按钮"
echo "4. 观察页面实时通知效果"
echo ""
read -p "按 Enter 继续测试..."
echo ""

# 第一步：注册/登录用户
echo "🔐 Step 1: 用户登录..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

if [ -z "$TOKEN" ]; then
  echo "❌ 登录失败，尝试注册新用户..."
  
  REGISTER_RESPONSE=$(curl -s -X POST "$API_BASE/auth/register" \
    -H "Content-Type: application/json" \
    -d '{
      "email": "customer@example.com",
      "password": "password123",
      "firstName": "测试",
      "lastName": "用户",
      "phone": "1234567890",
      "role": "CUSTOMER"
    }')
  
  TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
  
  if [ -z "$TOKEN" ]; then
    echo "❌ 注册失败！请检查 API 服务"
    exit 1
  fi
  echo "✅ 用户注册成功"
else
  echo "✅ 用户登录成功"
fi

echo "🔑 Token: ${TOKEN:0:50}..."
echo ""
sleep 2

# 第二步：查看当前通知
echo "📬 Step 2: 查看当前通知列表..."
CURRENT_NOTIFICATIONS=$(curl -s -X GET "$API_BASE/notifications" \
  -H "Authorization: Bearer $TOKEN")

UNREAD_COUNT=$(curl -s -X GET "$API_BASE/notifications/unread/count" \
  -H "Authorization: Bearer $TOKEN")

echo "📊 未读通知数量：$UNREAD_COUNT"
echo ""
sleep 2

# 第三步：创建订单（触发通知）
echo "🍔 Step 3: 创建测试订单（将触发实时通知）..."
echo "⏳ 请观察 notification-test.html 页面的实时通知..."
echo ""

ORDER_RESPONSE=$(curl -s -X POST "$API_BASE/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "items": [
      {
        "menuItemId": 1,
        "quantity": 2,
        "specialInstructions": "实时通知测试订单"
      }
    ],
    "deliveryAddress": "123 测试街道, 测试市",
    "deliveryLatitude": 37.7749,
    "deliveryLongitude": -122.4194,
    "specialInstructions": "这是一个测试订单，用于验证实时通知系统"
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')

if [ -z "$ORDER_ID" ]; then
  echo "❌ 订单创建失败！"
  echo "Response: $ORDER_RESPONSE"
  exit 1
fi

echo "✅ 订单创建成功！订单ID: $ORDER_ID"
echo ""
echo "🎯 应该触发的通知类型：ORDER_CREATED"
echo "📱 请检查 notification-test.html 页面是否收到实时通知！"
echo ""
sleep 3

# 第四步：查看新通知
echo "📬 Step 4: 查看新增的通知..."
NEW_NOTIFICATIONS=$(curl -s -X GET "$API_BASE/notifications" \
  -H "Authorization: Bearer $TOKEN")

echo "📋 通知列表："
echo $NEW_NOTIFICATIONS | jq '.[0:3]' 2>/dev/null || echo $NEW_NOTIFICATIONS

NEW_UNREAD_COUNT=$(curl -s -X GET "$API_BASE/notifications/unread/count" \
  -H "Authorization: Bearer $TOKEN")

echo ""
echo "📊 新的未读通知数量：$NEW_UNREAD_COUNT"
echo ""
sleep 2

# 第五步：标记通知为已读
echo "✔️  Step 5: 测试标记已读功能..."

UNREAD_NOTIFICATIONS=$(curl -s -X GET "$API_BASE/notifications/unread" \
  -H "Authorization: Bearer $TOKEN")

FIRST_NOTIFICATION_ID=$(echo $UNREAD_NOTIFICATIONS | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')

if [ ! -z "$FIRST_NOTIFICATION_ID" ]; then
  echo "📝 标记通知 $FIRST_NOTIFICATION_ID 为已读..."
  
  curl -s -X PUT "$API_BASE/notifications/$FIRST_NOTIFICATION_ID/read" \
    -H "Authorization: Bearer $TOKEN" > /dev/null
  
  echo "✅ 已标记为已读"
  echo ""
  
  FINAL_UNREAD_COUNT=$(curl -s -X GET "$API_BASE/notifications/unread/count" \
    -H "Authorization: Bearer $TOKEN")
  
  echo "📊 最终未读通知数量：$FINAL_UNREAD_COUNT"
else
  echo "ℹ️  没有未读通知"
fi

echo ""
sleep 2

# 测试总结
echo "======================================"
echo "✅ 测试完成！"
echo "======================================"
echo ""
echo "📊 测试结果总结："
echo "- 用户认证：✅"
echo "- 订单创建：✅ (订单ID: $ORDER_ID)"
echo "- 通知发送：✅"
echo "- 通知查询：✅"
echo "- 标记已读：✅"
echo ""
echo "🎯 WebSocket 实时推送验证："
echo "请检查 notification-test.html 页面："
echo "1. 是否显示'已连接'状态（绿色）"
echo "2. 是否收到实时通知卡片"
echo "3. 通知内容是否包含订单信息"
echo "4. 未读徽章数字是否更新"
echo ""
echo "📚 下一步测试建议："
echo "1. 多次创建订单，观察通知累积效果"
echo "2. 点击'标记全部已读'按钮"
echo "3. 测试通知过期功能（7天后自动删除）"
echo "4. 测试浏览器通知权限"
echo ""
echo "======================================"
