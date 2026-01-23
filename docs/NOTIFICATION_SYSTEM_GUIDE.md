# 实时通知系统 - 测试指南

## 🎯 功能概述

Phase 2 实时通知系统已成功实现，支持：

### 核心功能
✅ **通知创建与存储** - 数据库持久化通知记录  
✅ **WebSocket 实时推送** - 订阅式实时通知推送  
✅ **通知类型分类** - 15+ 种通知类型  
✅ **优先级管理** - LOW, NORMAL, HIGH, URGENT  
✅ **已读/未读状态** - 完整的状态管理  
✅ **批量操作** - 全部标记为已读  
✅ **异步处理** - 不阻塞主业务流程  

---

## 📊 通知类型

### 订单相关通知

| 类型 | 触发场景 | 优先级 |
|------|----------|--------|
| `ORDER_CREATED` | 订单创建成功 | NORMAL |
| `ORDER_CONFIRMED` | 餐厅确认订单 | HIGH |
| `ORDER_PREPARING` | 餐厅正在准备 | NORMAL |
| `ORDER_READY` | 订单准备完成 | HIGH |
| `ORDER_PICKED_UP` | 配送员已取餐 | HIGH |
| `ORDER_IN_TRANSIT` | 配送中 | HIGH |
| `ORDER_DELIVERED` | 订单已送达 | HIGH |
| `ORDER_CANCELLED` | 订单已取消 | HIGH |

### 配送相关通知

| 类型 | 触发场景 | 优先级 |
|------|----------|--------|
| `DELIVERY_ASSIGNED` | 配送员已分配 | HIGH |
| `DELIVERY_ACCEPTED` | 配送员接单 | HIGH |
| `DELIVERY_REJECTED` | 配送员拒绝 | NORMAL |
| `DELIVERY_NEAR` | 配送员即将到达 | URGENT |

### 支付相关通知

| 类型 | 触发场景 | 优先级 |
|------|----------|--------|
| `PAYMENT_SUCCESS` | 支付成功 | NORMAL |
| `PAYMENT_FAILED` | 支付失败 | HIGH |
| `REFUND_PROCESSED` | 退款已处理 | NORMAL |

### 其他通知

| 类型 | 触发场景 | 优先级 |
|------|----------|--------|
| `PROMOTION` | 促销活动 | LOW |
| `SYSTEM_MESSAGE` | 系统消息 | NORMAL |

---

## 🗄️ 数据库 Schema

### Notifications 表结构

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    order_id BIGINT,
    delivery_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    extra_data TEXT,
    created_at TIMESTAMP NOT NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);
```

---

## 🔌 REST API 端点

### 1. 获取所有通知

**GET** `/api/notifications`

```bash
curl -X GET http://localhost:8080/api/notifications \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例:**

```json
[
  {
    "id": 1,
    "type": "ORDER_CREATED",
    "title": "订单已创建",
    "message": "您的订单 ORD-123 已创建成功！餐厅川味小厨正在确认订单。预计 25 分钟后送达。",
    "orderId": 123,
    "deliveryId": null,
    "isRead": false,
    "readAt": null,
    "priority": "NORMAL",
    "extraData": "{\"orderNumber\":\"ORD-123\",\"restaurantName\":\"川味小厨\"}",
    "createdAt": "2026-01-21T18:30:00",
    "timeAgo": "5 分钟前"
  },
  {
    "id": 2,
    "type": "DELIVERY_ASSIGNED",
    "title": "配送员已分配",
    "message": "配送员李师傅已接单，正在前往餐厅取餐",
    "orderId": 123,
    "deliveryId": 456,
    "isRead": false,
    "readAt": null,
    "priority": "HIGH",
    "extraData": "{\"driverName\":\"李师傅\"}",
    "createdAt": "2026-01-21T18:35:00",
    "timeAgo": "刚刚"
  }
]
```

### 2. 获取未读通知

**GET** `/api/notifications/unread`

```bash
curl -X GET http://localhost:8080/api/notifications/unread \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. 获取未读通知数量

**GET** `/api/notifications/unread/count`

```bash
curl -X GET http://localhost:8080/api/notifications/unread/count \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例:**

