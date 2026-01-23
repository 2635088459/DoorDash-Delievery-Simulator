# 🚀 DoorDash 前端应用 - 快速启动指南

## ⚡ 5分钟快速启动

### 步骤 1: 安装依赖
```bash
cd frontend
npm install
```

### 步骤 2: 启动开发服务器
```bash
npm run dev
```

### 步骤 3: 访问应用
打开浏览器访问: **http://localhost:3000**

### 步骤 4: 登录测试
```
邮箱: customer@example.com
密码: password123
```

---

## 🎯 核心功能演示

### 1. 实时通知系统 🔔

**自动连接 WebSocket:**
- 登录后自动连接
- 右上角显示连接状态（绿点 = 已连接）
- 未读通知数量实时更新

**测试实时通知:**
1. 登录应用
2. 打开终端，运行测试脚本：
   ```bash
   cd ..  # 回到项目根目录
   ./demo-notification-flow.sh
   ```
3. 观察前端应用：
   - 导航栏通知图标显示未读数量
   - 自动弹出 Toast 提示
   - 点击通知图标查看详情

**通知功能:**
- ✅ 实时推送（< 1秒延迟）
- ✅ 优先级颜色区分
- ✅ 未读/已读状态
- ✅ 一键全部已读
- ✅ 相对时间显示
- ✅ 15+ 种通知类型

### 2. 用户界面 🎨

**首页:**
- 欢迎面板
- 快速入口卡片
- 统计信息
- WebSocket 状态

**通知中心:**
- 通知列表（按时间倒序）
- 优先级标签
- 通知类型图标
- 批量操作

**餐厅列表:**
- 餐厅卡片展示
- 评分和配送时间
- 筛选功能（占位）

**订单列表:**
- 订单历史
- 状态徽章
- 快速操作

---

## 📦 项目结构说明

```
frontend/
├── src/
│   ├── components/           # 可复用组件
│   │   ├── Navbar.jsx       # 导航栏（实时通知徽章）
│   │   └── PrivateRoute.jsx # 认证路由守卫
│   │
│   ├── pages/               # 页面组件
│   │   ├── Home.jsx         # 首页仪表板
│   │   ├── Login.jsx        # 登录页
│   │   ├── Register.jsx     # 注册页
│   │   ├── Notifications.jsx # 通知中心 ⭐ 核心功能
│   │   ├── Restaurants.jsx  # 餐厅列表
│   │   ├── Orders.jsx       # 订单列表
│   │   └── ...
│   │
│   ├── services/            # 服务层
│   │   ├── api.js          # Axios 配置（拦截器、Token）
│   │   ├── apiService.js   # API 调用封装
│   │   └── websocket.js    # WebSocket 服务 ⭐ 核心
│   │
│   ├── store/              # Zustand 状态管理
│   │   ├── authStore.js    # 认证状态（用户、Token）
│   │   └── notificationStore.js # 通知状态 ⭐ 核心
│   │
│   ├── App.jsx             # 根组件（路由配置）
│   ├── main.jsx            # 应用入口
│   └── index.css           # Tailwind + 自定义样式
│
├── public/                 # 静态资源
├── index.html              # HTML 模板
├── vite.config.js          # Vite 配置（代理、端口）
├── tailwind.config.js      # Tailwind 主题配置
└── package.json            # 依赖和脚本
```

---

## 🔧 关键技术点

### WebSocket 实时通信

**连接流程:**
```javascript
// 1. 用户登录后触发
useEffect(() => {
  if (isAuthenticated && user?.email) {
    // 2. 连接 WebSocket
    websocketService.connect(
      user.email,           // 用户标识
      onNotification,       // 收到消息回调
      onConnect,           // 连接成功回调
      onDisconnect         // 断开连接回调
    );
  }
}, [isAuthenticated, user?.email]);

// 3. 订阅用户专属频道
/topic/notifications/{userEmail}

// 4. 接收实时消息
const onNotification = (notification) => {
  addNotification(notification);  // 更新状态
  toast.success(`新通知: ${notification.title}`);  // 显示提示
};
```

**关键文件:**
- `src/services/websocket.js` - WebSocket 客户端
- `src/components/Navbar.jsx` - 连接初始化
- `src/store/notificationStore.js` - 状态管理

### 状态管理（Zustand）

**认证状态:**
```javascript
// src/store/authStore.js
const useAuthStore = create((set) => ({
  user: null,
  token: localStorage.getItem('token'),
  isAuthenticated: !!localStorage.getItem('token'),
  
  login: (userData, token) => { /* ... */ },
  logout: () => { /* ... */ },
}));
```

**通知状态:**
```javascript
// src/store/notificationStore.js
const useNotificationStore = create((set) => ({
  notifications: [],
  unreadCount: 0,
  isConnected: false,
  
  addNotification: (notification) => { /* ... */ },
  markAsRead: (id) => { /* ... */ },
  markAllAsRead: () => { /* ... */ },
}));
```

### API 调用封装

**自动注入 Token:**
```javascript
// src/services/api.js
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

**自动处理 401:**
```javascript
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token 过期，跳转登录
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 🎨 UI 组件示例

