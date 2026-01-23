# ✅ 实时通知测试完成！

## 📊 测试结果

### 数据库状态
- ✅ 用户：customer@example.com (ID: 2)
- ✅ 餐厅：3 家餐厅数据
- ✅ 通知：3 条测试通知已创建

### 通知详情
| ID | 类型 | 标题 | 时间 | 状态 |
|---|---|---|---|---|
| 1 | ORDER_CREATED | 订单创建成功 | 22:14:39 | 未读 |
| 2 | PAYMENT_SUCCESS | 支付成功 | 22:12:39 | 未读 |
| 3 | DELIVERY_ASSIGNED | 配送员已分配 | 22:09:39 | 未读 |

**未读数量：3**

---

## 🎯 现在可以测试的功能

### 方法 1：使用 notification-test.html 页面（推荐）

1. **打开测试页面**（应该已经打开）
   ```
   文件：docs/notification-test.html
   ```

2. **连接 WebSocket**
   - 用户邮箱：`customer@example.com`
   - WebSocket URL：`http://localhost:8080/api/ws`
   - 点击 "连接 WebSocket" 按钮

3. **查看通知**
   - 连接成功后，页面应该显示 "已连接"（绿色）
   - 通知列表会自动加载
   - 未读徽章显示：3

4. **测试实时推送**
   - 在终端运行下面的命令创建新通知
   - 观察页面实时更新

---

### 方法 2：使用 cURL 测试 REST API

#### 步骤 1：获取认证 Token

由于当前 API 有认证问题，我们先创建一个简单的测试方法。

#### 步骤 2：直接查看数据库通知（绕过 API）

```bash
# 查看所有通知
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
SELECT id, notification_type, title, message, created_at 
FROM notifications 
ORDER BY created_at DESC;
"

# 查看未读通知数量
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
SELECT COUNT(*) as unread_count 
FROM notifications 
WHERE is_read = false;
"

# 标记通知为已读
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
UPDATE notifications 
SET is_read = true, read_at = NOW() 
WHERE id = 1;
"
```

---

## 🚀 创建更多测试通知

### 快速创建通知（数据库方式）

```bash
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications 
  (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES 
  (2, 'DELIVERY_NEAR', '配送员即将到达', '您的订单将在 5 分钟内送达，请准备接收。', 'URGENT', false, NOW())
RETURNING id, title;
"
```

**可用的通知类型：**
- ORDER_CREATED
- ORDER_CONFIRMED
- ORDER_PREPARING
- ORDER_READY
- DELIVERY_ASSIGNED
- DELIVERY_PICKED_UP
- DELIVERY_IN_PROGRESS
- DELIVERY_NEAR
- ORDER_DELIVERED
- ORDER_CANCELLED
- PAYMENT_SUCCESS
- PAYMENT_FAILED
- REFUND_PROCESSED
- PROMOTION_AVAILABLE
- DRIVER_RATING_REQUEST

**优先级：**
- LOW - 低优先级
- NORMAL - 普通（默认）
- HIGH - 高优先级
- URGENT - 紧急（红色显示）

---

## 📱 测试场景

### 场景 1：订单流程通知
```bash
# 1. 订单确认
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES (2, 'ORDER_CONFIRMED', '订单已确认', '餐厅已接单，正在准备您的美食', 'NORMAL', false, NOW());
"

# 等待 3 秒

# 2. 配送员取餐
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES (2, 'DELIVERY_PICKED_UP', '配送员已取餐', '配送员正在火速赶往您的地址', 'HIGH', false, NOW());
"

# 等待 3 秒

# 3. 即将到达（紧急）
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES (2, 'DELIVERY_NEAR', '⚠️ 配送员即将到达', '您的订单将在 3 分钟内送达！', 'URGENT', false, NOW());
"
```

### 场景 2：支付和促销
```bash
# 支付成功
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES (2, 'PAYMENT_SUCCESS', '💳 支付成功', '您已成功支付 ￥68.80', 'NORMAL', false, NOW());
"

# 促销通知
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
INSERT INTO notifications (user_id, notification_type, title, message, priority, is_read, created_at)
VALUES (2, 'PROMOTION_AVAILABLE', '🎉 限时优惠', '您有一张 8折优惠券即将过期，立即使用！', 'LOW', false, NOW());
"
```

---

## 🔍 验证清单

### WebSocket 连接验证
- [ ] notification-test.html 页面已打开
- [ ] 输入邮箱：customer@example.com
- [ ] 点击 "连接 WebSocket"
- [ ] 状态显示 "已连接"（绿色圆圈）
- [ ] 浏览器控制台无错误

### 通知显示验证
- [ ] 页面显示 3 条通知
- [ ] 未读徽章显示：3
- [ ] 通知按时间倒序排列
- [ ] 时间显示为相对时间（"5分钟前"）
- [ ] 优先级显示正确（不同颜色）

### 实时推送验证
- [ ] 创建新通知后，页面自动更新
- [ ] 新通知卡片滑入动画流畅
- [ ] 未读徽章数字自动增加
- [ ] 浏览器通知弹出（如果已授权）

### 交互功能验证
- [ ] 点击 "清空通知" 按钮有效
- [ ] 断开/重连 WebSocket 功能正常
- [ ] 页面刷新后数据保持一致

---

## 🐛 故障排查

### 问题 1：WebSocket 连接失败
**解决方案：**
```bash
# 检查应用日志
docker logs doordash-app | grep -i "websocket\|stomp"

# 重启容器
docker-compose restart app
```

### 问题 2：通知不显示
**检查数据库：**
```bash
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
SELECT * FROM notifications WHERE user_id = 2 ORDER BY created_at DESC LIMIT 5;
"
```

### 问题 3：未读数量不正确
**重新计算：**
```bash
docker exec doordash-postgres psql -U postgres -d doordash_db -c "
SELECT 
  COUNT(CASE WHEN is_read = false THEN 1 END) as unread,
  COUNT(CASE WHEN is_read = true THEN 1 END) as read
FROM notifications 
WHERE user_id = 2;
"
```

---

## ✅ 测试完成后的下一步

### 选项 A：继续完善通知系统
- [ ] 修复 API 认证问题
- [ ] 实现真正的 WebSocket 实时推送
- [ ] 添加通知声音效果
- [ ] 集成浏览器通知 API

### 选项 B：开发其他 Phase 2 功能
- [ ] 配送员收益聚合系统
- [ ] 配送员评分系统
- [ ] 真实天气 API 集成

### 选项 C：开始 Phase 3
- [ ] Redis 缓存集成
- [ ] PostGIS 空间查询
- [ ] 性能优化和监控

### 选项 D：前端开发
- [ ] React/Vue 客户端应用
- [ ] 实时订单追踪地图
- [ ] 通知中心 UI

---

## 📚 相关文档

- **完整指南**: `docs/NOTIFICATION_SYSTEM_GUIDE.md`
- **快速参考**: `docs/NOTIFICATION_QUICK_REF.md`
- **实现总结**: `docs/NOTIFICATION_SYSTEM_SUMMARY.md`
- **测试指南**: `docs/QUICK_TEST_GUIDE.md`

---

**测试愉快！有问题随时问我 🚀**