```json
{
  "count": 3,
  "hasUnread": true
}
```

### 4. 标记通知为已读

**PUT** `/api/notifications/{id}/read`

```bash
curl -X PUT http://localhost:8080/api/notifications/1/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例:**

```json
{
  "id": 1,
  "type": "ORDER_CREATED",
  "title": "订单已创建",
  "message": "您的订单 ORD-123 已创建成功！",
  "isRead": true,
  "readAt": "2026-01-21T18:40:00",
  "priority": "NORMAL"
}
```

### 5. 全部标记为已读

**PUT** `/api/notifications/read-all`

```bash
curl -X PUT http://localhost:8080/api/notifications/read-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例:**

```json
{
  "message": "已标记 5 条通知为已读",
  "count": 5
}
```

### 6. 删除通知

**DELETE** `/api/notifications/{id}`

```bash
curl -X DELETE http://localhost:8080/api/notifications/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**响应示例:**

```json
{
  "message": "通知已删除"
}
```

---

## 📡 WebSocket 实时推送

### 连接 WebSocket

**JavaScript 前端示例:**

```javascript
// 1. 建立 WebSocket 连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. 连接到服务器
stompClient.connect({}, function(frame) {
    console.log('WebSocket 已连接:', frame);
    
    // 3. 订阅通知频道（使用用户邮箱）
    const userEmail = 'customer@example.com';
    stompClient.subscribe(`/topic/notifications/${userEmail}`, function(message) {
        const notification = JSON.parse(message.body);
        console.log('收到新通知:', notification);
        
        // 显示通知
        showNotification(notification);
        
        // 更新未读数量小红点
        updateUnreadBadge();
    });
});

// 4. 显示通知（使用浏览器通知 API）
function showNotification(notification) {
    if (Notification.permission === "granted") {
        new Notification(notification.title, {
            body: notification.message,
            icon: '/icon.png',
            badge: '/badge.png'
        });
    }
    
    // 同时在页面上显示
    appendNotificationToList(notification);
}

// 5. 更新未读数量
function updateUnreadBadge() {
    fetch('/api/notifications/unread/count', {
        headers: {
            'Authorization': 'Bearer ' + getToken()
        }
    })
    .then(res => res.json())
    .then(data => {
        const badge = document.getElementById('notification-badge');
        badge.textContent = data.count;
        badge.style.display = data.hasUnread ? 'block' : 'none';
    });
}
```

### React 示例

```jsx
import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

function NotificationCenter({ userEmail }) {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [stompClient, setStompClient] = useState(null);

    useEffect(() => {
        // 连接 WebSocket
        const socket = new SockJS('http://localhost:8080/ws');
        const client = Stomp.over(socket);

        client.connect({}, () => {
            console.log('WebSocket 已连接');
            
            // 订阅通知
            client.subscribe(`/topic/notifications/${userEmail}`, (message) => {
                const notification = JSON.parse(message.body);
                
                // 添加到通知列表
                setNotifications(prev => [notification, ...prev]);
                
                // 更新未读数量
                setUnreadCount(prev => prev + 1);
                
                // 显示浏览器通知
                if (Notification.permission === 'granted') {
                    new Notification(notification.title, {
                        body: notification.message
                    });
                }
            });
        });

        setStompClient(client);

        return () => {
            if (client) {
                client.disconnect();
            }
        };
    }, [userEmail]);

    return (
        <div className="notification-center">
            <div className="notification-badge">
                {unreadCount > 0 && <span>{unreadCount}</span>}
            </div>
            <div className="notification-list">
                {notifications.map(notif => (
                    <NotificationItem key={notif.id} notification={notif} />
                ))}
            </div>
        </div>
    );
}
```

---

## 🧪 测试场景

### 场景 1: 订单创建通知

1. **创建订单**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "deliveryAddressId": 1,
    "items": [{"menuItemId": 1, "quantity": 2}],
    "paymentMethod": "CREDIT_CARD"
  }'
```

2. **自动收到通知**
- WebSocket 实时推送
- 类型: `ORDER_CREATED`
- 标题: "订单已创建"
- 消息: "您的订单 ORD-XXX 已创建成功！餐厅 XXX 正在确认订单。预计 XX 分钟后送达。"