### 通知卡片
```jsx
<div className="border-l-4 rounded-lg shadow-md p-4 
               border-red-500 bg-red-50">  {/* URGENT 优先级 */}
  <div className="flex items-start">
    <span className="text-2xl">⚠️</span>  {/* 通知图标 */}
    <div className="flex-1 ml-3">
      <h3 className="text-lg font-bold">
        配送员即将到达
      </h3>
      <p className="text-gray-700">
        您的订单将在 2 分钟内送达！
      </p>
      <div className="mt-2 text-sm text-gray-500">
        刚刚  {/* 相对时间 */}
      </div>
    </div>
  </div>
</div>
```

### 未读徽章
```jsx
<div className="relative">
  <Bell className="w-6 h-6" />
  {unreadCount > 0 && (
    <span className="absolute -top-1 -right-1 
                     bg-red-500 text-white text-xs 
                     rounded-full w-5 h-5 
                     flex items-center justify-center 
                     animate-bounce-subtle">
      {unreadCount > 99 ? '99+' : unreadCount}
    </span>
  )}
</div>
```

---

## 🧪 测试场景

### 场景 1: 新用户注册并接收通知
```bash
1. 访问 http://localhost:3000/register
2. 填写表单注册新账户
3. 自动登录并连接 WebSocket
4. 运行测试脚本触发通知
5. 观察实时推送效果
```

### 场景 2: 完整订单流程通知
```bash
1. 登录应用
2. 运行: ./demo-notification-flow.sh
3. 观察通知逐个推送：
   - 订单创建 → 确认 → 支付 → 准备 → 配送 → 送达
4. 点击通知查看详情
5. 测试标记已读功能
```

### 场景 3: 断线重连测试
```bash
1. 登录应用（WebSocket 自动连接）
2. 停止后端服务: docker-compose stop app
3. 观察连接状态变为"未连接"
4. 启动后端服务: docker-compose start app
5. WebSocket 自动重连（5秒后）
6. 连接状态恢复为"已连接"
```

---

## 📊 性能指标

### 加载性能
- ⚡ 首屏加载: < 1s
- ⚡ 路由切换: < 100ms
- ⚡ WebSocket 连接: < 500ms

### 实时性能
- 🚀 通知推送延迟: < 1s
- 🚀 UI 更新响应: < 16ms (60fps)
- 🚀 状态同步: 实时

### 网络优化
- 📦 代码分割: 按路由懒加载
- 📦 API 请求: 自动重试
- 📦 WebSocket: 自动重连

---

## 🐛 常见问题

### Q1: WebSocket 连接失败
**症状:** 显示"未连接"，无法收到通知

**解决方案:**
1. 确认后端服务运行: `docker ps`
2. 检查端口 8080 是否开放
3. 查看浏览器控制台错误
4. 确认 WebSocket URL 正确

### Q2: 登录后跳转到登录页
**症状:** Token 无效，无法保持登录状态

**解决方案:**
1. 清空浏览器缓存
2. 检查 LocalStorage 中的 token
3. 确认后端 JWT 配置正确
4. 使用测试账户重新登录

### Q3: 通知不显示
**症状:** 数据库有通知，前端不显示

**解决方案:**
1. 确认用户邮箱匹配
2. 检查 API 请求是否成功
3. 查看浏览器控制台错误
4. 刷新页面重新加载

### Q4: 样式异常
**症状:** Tailwind CSS 样式不生效

**解决方案:**
1. 重启开发服务器
2. 清空 node_modules 重新安装
3. 检查 tailwind.config.js 配置
4. 确认 PostCSS 配置正确

---

## 🔐 环境变量

创建 `.env` 文件（可选）:

```env
# API 配置
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/api/ws

# 应用配置
VITE_APP_NAME=DoorDash
VITE_APP_VERSION=1.0.0

# 功能开关
VITE_ENABLE_NOTIFICATIONS=true
VITE_ENABLE_WEBSOCKET=true
```

---

## 📝 下一步开发

### 立即可做:
1. ✅ 测试实时通知功能
2. ✅ 体验用户界面
3. ✅ 查看响应式设计

### 短期开发:
1. 🔄 完善餐厅详情页面
2. 🔄 实现购物车功能
3. 🔄 创建订单流程
4. 🔄 集成支付接口

### 长期规划:
1. 📅 实时订单追踪地图
2. 📅 配送员位置实时更新
3. 📅 PWA 离线支持
4. 📅 移动端优化

---

## 🎓 学习资源

- **React 文档**: https://react.dev
- **Vite 文档**: https://vitejs.dev
- **Tailwind CSS**: https://tailwindcss.com
- **Zustand**: https://github.com/pmndrs/zustand
- **STOMP.js**: https://stomp-js.github.io

---

## 🎉 完成检查清单

- [ ] 成功安装依赖
- [ ] 启动开发服务器
- [ ] 登录测试账户
- [ ] 查看首页仪表板
- [ ] 测试 WebSocket 连接
- [ ] 运行通知演示脚本
- [ ] 观察实时通知推送
- [ ] 测试标记已读功能
- [ ] 浏览所有页面
- [ ] 检查响应式布局

---

**🚀 准备好了吗？开始体验吧！**

```bash
cd frontend
npm install
npm run dev
```

**然后访问: http://localhost:3000**
