import { useMemo, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { MapPin, DollarSign, Package, Filter } from 'lucide-react';
import { deliveryService } from '../services/apiService';

const statusMap = {
  READY_FOR_PICKUP: { label: '待取餐', color: 'bg-orange-100 text-orange-800' },
  PICKED_UP: { label: '已取餐', color: 'bg-blue-100 text-blue-800' },
  IN_TRANSIT: { label: '配送中', color: 'bg-purple-100 text-purple-800' },
  DELIVERED: { label: '已送达', color: 'bg-green-100 text-green-800' }
};

const DriverDashboard = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortKey, setSortKey] = useState('createdDesc');
  const [minFee, setMinFee] = useState('');
  const [maxFee, setMaxFee] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  useEffect(() => {
    const token = localStorage.getItem('token');
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    if (!token) {
      navigate('/login');
      return;
    }

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
      const response = await deliveryService.getAvailable();
      setOrders(response || []);
      setError('');
    } catch (err) {
      console.error('Failed to fetch orders:', err);
      setError(err.response?.data?.message || '获取订单列表失败');
    } finally {
      setLoading(false);
    }
  };

  const acceptOrder = async (orderId) => {
    try {
      await deliveryService.acceptOrder(orderId);
      toast.success('接单成功');
      await fetchAvailableOrders();
    } catch (err) {
      console.error('Failed to accept order:', err);
      toast.error(err.response?.data?.message || '接单失败');
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

  const getMapUrl = (origin, destination) => {
    if (!origin || !destination) return null;
    const originEncoded = encodeURIComponent(origin);
    const destinationEncoded = encodeURIComponent(destination);
    return `https://www.google.com/maps/dir/?api=1&origin=${originEncoded}&destination=${destinationEncoded}`;
  };

  const filteredOrders = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase();
  const minFeeValue = minFee === '' ? null : Number(minFee);
  const maxFeeValue = maxFee === '' ? null : Number(maxFee);
    let results = [...orders];

    if (keyword) {
      results = results.filter((order) => {
        const content = [
          order.restaurantName,
          order.restaurantAddress,
          order.customerName,
          order.deliveryStreet,
          order.deliveryCity,
          order.deliveryState,
          order.deliveryZipCode,
          order.orderNumber,
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return content.includes(keyword);
      });
    }

    if (minFeeValue !== null && !Number.isNaN(minFeeValue)) {
      results = results.filter((order) => Number(order.deliveryFee || 0) >= minFeeValue);
    }

    if (maxFeeValue !== null && !Number.isNaN(maxFeeValue)) {
      results = results.filter((order) => Number(order.deliveryFee || 0) <= maxFeeValue);
    }

    if (statusFilter !== 'ALL') {
      results = results.filter((order) => order.orderStatus === statusFilter);
    }

    switch (sortKey) {
      case 'feeDesc':
        results.sort((a, b) => Number(b.deliveryFee || 0) - Number(a.deliveryFee || 0));
        break;
      case 'feeAsc':
        results.sort((a, b) => Number(a.deliveryFee || 0) - Number(b.deliveryFee || 0));
        break;
      case 'createdAsc':
        results.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        break;
      default:
        results.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    return results;
  }, [orders, searchTerm, sortKey, minFee, maxFee, statusFilter]);

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
      <div className="bg-gradient-to-r from-blue-500 to-blue-700 rounded-lg shadow-md p-6 mb-6 text-white">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">可接订单列表</h1>
            <p className="mt-1 opacity-90">查看附近可接订单并快速接单</p>
          </div>
          <div className="text-right">
            <Package className="w-12 h-12 mb-2 mx-auto" />
            <div className="text-sm opacity-90">在线接单中</div>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div className="flex items-center gap-2 text-gray-700">
            <Filter className="w-4 h-4" />
            <span className="text-sm font-medium">筛选/排序</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-5 gap-3 w-full md:w-auto">
            <input
              type="text"
              placeholder="搜索餐厅/客户/地址"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="number"
              min="0"
              step="0.1"
              placeholder="最低配送费"
              value={minFee}
              onChange={(event) => setMinFee(event.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="number"
              min="0"
              step="0.1"
              placeholder="最高配送费"
              value={maxFee}
              onChange={(event) => setMaxFee(event.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            />
            <select
              value={sortKey}
              onChange={(event) => setSortKey(event.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              <option value="createdDesc">最新订单</option>
              <option value="createdAsc">最早订单</option>
              <option value="feeDesc">配送费从高到低</option>
              <option value="feeAsc">配送费从低到高</option>
            </select>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">全部状态</option>
              <option value="READY_FOR_PICKUP">待取餐</option>
              <option value="PICKED_UP">已取餐</option>
              <option value="IN_TRANSIT">配送中</option>
              <option value="DELIVERED">已送达</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-900">可配送订单</h2>
          <p className="text-gray-600 text-sm mt-1">接单并完成配送任务</p>
        </div>

        {filteredOrders.length === 0 ? (
          <div className="p-12 text-center">
            <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">暂无可接订单</h3>
            <p className="text-gray-600">当前没有待配送的订单，请稍后再试</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200">
            {filteredOrders.map((order) => (
              <div key={order.orderId} className="p-6 hover:bg-gray-50 transition-colors">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                      <h3 className="text-lg font-semibold text-gray-900">订单 #{order.orderNumber}</h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusMap[order.orderStatus]?.color || 'bg-gray-100 text-gray-800'}`}>
                        {statusMap[order.orderStatus]?.label || order.orderStatus}
                      </span>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                      <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-gray-400 mt-0.5" />
                        <div>
                          <div className="text-sm font-medium text-gray-900">取餐地址</div>
                          <div className="text-sm text-gray-600">{order.restaurantName}</div>
                          <div className="text-sm text-gray-600">{order.restaurantAddress}</div>
                        </div>
                      </div>
                      <div className="flex items-start gap-2">
                        <MapPin className="w-4 h-4 text-red-400 mt-0.5" />
                        <div>
                          <div className="text-sm font-medium text-gray-900">送餐地址</div>
                          <div className="text-sm text-gray-600">
                            {order.deliveryStreet} {order.deliveryCity} {order.deliveryState} {order.deliveryZipCode}
                          </div>
                          <div className="text-sm text-gray-600">{order.customerName}</div>
                        </div>
                      </div>
                    </div>

                    {order.specialInstructions && (
                      <div className="text-sm text-orange-600 mb-2">备注：{order.specialInstructions}</div>
                    )}

                    <div className="flex flex-wrap items-center gap-4 text-sm text-gray-600">
                      <span>下单时间: {formatDate(order.createdAt)}</span>
                      <div className="flex items-center gap-1">
                        <DollarSign className="w-4 h-4" />
                        <span>配送费: ¥{Number(order.deliveryFee || 0).toFixed(2)}</span>
                      </div>
                      <span>订单金额: ¥{Number(order.totalAmount || 0).toFixed(2)}</span>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2">
                    {getMapUrl(order.restaurantAddress, `${order.deliveryStreet} ${order.deliveryCity} ${order.deliveryState} ${order.deliveryZipCode}`) && (
                      <a
                        href={getMapUrl(
                          order.restaurantAddress,
                          `${order.deliveryStreet} ${order.deliveryCity} ${order.deliveryState} ${order.deliveryZipCode}`
                        )}
                        target="_blank"
                        rel="noreferrer"
                        className="px-4 py-2 bg-white text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-center"
                      >
                        查看路线
                      </a>
                    )}
                    <button
                      onClick={() => acceptOrder(order.orderId)}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      接单
                    </button>
                    <button
                      onClick={() => navigate(`/orders/${order.orderId}`)}
                      className="px-4 py-2 bg-white text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                      订单详情
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
