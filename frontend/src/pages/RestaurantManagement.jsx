import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderService, restaurantService } from '../services/apiService';

const statusMap = {
  PENDING: { label: '待确认', color: 'bg-yellow-100 text-yellow-800', next: 'CONFIRMED' },
  CONFIRMED: { label: '已确认', color: 'bg-blue-100 text-blue-800', next: 'PREPARING' },
  PREPARING: { label: '制作中', color: 'bg-purple-100 text-purple-800', next: 'READY_FOR_PICKUP' },
  READY_FOR_PICKUP: { label: '待取餐', color: 'bg-orange-100 text-orange-800', next: 'PICKED_UP' },
  PICKED_UP: { label: '配送中', color: 'bg-indigo-100 text-indigo-800', next: 'IN_TRANSIT' },
  IN_TRANSIT: { label: '运输中', color: 'bg-cyan-100 text-cyan-800', next: 'DELIVERED' },
  DELIVERED: { label: '已送达', color: 'bg-green-100 text-green-800', next: null },
  CANCELLED: { label: '已取消', color: 'bg-red-100 text-red-800', next: null }
};

const RestaurantManagement = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [restaurantInfo, setRestaurantInfo] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    
    if (!token) {
      navigate('/login');
      return;
    }

    // 检查是否是餐厅老板
    if (user.role !== 'RESTAURANT_OWNER') {
      setError('您没有权限访问此页面。此页面仅供餐厅老板使用。');
      setLoading(false);
      return;
    }

    fetchRestaurantOrders();
  }, [navigate]);

  const fetchRestaurantOrders = async () => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    try {
      setLoading(true);

      // 获取餐厅信息
      const restaurantData = await restaurantService.getOwnerRestaurant();
      setRestaurantInfo(restaurantData);

      // 获取餐厅订单
      const ordersResponse = await orderService.getRestaurantOrders(restaurantData.id);
      
      // 按状态和创建时间排序
      const sortedOrders = (ordersResponse || []).sort((a, b) => {
        const statusPriority = {
          PENDING: 1,
          CONFIRMED: 2,
          PREPARING: 3,
          READY_FOR_PICKUP: 4,
          PICKED_UP: 5,
          IN_TRANSIT: 6,
          DELIVERED: 7,
          CANCELLED: 8
        };
        
        if (statusPriority[a.status] !== statusPriority[b.status]) {
          return statusPriority[a.status] - statusPriority[b.status];
        }
        
        return new Date(b.createdAt) - new Date(a.createdAt);
      });
      
      setOrders(sortedOrders);
      setError('');
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      console.error('Error response:', err.response);
      
      if (err.response?.status === 403) {
        if (user.role === 'RESTAURANT_OWNER') {
          setError('登录已过期，请重新登录');
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          navigate('/login');
          return;
        }
        setError('您没有权限访问此页面。请确保使用餐厅老板账号登录。');
      } else if (err.response?.status === 401) {
        setError('登录已过期，请重新登录');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login');
      } else {
        setError(err.response?.data?.message || '获取订单列表失败');
      }
    } finally {
      setLoading(false);
    }
  };

  const updateOrderStatus = async (orderId, newStatus) => {
    try {
      await orderService.updateStatus(orderId, { status: newStatus });

      // 刷新订单列表
      await fetchRestaurantOrders();
      
      alert(`订单状态已更新为：${statusMap[newStatus].label}`);
    } catch (err) {
      console.error('Failed to update order status:', err);
      alert(err.response?.data?.message || '更新订单状态失败');
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>
    );
  }

  if (error && !restaurantInfo) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6">
      {/* 餐厅信息头部 */}
      {restaurantInfo && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{restaurantInfo.name}</h1>
              <p className="text-gray-600 mt-1">{restaurantInfo.cuisine} • {restaurantInfo.address}</p>
            </div>
            <div className="text-right">
              <div className="text-sm text-gray-500">餐厅 ID</div>
              <div className="text-lg font-semibold text-gray-900">#{restaurantInfo.id}</div>
            </div>
          </div>
        </div>
      )}

      {/* 订单统计 */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="bg-yellow-50 rounded-lg p-4 border border-yellow-200">
          <div className="text-yellow-800 text-sm font-medium">待确认</div>
          <div className="text-2xl font-bold text-yellow-900 mt-1">
            {orders.filter(o => o.status === 'PENDING').length}
          </div>
        </div>
        <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
          <div className="text-blue-800 text-sm font-medium">制作中</div>
          <div className="text-2xl font-bold text-blue-900 mt-1">
            {orders.filter(o => ['CONFIRMED', 'PREPARING'].includes(o.status)).length}
          </div>
        </div>
        <div className="bg-orange-50 rounded-lg p-4 border border-orange-200">
          <div className="text-orange-800 text-sm font-medium">待取餐</div>
          <div className="text-2xl font-bold text-orange-900 mt-1">
            {orders.filter(o => o.status === 'READY_FOR_PICKUP').length}
          </div>
        </div>
        <div className="bg-green-50 rounded-lg p-4 border border-green-200">
          <div className="text-green-800 text-sm font-medium">今日已完成</div>
          <div className="text-2xl font-bold text-green-900 mt-1">
            {orders.filter(o => {
              const today = new Date().toDateString();
              const orderDate = new Date(o.createdAt).toDateString();
              return o.status === 'DELIVERED' && today === orderDate;
            }).length}
          </div>
        </div>
      </div>

      {/* 订单列表 */}
      <div className="bg-white rounded-lg shadow-md">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">订单管理</h2>
          <p className="text-gray-600 text-sm mt-1">管理您的餐厅订单状态</p>
        </div>

        {orders.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-gray-400 text-6xl mb-4">📋</div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无订单</h3>
            <p className="text-gray-600">您的餐厅还没有收到任何订单</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {orders.map((order) => (
              <div key={order.id} className="p-6 hover:bg-gray-50 transition-colors">
                <div className="flex items-start justify-between">
                  {/* 订单信息 */}
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-semibold text-gray-900">
                        订单 #{order.orderNumber}
                      </h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusMap[order.status]?.color || 'bg-gray-100 text-gray-800'}`}>
                        {statusMap[order.status]?.label || order.status}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-sm text-gray-600 mb-3">
                      <div>
                        <span className="font-medium">客户：</span>
                        {order.customerName} ({order.customerEmail})
                      </div>
                      <div>
                        <span className="font-medium">下单时间：</span>
                        {formatDate(order.createdAt)}
                      </div>
                      <div>
                        <span className="font-medium">配送地址：</span>
                        {order.deliveryAddressStreet}, {order.deliveryAddressCity}
                      </div>
                      <div>
                        <span className="font-medium">订单金额：</span>
                        <span className="text-red-600 font-semibold">¥{order.totalAmount}</span>
                      </div>
                    </div>

                    {/* 订单商品 */}
                    <div className="bg-gray-50 rounded-lg p-3 mb-3">
                      <div className="text-xs text-gray-500 mb-2">订单内容：</div>
                      {order.items?.map((item, idx) => (
                        <div key={idx} className="text-sm text-gray-700">
                          {item.menuItemName} x {item.quantity} = ¥{item.subtotal}
                        </div>
                      ))}
                    </div>

                    {order.specialInstructions && (
                      <div className="bg-yellow-50 border border-yellow-200 rounded p-2 text-sm text-yellow-800">
                        <span className="font-medium">备注：</span>
                        {order.specialInstructions}
                      </div>
                    )}
                  </div>

                  {/* 操作按钮 */}
                  <div className="ml-6 flex flex-col gap-2">
                    {statusMap[order.status]?.next && (
                      <button
                        onClick={() => updateOrderStatus(order.id, statusMap[order.status].next)}
                        className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors font-medium text-sm whitespace-nowrap"
                      >
                        {order.status === 'PENDING' && '确认订单 →'}
                        {order.status === 'CONFIRMED' && '开始制作 →'}
                        {order.status === 'PREPARING' && '制作完成 →'}
                        {order.status === 'READY_FOR_PICKUP' && '骑手已取餐 →'}
                      </button>
                    )}
                    
                    <button
                      onClick={() => navigate(`/orders/${order.id}`)}
                      className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors text-sm"
                    >
                      查看详情
                    </button>

                    {order.status === 'PENDING' && (
                      <button
                        onClick={() => {
                          if (confirm('确定要取消这个订单吗？')) {
                            updateOrderStatus(order.id, 'CANCELLED');
                          }
                        }}
                        className="bg-white text-red-600 px-4 py-2 rounded-lg border border-red-300 hover:bg-red-50 transition-colors text-sm"
                      >
                        取消订单
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default RestaurantManagement;
