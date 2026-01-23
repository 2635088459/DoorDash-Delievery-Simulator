# Phase 2 功能测试指南

## 🎯 Phase 2 新增功能

### 1. ✅ 配送费计算器 (DeliveryFeeCalculator)

**功能特性:**
- 基础费用: $3.00
- 距离费用: $1.50/km
- 高峰期加价: 1.5倍（11:00-13:00, 17:00-20:00）
- 恶劣天气加价: 1.2倍
- 最低配送费: $2.00
- 配送员收益: 80%（平台抽成20%）

**测试场景:**

```bash
# 场景1: 基础配送（3km，非高峰期）
距离: 3km
时间: 15:00
天气: 正常
预期配送费: $3.00 + (3 × $1.50) = $7.50
配送员收益: $7.50 × 80% = $6.00

# 场景2: 高峰期配送（5km，午餐时间）
距离: 5km
时间: 12:00
天气: 正常
预期配送费: ($3.00 + (5 × $1.50)) × 1.5 = $16.13
配送员收益: $16.13 × 80% = $12.90

# 场景3: 恶劣天气+高峰期（2km）
距离: 2km
时间: 18:00
天气: 恶劣
预期配送费: ($3.00 + (2 × $1.50)) × 1.5 × 1.2 = $10.80
配送员收益: $10.80 × 80% = $8.64
```

**代码示例:**

```java
@Autowired
private DeliveryFeeCalculator feeCalculator;

// 计算配送费
BigDecimal fee = feeCalculator.calculateDeliveryFee(
    5.0,                     // 5km
    LocalDateTime.now(),     // 当前时间
    false                    // 天气正常
);

// 计算配送员收益
BigDecimal earnings = feeCalculator.calculateDriverEarnings(fee);

// 估算配送时间
int minutes = feeCalculator.estimateDeliveryTime(5.0);
```

---

### 2. ✅ WebSocket 实时位置追踪

**功能特性:**
- 配送员实时位置更新
- 客户实时查看配送进度
- 自动计算预计送达时间（ETA）
- 双向通信支持

**WebSocket Endpoints:**

| Endpoint | 类型 | 描述 |
|----------|------|------|
| `/ws` | 连接端点 | 建立 WebSocket 连接 |
| `/app/location/update` | 消息发送 | 配送员发送位置更新 |
| `/topic/delivery/{deliveryId}` | 订阅 | 客户订阅配送进度 |
| `/topic/driver/{driverId}` | 订阅 | 管理员订阅配送员位置 |
| `/app/location/test` | 测试 | 测试 WebSocket 连接 |

**前端连接示例 (JavaScript):**

```javascript
// 1. 建立 WebSocket 连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. 连接到服务器
stompClient.connect({}, function(frame) {
    console.log('WebSocket 已连接:', frame);
    
    // 3. 订阅配送进度（客户端）
    const deliveryId = 123;
    stompClient.subscribe(`/topic/delivery/${deliveryId}`, function(message) {
        const status = JSON.parse(message.body);
        console.log('配送状态更新:', status);
        
        // 更新UI
        updateDeliveryStatus(status);
    });
    
    // 4. 订阅配送员位置（管理端）
    const driverId = 456;
    stompClient.subscribe(`/topic/driver/${driverId}`, function(message) {
        const location = JSON.parse(message.body);
        console.log('配送员位置更新:', location);
        
        // 在地图上更新配送员位置
        updateDriverLocationOnMap(location);
    });
});

// 5. 配送员发送位置更新
function sendLocationUpdate() {
    const locationUpdate = {
        deliveryId: 123,
        driverId: 456,
        latitude: 37.7749,
        longitude: -122.4194,
        speed: 25.5,      // km/h
        heading: 90.0,    // 东向
        timestamp: new Date().toISOString()
    };
    
    stompClient.send('/app/location/update', {}, JSON.stringify(locationUpdate));
}

// 6. 测试 WebSocket 连接
function testWebSocket() {
    stompClient.subscribe('/topic/location/test', function(response) {
        console.log('测试响应:', response.body);
    });
    
    stompClient.send('/app/location/test', {}, 'Hello WebSocket!');
}
```

**Python 测试脚本:**

