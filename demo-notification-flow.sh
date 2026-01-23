#!/bin/bash

# 实时通知动态演示脚本
# 模拟真实订单流程，逐步推送通知

echo "======================================"
echo "🎬 实时通知系统动态演示"
echo "======================================"
echo ""
echo "📱 请确保 notification-test.html 页面已打开并连接！"
echo ""
read -p "按 Enter 开始演示..."

USER_ID=2
DB_NAME="doordash_db"

# 场景：完整订单流程
echo ""
echo "🍔 场景演示：完整订单流程"
echo "======================================"
echo ""

# 1. 订单创建
echo "⏰ [00:00] 📝 用户下单..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'ORDER_CREATED', '订单创建成功 #2024', 
'感谢您的订单！我们已收到您在「美味中餐馆」的订单，订单金额 ￥89.50', 
'NORMAL', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 3

# 2. 订单确认
echo ""
echo "⏰ [00:30] ✅ 餐厅确认订单..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'ORDER_CONFIRMED', '订单已确认', 
'「美味中餐馆」已接受您的订单，正在为您精心准备美食！', 
'NORMAL', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 3

# 3. 支付成功
echo ""
echo "⏰ [01:00] 💳 支付处理..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'PAYMENT_SUCCESS', '支付成功', 
'支付金额 ￥89.50 已确认。您的订单正在准备中，预计 30 分钟送达。', 
'NORMAL', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 4

# 4. 准备中
echo ""
echo "⏰ [05:00] 👨‍🍳 厨房准备中..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'ORDER_PREPARING', '订单准备中', 
'大厨正在为您烹饪美味佳肴，美食即将完成！', 
'NORMAL', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 4

# 5. 订单准备完成
echo ""
echo "⏰ [15:00] 🎉 订单准备完成..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'ORDER_READY', '订单准备完成', 
'您的美食已准备好，等待配送员取餐。', 
'HIGH', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 3

# 6. 配送员分配
echo ""
echo "⏰ [16:00] 🚴 分配配送员..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'DELIVERY_ASSIGNED', '配送员已分配', 
'配送员「小李」已接单，正在前往餐厅取餐。评分：⭐⭐⭐⭐⭐ 5.0', 
'HIGH', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 4

# 7. 配送员取餐
echo ""
echo "⏰ [20:00] 📦 配送员取餐..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'DELIVERY_PICKED_UP', '配送员已取餐', 
'配送员「小李」已从餐厅取餐，正在火速赶往您的地址！', 
'HIGH', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 4

# 8. 配送中
echo ""
echo "⏰ [25:00] 🚚 配送进行中..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'DELIVERY_IN_PROGRESS', '配送进行中', 
'配送员正在路上，距离您还有 2.5 公里，预计 8 分钟后送达。', 
'HIGH', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 5

# 9. 即将到达（紧急）
echo ""
echo "⏰ [33:00] ⚠️ 即将到达（紧急通知）..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'DELIVERY_NEAR', '⚠️ 配送员即将到达', 
'您的订单将在 2 分钟内送达，请准备接收！配送员正在楼下。', 
'URGENT', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 6

# 10. 订单送达
echo ""
echo "⏰ [35:00] 🎊 订单送达完成！"
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'ORDER_DELIVERED', '✅ 订单已送达', 
'您的订单已安全送达，祝您用餐愉快！感谢使用 DoorDash 🎉', 
'HIGH', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 3

# 11. 评价请求
echo ""
echo "⏰ [36:00] ⭐ 请求评价..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'DRIVER_RATING_REQUEST', '给配送员评分', 
'希望您对配送员「小李」的服务感到满意，请为 TA 评分！', 
'LOW', false, NOW())
RETURNING id, title;
" | tail -n 3

sleep 3

# 12. 促销推送
echo ""
echo "⏰ [37:00] 🎁 促销通知..."
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES ($USER_ID, 'PROMOTION_AVAILABLE', '🎉 限时优惠来啦', 
'感谢您的支持！下单即享 8 折优惠，优惠码：WELCOME20，有效期至明天！', 
'LOW', false, NOW())
RETURNING id, title;
" | tail -n 3

echo ""
echo "======================================"
echo "✅ 演示完成！"
echo "======================================"
echo ""
echo "📊 统计信息："

# 统计通知数量
STATS=$(docker exec doordash-postgres psql -U postgres -d $DB_NAME -t -c "
SELECT 
  COUNT(*) as total,
  COUNT(CASE WHEN is_read = false THEN 1 END) as unread,
  COUNT(CASE WHEN priority = 'URGENT' THEN 1 END) as urgent,
  COUNT(CASE WHEN priority = 'HIGH' THEN 1 END) as high,
  COUNT(CASE WHEN priority = 'NORMAL' THEN 1 END) as normal,
  COUNT(CASE WHEN priority = 'LOW' THEN 1 END) as low
FROM notifications 
WHERE user_id = $USER_ID;
")

echo "通知总数：$(echo $STATS | awk '{print $1}')"
echo "未读通知：$(echo $STATS | awk '{print $2}')"
echo ""
echo "优先级分布："
echo "  🔴 URGENT: $(echo $STATS | awk '{print $3}')"
echo "  🟠 HIGH:   $(echo $STATS | awk '{print $4}')"
echo "  🟢 NORMAL: $(echo $STATS | awk '{print $5}')"
echo "  🔵 LOW:    $(echo $STATS | awk '{print $6}')"

echo ""
echo "🎯 通知类型分布："
docker exec doordash-postgres psql -U postgres -d $DB_NAME -c "
SELECT 
  notification_type,
  COUNT(*) as count
FROM notifications 
WHERE user_id = $USER_ID
GROUP BY notification_type
ORDER BY count DESC;
"

echo ""
echo "======================================"
echo "💡 建议："
echo "1. 检查 notification-test.html 页面的实时更新"
echo "2. 观察不同优先级的通知样式"
echo "3. 测试标记已读功能"
echo "4. 尝试清空所有通知"
echo ""
echo "======================================"