3. **查询通知**
```bash
curl -X GET http://localhost:8080/api/notifications/unread \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 场景 2: 配送员接单通知

**触发方式:**
```java
// 在 DeliveryService 中调用
notificationService.notifyDriverAssigned(
    customerId,
    orderId,
    "李师傅"
);
```

**通知内容:**
- 类型: `DELIVERY_ASSIGNED`
- 标题: "配送员已分配"
- 消息: "配送员李师傅已接单，正在前往餐厅取餐"
- 优先级: HIGH

### 场景 3: 配送员即将到达

**触发方式:**
```java
// WebSocket 位置更新时检测距离
if (remainingDistance < 0.5) { // 小于 500 米
    notificationService.notifyDriverNearby(
        customerId,
        orderId,
        2  // 预计 2 分钟到达
    );
}
```

**通知内容:**
- 类型: `DELIVERY_NEAR`
- 标题: "配送员即将到达"
- 消息: "配送员距离您还有约 2 分钟，请准备接收订单"
- 优先级: URGENT

---

## 📊 通知流程图

```
订单创建
   ↓
[OrderService.createOrder()]
   ↓
保存订单到数据库
   ↓
[NotificationService.notifyOrderStatusChange()] ← 异步调用
   ↓
创建 Notification 实体
   ↓
保存到数据库
   ↓
通过 WebSocket 推送 → [客户端订阅 /topic/notifications/{userEmail}]
   ↓                        ↓
日志记录              实时显示通知
                           ↓
                     更新未读数量小红点
```

---

## 🔍 数据库查询

### 查看所有通知

```sql
SELECT 
    id,
    notification_type,
    title,
    message,
    is_read,
    priority,
    created_at
FROM notifications
ORDER BY created_at DESC
LIMIT 20;
```

### 查看未读通知统计

```sql
SELECT 
    notification_type,
    COUNT(*) as count
FROM notifications
WHERE is_read = FALSE
GROUP BY notification_type
ORDER BY count DESC;
```

### 查看用户通知历史

```sql
SELECT 
    n.id,
    n.notification_type,
    n.title,
    n.created_at,
    u.email
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE u.email = 'customer@example.com'
ORDER BY n.created_at DESC;
```

---

## ⚡ 性能优化

### 1. 数据库索引

已创建索引：
- `idx_user_id` - 快速查询用户通知
- `idx_notification_type` - 按类型筛选
- `idx_is_read` - 快速查询未读通知
- `idx_created_at` - 按时间排序

### 2. 异步处理

所有通知创建和发送都是异步的，不会阻塞主业务流程：

```java
@Async  // Spring 异步注解
public void createAndSendNotification(...) {
    // 创建通知
    // 保存到数据库
    // WebSocket 推送
}
```

### 3. 线程池配置

- **通知线程池**: 5-10 个线程
- **默认异步线程池**: 10-20 个线程
- **队列容量**: 100-200 个任务

---

## 📝 日志示例

```
2026-01-21 18:30:15 INFO  NotificationService - 创建通知: userId=5, type=ORDER_CREATED, title=订单已创建
2026-01-21 18:30:15 DEBUG NotificationService - WebSocket 通知已发送: destination=/topic/notifications/customer@example.com, type=ORDER_CREATED
2026-01-21 18:30:15 INFO  NotificationService - 通知已创建并发送: notificationId=1, userId=5, type=ORDER_CREATED
2026-01-21 18:30:20 INFO  OrderService - 订单状态通知已发送: orderId=123, status=PENDING
```

---

## ✅ 验证清单

- [x] Notification 实体创建
- [x] NotificationRepository 创建
- [x] NotificationDTO 创建
- [x] NotificationService 实现
- [x] NotificationController 创建
- [x] AsyncConfig 配置
- [x] OrderService 集成通知
- [x] WebSocket 实时推送
- [x] 数据库表自动创建
- [x] 应用成功启动

**状态:** ✅ 所有功能已实现并运行成功！

---

**实现时间:** 2026-01-21  
**状态:** 生产就绪