```python
#!/usr/bin/env python3
"""
WebSocket 位置追踪测试
"""
import websocket
import json
import time

# WebSocket 服务器地址
WS_URL = "ws://localhost:8080/ws"

def on_message(ws, message):
    print(f"收到消息: {message}")
    data = json.loads(message)
    print(f"配送状态: {data}")

def on_error(ws, error):
    print(f"错误: {error}")

def on_close(ws, close_status_code, close_msg):
    print("WebSocket 连接已关闭")

def on_open(ws):
    print("WebSocket 连接已建立")
    
    # 订阅配送进度
    subscribe_msg = json.dumps({
        "command": "subscribe",
        "destination": "/topic/delivery/123"
    })
    ws.send(subscribe_msg)
    
    # 模拟配送员发送位置更新
    for i in range(5):
        location_update = {
            "deliveryId": 123,
            "driverId": 456,
            "latitude": 37.7749 + (i * 0.001),
            "longitude": -122.4194 + (i * 0.001),
            "speed": 25.5,
            "heading": 90.0,
            "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S")
        }
        
        msg = json.dumps({
            "destination": "/app/location/update",
            "body": json.dumps(location_update)
        })
        
        ws.send(msg)
        time.sleep(2)  # 每2秒发送一次

if __name__ == "__main__":
    ws = websocket.WebSocketApp(WS_URL,
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    
    ws.run_forever()
```

**测试步骤:**

```bash
# 1. 使用 wscat 测试 WebSocket 连接
npm install -g wscat
wscat -c ws://localhost:8080/ws

# 2. 发送测试消息
> CONNECT
> SUBSCRIBE
destination:/topic/location/test

> SEND
destination:/app/location/test
content-type:text/plain

Hello WebSocket!
```

---

### 3. ✅ 智能 ETA 计算

**算法:**
- 平均速度: 20 km/h
- 准备时间: 10 分钟
- 动态更新: 基于实时位置

**示例:**

```java
// 计算预计送达时间
LocalDateTime eta = feeCalculator.estimateDeliveryTime(
    LocalDateTime.now(),     // 当前时间
    5.0                      // 剩余距离 5km
);

// 预计: 现在 + 10分钟（准备） + 15分钟（行驶） = 25分钟后送达
```

---

## 🧪 完整测试流程

### 场景: 实时配送追踪

1. **创建订单**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": 1,
    "items": [{"menuItemId": 1, "quantity": 2}],
    "deliveryAddress": "123 Main St",
    "deliveryLat": 37.7749,
    "deliveryLon": -122.4194
  }'
```

2. **配送员接单**
```bash
curl -X POST http://localhost:8080/api/deliveries/123/accept \
  -H "Authorization: Bearer DRIVER_TOKEN"
```

3. **客户端订阅配送进度**
```javascript
stompClient.subscribe('/topic/delivery/123', function(message) {
    const status = JSON.parse(message.body);
    console.log(`配送员距离: ${status.message}`);
    console.log(`预计送达: ${status.estimatedArrival}`);
});
```

4. **配送员更新位置（每5秒）**
```javascript
setInterval(() => {
    sendLocationUpdate();
}, 5000);
```

5. **客户端实时接收**
```
配送员距离您还有 4.5 公里，预计 23 分钟送达
配送员距离您还有 3.2 公里，预计 16 分钟送达
配送员距离您还有 1.8 公里，预计 9 分钟送达
配送员距离您还有 0.3 公里，预计 2 分钟送达
```

---

## 📊 Phase 2 vs Phase 1 对比

| 功能 | Phase 1 | Phase 2 |
|------|---------|---------|
| 配送费计算 | ❌ 固定费用 | ✅ 动态定价（高峰期+天气） |
| 位置追踪 | ❌ 仅存储 | ✅ 实时 WebSocket 推送 |
| ETA 计算 | ❌ 固定时间 | ✅ 基于实时距离动态计算 |
| 配送员收益 | ❌ 手动计算 | ✅ 自动计算并记录 |
| 客户体验 | ❌ 刷新页面 | ✅ 实时更新无需刷新 |

---

## 🚀 下一步: Phase 3

Phase 3 将实现:
1. **Redis 缓存**: 配送员位置缓存，提升查询性能
2. **PostGIS**: 空间数据库查询优化
3. **批量分配算法**: 智能分配多个订单给最优配送员
4. **配送员评分系统**: 基于准时率和客户评价

---

**Phase 2 完成时间**: 2026-01-21  
**状态**: ✅ 已部署运行
