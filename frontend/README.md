# DoorDash Frontend

现代化的 DoorDash 配送平台前端应用。

## 🚀 技术栈

- **React 18** - UI 框架
- **Vite** - 构建工具
- **Tailwind CSS** - 样式框架
- **Zustand** - 状态管理
- **React Router** - 路由管理
- **Axios** - HTTP 客户端
- **STOMP.js + SockJS** - WebSocket 实时通信
- **React Hot Toast** - 通知提示
- **Lucide React** - 图标库
- **date-fns** - 日期处理

## 📦 安装

```bash
npm install
```

## 🏃 运行

### 开发模式
```bash
npm run dev
```
访问 http://localhost:3000

### 生产构建
```bash
npm run build
npm run preview
```

## 🎯 功能特性

### ✅ 已实现
- 🔐 用户认证（登录/注册）
- 🏠 首页仪表板
- 🔔 **实时通知中心**
  - WebSocket 实时推送
  - 15+ 种通知类型
  - 优先级分级显示
  - 已读/未读管理
  - 一键全部已读
  - 相对时间显示
- 🍔 餐厅列表（占位）
- 📦 订单列表（占位）
- 🎨 现代化 UI 设计
- 📱 响应式布局
- 🌐 WebSocket 状态监控

### 🚧 开发中
- 餐厅详情和菜单
- 购物车功能
- 订单创建流程
- 实时订单追踪
- 配送员位置地图
- 支付集成

## 📂 项目结构

```
frontend/
├── src/
│   ├── components/        # React 组件
│   │   ├── Navbar.jsx    # 导航栏（含通知徽章）
│   │   └── PrivateRoute.jsx  # 路由守卫
│   ├── pages/            # 页面组件
│   │   ├── Home.jsx      # 首页
│   │   ├── Login.jsx     # 登录页
│   │   ├── Register.jsx  # 注册页
│   │   ├── Notifications.jsx  # 通知中心 ⭐
│   │   ├── Restaurants.jsx    # 餐厅列表
│   │   ├── Orders.jsx         # 订单列表
│   │   └── ...
│   ├── services/         # 服务层
│   │   ├── api.js        # Axios 实例
│   │   ├── apiService.js # API 调用封装
│   │   └── websocket.js  # WebSocket 服务 ⭐
│   ├── store/            # 状态管理
│   │   ├── authStore.js  # 认证状态
│   │   └── notificationStore.js  # 通知状态 ⭐
│   ├── App.jsx           # 根组件
│   ├── main.jsx          # 入口文件
│   └── index.css         # 全局样式
├── public/               # 静态资源
├── index.html            # HTML 模板
├── vite.config.js        # Vite 配置
├── tailwind.config.js    # Tailwind 配置
└── package.json
```

## 🔔 实时通知系统

### WebSocket 连接
应用启动后自动连接到后端 WebSocket 服务：
- 地址: `ws://localhost:8080/api/ws`
- 协议: STOMP over SockJS
- 订阅: `/topic/notifications/{userEmail}`

### 通知类型支持
- 📝 ORDER_CREATED - 订单创建
- ✅ ORDER_CONFIRMED - 订单确认
- 👨‍🍳 ORDER_PREPARING - 准备中
- 🎉 ORDER_READY - 准备完成
- 🚴 DELIVERY_ASSIGNED - 配送员分配
- 📦 DELIVERY_PICKED_UP - 已取餐
- 🚚 DELIVERY_IN_PROGRESS - 配送中
- ⚠️ DELIVERY_NEAR - 即将到达（紧急）
- ✅ ORDER_DELIVERED - 已送达
- 💳 PAYMENT_SUCCESS - 支付成功
- 🎁 PROMOTION_AVAILABLE - 促销通知
- ⭐ DRIVER_RATING_REQUEST - 评分请求

### 优先级显示
- 🔴 **URGENT** - 红色边框（如：配送员即将到达）
- 🟠 **HIGH** - 橙色边框（如：配送进度）
- 🟢 **NORMAL** - 蓝色边框（如：订单状态）
- 🔵 **LOW** - 灰色边框（如：促销信息）

## 🔐 测试账户

```
邮箱: customer@example.com
密码: password123
```

## 🌐 API 端点

前端通过 Vite 代理访问后端 API：

### 认证
- POST `/api/auth/login` - 登录
- POST `/api/auth/register` - 注册

### 通知
- GET `/api/notifications` - 获取所有通知
- GET `/api/notifications/unread` - 获取未读通知
- GET `/api/notifications/unread/count` - 获取未读数量
- PUT `/api/notifications/{id}/read` - 标记已读
- PUT `/api/notifications/read-all` - 全部已读
- DELETE `/api/notifications/{id}` - 删除通知

### 餐厅
- GET `/api/restaurants` - 获取餐厅列表
- GET `/api/restaurants/{id}` - 获取餐厅详情

### 订单
- POST `/api/orders` - 创建订单
- GET `/api/orders/my-orders` - 我的订单
- GET `/api/orders/{id}` - 订单详情

## 🎨 UI 设计

### 颜色主题
- Primary: Red (#ef4444 系列)
- Success: Green (#10b981)
- Warning: Yellow (#f59e0b)
- Error: Red (#ef4444)
- Info: Blue (#3b82f6)

### 动画效果
- `slide-in` - 通知卡片滑入
- `fade-in` - 淡入效果
- `bounce-subtle` - 未读徽章跳动

## 🔧 环境配置

### 开发环境
```env
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/api/ws
```

### 生产环境
```env
VITE_API_URL=https://your-domain.com/api
VITE_WS_URL=wss://your-domain.com/api/ws
```

## 📱 响应式设计

支持多种设备尺寸：
- 📱 Mobile: < 768px
- 📱 Tablet: 768px - 1024px
- 💻 Desktop: > 1024px

## 🐛 调试

### 查看 WebSocket 日志
浏览器控制台会显示：
- WebSocket 连接状态
- STOMP 调试信息
- 接收到的通知消息

### 查看网络请求
使用浏览器开发者工具的 Network 标签页查看：
- API 请求/响应
- WebSocket 连接
- 错误信息

## 🚀 部署

### Docker 部署（推荐）
```bash
# 构建镜像
docker build -t doordash-frontend .

# 运行容器
docker run -p 3000:3000 doordash-frontend
```

### Nginx 部署
```bash
# 构建生产版本
npm run build

# 将 dist 目录部署到 Nginx
cp -r dist/* /var/www/html/
```

## 📝 开发计划

### Phase 1 - MVP ✅
- [x] 用户认证
- [x] 基础页面框架
- [x] 实时通知系统
- [x] WebSocket 集成

### Phase 2 - 核心功能 🚧
- [ ] 餐厅详情和菜单
- [ ] 购物车功能
- [ ] 订单创建流程
- [ ] 支付集成

### Phase 3 - 高级功能 📅
- [ ] 实时订单追踪地图
- [ ] 配送员位置实时更新
- [ ] 评价和评分系统
- [ ] 优惠券和促销

### Phase 4 - 优化 📅
- [ ] PWA 支持
- [ ] 离线功能
- [ ] 性能优化
- [ ] SEO 优化

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

---

**Made with ❤️ using React + Vite**
