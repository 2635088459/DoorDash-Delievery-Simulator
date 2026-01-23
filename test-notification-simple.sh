#!/bin/bash

# 简化版通知测试 - 使用数据库直接操作

echo "======================================"
echo "🚀 实时通知系统快速测试"
echo "======================================"
echo ""

# 检查 Docker 容器
echo "📦 Step 1: 检查 Docker 容器状态..."
docker ps --format "table {{.Names}}\t{{.Status}}" | grep doordash

echo ""
echo "📊 Step 2: 检查数据库中的用户..."

# 查询现有用户
USERS=$(docker exec doordash-postgres psql -U postgres -d doordash_db_db -t -c "
SELECT email, role FROM users LIMIT 5;
")

if [ -z "$USERS" ]; then
  echo "❌ 数据库中没有用户"
  echo "📝 正在创建测试用户..."
  
  # 直接在数据库中创建用户（密码: password123 的 BCrypt hash）
  docker exec doordash-postgres psql -U postgres -d doordash_db -c "
  INSERT INTO users (email, password_hash, first_name, last_name, phone, role, created_at, updated_at)
  VALUES 
    ('customer@example.com', '\$2a\$10\$xQxPKp/9Y0V.Lj4L1GDYXeH6KvPvZxYyRGYF0Vq1NpN9j9h7X8XxG', '测试', '顾客', '1234567890', 'CUSTOMER', NOW(), NOW()),
    ('driver@example.com', '\$2a\$10\$xQxPKp/9Y0V.Lj4L1GDYXeH6KvPvZxYyRGYF0Vq1NpN9j9h7X8XxG', '测试', '司机', '1234567891', 'DRIVER', NOW(), NOW()),
    ('restaurant@example.com', '\$2a\$10\$xQxPKp/9Y0V.Lj4L1GDYXeH6KvPvZxYyRGYF0Vq1NpN9j9h7X8XxG', '测试', '餐厅', '1234567892', 'RESTAURANT_OWNER', NOW(), NOW())
  ON CONFLICT (email) DO NOTHING;
  "
  
  echo "✅ 测试用户创建成功"
else
  echo "✅ 找到现有用户："
  echo "$USERS"
fi

echo ""
echo "🍔 Step 3: 检查餐厅数据..."

RESTAURANTS=$(docker exec doordash-postgres psql -U postgres -d doordash_db -t -c "
SELECT id, name FROM restaurants LIMIT 3;
")

if [ -z "$RESTAURANTS" ]; then
  echo "❌ 没有餐厅数据"
  echo "📝 正在创建测试餐厅..."
  
  docker exec doordash-postgres psql -U postgres -d doordash_db -c "
  INSERT INTO restaurants (name, address, phone, latitude, longitude, cuisine_type, rating, is_active, created_at, updated_at)
  VALUES 
    ('测试餐厅 A', '123 美食街', '555-0001', 37.7749, -122.4194, '中餐', 4.5, true, NOW(), NOW()),
    ('测试餐厅 B', '456 餐饮路', '555-0002', 37.7849, -122.4294, '日料', 4.8, true, NOW(), NOW())
  ON CONFLICT DO NOTHING;
  "
  
  echo "✅ 测试餐厅创建成功"
else
  echo "✅ 找到现有餐厅："
  echo "$RESTAURANTS"
fi

echo ""
echo "📱 Step 4: WebSocket 连接指南"
echo "======================================"
echo ""
echo "🌐 打开 notification-test.html 页面"
echo "📧 输入邮箱：customer@example.com"
echo "🔗 点击 '连接 WebSocket' 按钮"
echo ""
echo "======================================"
echo ""

read -p "准备好了吗？按 Enter 继续创建测试通知..."

echo ""
echo "📬 Step 5: 创建测试通知..."

# 获取用户ID
USER_ID=$(docker exec doordash-postgres psql -U postgres -d doordash_db -t -c "
SELECT id FROM users WHERE email = 'customer@example.com';
" | tr -d ' ')

if [ -z "$USER_ID" ]; then
  echo "❌ 找不到用户"
  exit 1
fi

echo "👤 用户ID: $USER_ID"

# 直接在数据库中插入测试通知
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications 
  (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES 
  ($USER_ID, 'ORDER_CREATED', '订单创建成功', '您的测试订单已创建！餐厅正在准备您的美食。', 'NORMAL', false, NOW()),
  ($USER_ID, 'PAYMENT_SUCCESS', '支付成功', '支付金额 ￥45.50 已确认。', 'NORMAL', false, NOW() - INTERVAL '2 minutes'),
  ($USER_ID, 'DELIVERY_ASSIGNED', '配送员已分配', '配送员小王已接单，正在前往餐厅取餐。', 'HIGH', false, NOW() - INTERVAL '5 minutes')
RETURNING id, notification_type, title;
"

echo ""
echo "✅ 测试通知创建成功！"
echo ""

echo "📊 Step 6: 验证通知..."

NOTIFICATIONS=$(docker exec doordash-postgres psql -U postgres -d doordash_db -c "
SELECT 
  id,
  notification_type,
  title,
  SUBSTRING(message, 1, 30) || '...' as message,
  priority,
  is_read,
  TO_CHAR(created_at, 'HH24:MI:SS') as time
FROM notifications 
WHERE user_id = $USER_ID 
ORDER BY created_at DESC 
LIMIT 5;
")

echo "$NOTIFICATIONS"

UNREAD_COUNT=$(docker exec doordash-postgres psql -U postgres -d doordash_db -t -c "
SELECT COUNT(*) FROM notifications WHERE user_id = $USER_ID AND is_read = false;
" | tr -d ' ')

echo ""
echo "📮 未读通知数量：$UNREAD_COUNT"

echo ""
echo "======================================"
echo "✅ 数据库测试完成！"
echo "======================================"
echo ""
echo "🎯 下一步："
echo "1. 刷新 notification-test.html 页面"
echo "2. 使用邮箱 customer@example.com 连接"
echo "3. 点击'查询通知'按钮查看列表"
echo "4. 通过 REST API 测试实时功能"
echo ""
echo "💡 测试 REST API："
echo ""
echo "# 1. 获取通知列表"
echo "curl http://localhost:8080/api/notifications \\
  -H 'Authorization: Bearer YOUR_TOKEN'"
echo ""
echo "# 2. 获取未读数量"
echo "curl http://localhost:8080/api/notifications/unread/count \\
  -H 'Authorization: Bearer YOUR_TOKEN'"
echo ""
echo "======================================"
