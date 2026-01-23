# 🚀 实时通知系统快速测试指南

## 5分钟完整测试流程

### 步骤 1：准备 WebSocket 连接 （1分钟）

1. **打开测试页面**（已打开）
   - 文件：`docs/notification-test.html`
   - 或访问：file:///Users/aaronshan2635088459/Desktop/DoorDash/docs/notification-test.html

2. **连接 WebSocket**
   ```
   用户邮箱：customer@example.com
   WebSocket URL：http://localhost:8080/api/ws
   ```
   
3. **点击按钮**
   - 点击 "连接 WebSocket"
   - 等待状态变为 "已连接"（绿色圆圈）

---

### 步骤 2：运行测试脚本 （2分钟）

**在终端运行：**
```bash
./test-notification-flow.sh
```

**测试脚本会自动执行：**
- ✅ 用户登录/注册
- ✅ 创建测试订单
- ✅ 触发实时通知
- ✅ 查询通知列表
- ✅ 标记已读

---

### 步骤 3：观察实时效果 （2分钟）

**在 notification-test.html 页面观察：**

1. **实时通知推送**
   - 订单创建后，页面应立即收到通知
   - 通知卡片从右侧滑入
   - 显示订单信息

2. **未读徽章更新**
   - 红色徽章显示未读数量
   - 自动更新数字

3. **通知内容验证**
   - 标题：订单创建成功
   - 消息：包含订单号、餐厅名、预计送达时间
   - 时间：显示"刚刚"

4. **浏览器通知**（如果授权）
   - 系统级通知弹出
   - 点击可跳转到应用

---

## 快速 API 测试命令

### 1. 用户登录（获取 Token）
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }'
```

### 2. 创建订单（触发通知）
```bash
# 替换 YOUR_TOKEN
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "items": [{
      "menuItemId": 1,
      "quantity": 2
    }],
    "deliveryAddress": "123 Test St",
    "deliveryLatitude": 37.7749,
    "deliveryLongitude": -122.4194
  }'
```

### 3. 查看通知列表
```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. 查看未读数量
```bash
curl http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 5. 标记已读
```bash
# 替换 NOTIFICATION_ID
curl -X PUT http://localhost:8080/api/notifications/NOTIFICATION_ID/read \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 6. 标记全部已读
```bash
curl -X PUT http://localhost:8080/api/notifications/read-all \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 验证清单

### WebSocket 连接验证
- [ ] 页面显示 "已连接" 状态（绿色）
- [ ] 控制台无连接错误
- [ ] 订阅成功 `/topic/notifications/customer@example.com`

### 实时通知验证
- [ ] 订单创建后立即收到通知（< 1秒）
- [ ] 通知内容正确（订单号、餐厅名、时间）
- [ ] 通知类型为 ORDER_CREATED
- [ ] 优先级为 NORMAL

### UI 功能验证
- [ ] 通知卡片滑入动画流畅
- [ ] 未读徽章数字正确
- [ ] "刚刚" 时间显示正确
- [ ] 清空通知按钮可用

### REST API 验证
- [ ] 查询通知列表成功
- [ ] 未读数量统计正确
- [ ] 标记已读功能正常
- [ ] 批量已读功能正常

---

## 高级测试场景

### 场景 1：多通知测试
```bash
# 连续创建 3 个订单
for i in {1..3}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Authorization: Bearer YOUR_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "restaurantId": 1,
      "items": [{"menuItemId": 1, "quantity": 1}],
      "deliveryAddress": "Test Address '$i'",
      "deliveryLatitude": 37.7749,
      "deliveryLongitude": -122.4194
    }'
  sleep 2
done
```

**预期结果：**
- 页面收到 3 个实时通知
- 未读徽章显示 3
- 通知按时间倒序排列

### 场景 2：断线重连测试
1. 点击 "断开连接"
2. 创建订单（此时不会实时收到）
3. 点击 "连接 WebSocket"
4. 通过 REST API 查询通知（应该能看到）

### 场景 3：不同通知类型
```bash
# 需要先创建订单和配送，然后可以触发其他类型通知
# ORDER_CONFIRMED、DELIVERY_ASSIGNED、DELIVERY_NEAR 等
```

---

## 性能验证

### 通知发送性能
```bash
# 测试异步性能：订单创建应该很快返回
time curl -X POST http://localhost:8080/api/orders ...
```

**预期：**
- 订单创建响应时间 < 500ms
- 通知异步发送不阻塞

### 数据库查询性能
```bash
# 检查索引是否生效
docker exec doordash-postgres psql -U postgres -d doordash -c "
EXPLAIN ANALYZE 
SELECT * FROM notifications 
WHERE user_id = 1 
ORDER BY created_at DESC 
LIMIT 10;
"
```

**预期：**
- 使用 idx_user_id 和 idx_created_at 索引
- 查询时间 < 10ms

---

## 故障排查

### 问题 1：WebSocket 连接失败
**检查：**
```bash
# 确认 WebSocket 端点可用
curl http://localhost:8080/api/ws/info

# 检查应用日志
docker logs doordash-app | grep -i "websocket\|stomp"
```

### 问题 2：没有收到通知
**检查：**
1. 确认订单创建成功（返回 200）
2. 检查用户邮箱是否匹配
3. 查看数据库通知表
```bash
docker exec doordash-postgres psql -U postgres -d doordash -c "
SELECT id, notification_type, title, created_at 
FROM notifications 
ORDER BY created_at DESC 
LIMIT 5;
"
```

### 问题 3：通知延迟
**检查：**
```bash
# 查看异步线程池状态
docker logs doordash-app | grep -i "taskExecutor\|notification"

# 检查服务器负载
docker stats doordash-app
```

---

## 下一步建议

### 立即可做：
1. ✅ 完成基础功能测试
2. 🔄 测试多种通知类型
3. 📱 测试浏览器通知权限
4. 🎨 自定义通知样式

### 短期优化：
1. 添加通知声音
2. 实现通知分组（按订单）
3. 添加通知操作按钮（查看订单、取消等）
4. 集成消息已读状态同步

### 长期计划：
1. 移动端推送（FCM/APNs）
2. 邮件通知备份
3. 通知偏好设置
4. 通知统计分析

---

## 成功标准

测试通过标准：
- ✅ WebSocket 连接稳定
- ✅ 通知实时推送（< 1秒延迟）
- ✅ 通知内容准确
- ✅ UI 交互流畅
- ✅ REST API 功能完整
- ✅ 性能指标达标

---

**Happy Testing! 🎉**

有问题随时查看：
- 完整文档：`NOTIFICATION_SYSTEM_GUIDE.md`
- 快速参考：`NOTIFICATION_QUICK_REF.md`
- 实现总结：`NOTIFICATION_SYSTEM_SUMMARY.md`
