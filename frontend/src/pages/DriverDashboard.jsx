import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { MapPin, Clock, DollarSign, Package, CheckCircle } from 'lucide-react';

const API_BASE_URL = 'http://localhost:8080/api';

const statusMap = {
  READY_FOR_PICKUP: { label: '待取餐', color: 'bg-orange-100 text-orange-800', action: 'PICKED_UP', actionLabel: '已取餐' },
  PICKED_UP: { label: '已取餐', color: 'bg-blue-100 text-blue-800', action: 'IN_TRANSIT', actionLabel: '开始配送' },
  IN_TRANSIT: { label: '配送中', color: 'bg-purple-100 text-purple-800', action: 'DELIVERED', actionLabel: '完成配送' },
  DELIVERED: { label: '已送达', color: 'bg-green-100 text-green-800', action: null, actionLabel: null }
};

const DriverDashboard = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    today: 0,
    thisWeek: 0,
    totalEarnings: 0
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    
    if (!token) {
      navigate('/login');
      return;
    }

    // 检查是否是骑手
    if (user.role !== 'DRIVER') {
      setError('您没有权限访问此页面。此页面仅供配送骑手使用。');
      setLoading(false);
      return;
    }

    fetchAvailableOrders();
  }, [navigate]);

  const fetchAvailableOrders = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');

      // 获取可接单的订单（状态为 READY_FOR_PICKUP, PICKED_UP, IN_TRANSIT）
      const response = await axios.get(
        `${API_BASE_URL}/orders/available`,
        {
          headers: { Authorization: `Bearer ${token}` }
        }
      );
      
      setOrders(response.data);
      calculateStats(response.data);
      setError('');
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      
      // 如果API不存在，显示友好提示
      if (err.response?.status === 404) {
        setError('骑手功能正在开发中，敬请期待！');
      } else {
        setError(err.response?.data?.message || '获取订单列表失败');
      }
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (orderList) => {
    const today = new Date().toDateString();
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const todayOrders = orderList.filter(o => 
      o.status === 'DELIVERED' && 
      new Date(o.actualDelivery).toDateString() === today
    ).length;

    const weekOrders = orderList.filter(o => 
      o.status === 'DELIVERED' && 
      new Date(o.actualDelivery) >= weekAgo
    ).length;

    // 假设每单配送费 5 元
    const earnings = orderList.filter(o => o.status === 'DELIVERED').length * 5;

    setStats({
      today: todayOrders,
      thisWeek: weekOrders,
      totalEarnings: earnings
    });
  };

  const updateOrderStatus = async (orderId, newStatus) => {
    try {
      const token = localStorage.getItem('token');
      
      await axios.put(
        `${API_BASE_URL}/orders/${orderId}/status`,
        { status: newStatus },
        {
          headers: { 
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      await fetchAvailableOrders();
      alert(`订单状态已更新为：${statusMap[newStatus]?.label || newStatus}`);
    } catch (err) {
      console.error('Failed to update order status:', err);
      alert(err.response?.data?.message || '更新订单状态失败');
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error && orders.length === 0) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <p className="text-yellow-800">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="mt-4 bg-yellow-600 text-white px-4 py-2 rounded hover:bg-yellow-700"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6">
      {/* 骑手信息头部 */}
      <div className="bg-gradient-to-r from-blue-500 to-blue-700 rounded-lg shadow-md p-6 mb-6 text-white">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">配送骑手工作台</h1>
            <p className="mt-1 opacity-90">管理您的配送订单，赚取收入</p>
          </div>
          <div className="text-right">
            <Package className="w-12 h-12 mb-2 mx-auto" />
            <div className="text-sm opacity-90">在线接单中</div>
          </div>
        </div>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-white rounded-lg p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-gray-500 text-sm font-medium">今日完成</div>
              <div className="text-3xl font-bold text-blue-600 mt-1">{stats.today}</div>
              <div className="text-xs text-gray-400 mt-1">笔订单</div>
            </div>
            <CheckCircle className="w-12 h-12 text-blue-500 opacity-20" />
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-gray-500 text-sm font-medium">本周完成</div>
              <div className="text-3xl font-bold text-green-600 mt-1">{stats.thisWeek}</div>
              <div className="text-xs text-gray-400 mt-1">笔订单</div>
            </div>
            <Package className="w-12 h-12 text-green-500 opacity-20" />
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 border border-gray-200 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-gray-500 text-sm font-medium">总收入</div>
              <div className="text-3xl font-bold text-yellow-600 mt-1">¥{stats.totalEarnings}</div>
              <div className="text-xs text-gray-400 mt-1">配送费</div>
            </div>
            <DollarSign className="w-12 h-12 text-yellow-500 opacity-20" />
          </div>
        </div>
      </div>

      {/* 订单列表 */}
      <div className="bg-white rounded-lg shadow-md">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">可配送订单</h2>
          <p className="text-gray-600 text-sm mt-1">接单并完成配送任务</p>
        </div>

        {orders.length === 0 ? (
          <div className="p-12 text-center">
            <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无可接订单</h3>
            <p className="text-gray-600">当前没有待配送的订单，请稍后再试</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {orders.map((order) => (
              <div key={order.id} className="p-6 hover:bg-gray-50 transition-colors">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                      <h3 className="text-lg font-semibold text-gray-900">
                        订单 #{order.orderNumber}
                      </h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusMap[order.status]?.color || 'bg-gray-100 text-gray-800'}`}>
                        {statusMap[order.status]?.label || order.status}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-3">
                      <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-gray-400 mt-0.5" />
                        <div className="text-sm">
                          <div className="font-medium text-gray-700">取餐地址</div>
                          <div className="text-gray-600">{order.restaurantName}</div>
                        </div>
                      </div>
                      
                      <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-red-400 mt-0.5" />
                        <div className="text-sm">
                          <div className="font-medium text-gray-700">送餐地址</div>
                          <div className="text-gray-600">
                            {order.deliveryAddressStreet}, {order.deliveryAddressCity}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-start gap-2">
                        <Clock className="w-4 h-4 text-gray-400 mt-0.5" />
                        <div className="text-sm">
                          <div className="font-medium text-gray-700">预计送达</div>
                          <div className="text-gray-600">{formatDate(order.estimatedDelivery)}</div>
                        </div>
                      </div>

                      <div className="flex items-start gap-2">
                        <DollarSign className="w-4 h-4 text-green-500 mt-0.5" />
                        <div className="text-sm">
                          <div className="font-medium text-gray-700">配送费</div>
                          <div className="text-green-600 font-semibold">¥{order.deliveryFee || 5}</div>
                        </div>
                      </div>
                    </div>

                    {order.specialInstructions && (
                      <div className="bg-yellow-50 border border-yellow-200 rounded p-2 text-sm text-yellow-800 mb-3">
                        <span className="font-medium">备注：</span>
                        {order.specialInstructions}
                      </div>
                    )}
                  </div>

                  <div className="ml-6 flex flex-col gap-2">
                    {statusMap[order.status]?.action && (
                      <button
                        onClick={() => updateOrderStatus(order.id, statusMap[order.status].action)}
                        className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium text-sm whitespace-nowrap"
                      >
                        {statusMap[order.status].actionLabel}
                      </button>
                    )}
                    
                    <button
                      onClick={() => navigate(`/orders/${order.id}`)}
                      className="bg-white text-gray-700 px-6 py-2 rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors text-sm"
                    >
                      查看详情
                    </button>
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

export default DriverDashboard;
