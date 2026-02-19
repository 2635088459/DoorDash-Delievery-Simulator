import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { MapPin, Package, Truck, Clock } from 'lucide-react';
import { deliveryService } from '../services/apiService';

const statusMap = {
  READY_FOR_PICKUP: { label: '待取餐', color: 'bg-orange-100 text-orange-800', action: 'pickedUp', actionLabel: '已取餐' },
  PICKED_UP: { label: '已取餐', color: 'bg-blue-100 text-blue-800', action: 'inTransit', actionLabel: '开始配送' },
  IN_TRANSIT: { label: '配送中', color: 'bg-purple-100 text-purple-800', action: 'delivered', actionLabel: '完成配送' },
  DELIVERED: { label: '已送达', color: 'bg-green-100 text-green-800' },
};

const DriverDeliveries = () => {
  const navigate = useNavigate();
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

    loadDeliveries();
  }, [navigate]);

  const loadDeliveries = async () => {
    try {
      setLoading(true);
      const response = await deliveryService.getMyDeliveries();
      setDeliveries(response || []);
      setError('');
    } catch (err) {
      console.error('Failed to load deliveries:', err);
      setError(err.response?.data?.message || '获取配送订单失败');
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (orderId, action) => {
    try {
      if (action === 'pickedUp') {
        await deliveryService.markPickedUp(orderId);
      } else if (action === 'inTransit') {
        await deliveryService.markInTransit(orderId);
      } else if (action === 'delivered') {
        await deliveryService.markDelivered(orderId);
      }
      toast.success('配送状态已更新');
      await loadDeliveries();
    } catch (err) {
      console.error('Failed to update delivery status:', err);
      toast.error(err.response?.data?.message || '更新配送状态失败');
    }
  };

  const currentDeliveries = useMemo(
    () => deliveries.filter((delivery) => delivery.orderStatus !== 'DELIVERED'),
    [deliveries]
  );

  const completedDeliveries = useMemo(
    () => deliveries.filter((delivery) => delivery.orderStatus === 'DELIVERED'),
    [deliveries]
  );

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getMapUrl = (origin, destination) => {
    if (!origin || !destination) return null;
    const originEncoded = encodeURIComponent(origin);
    const destinationEncoded = encodeURIComponent(destination);
    return `https://www.google.com/maps/dir/?api=1&origin=${originEncoded}&destination=${destinationEncoded}`;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error && deliveries.length === 0) {
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
            <h1 className="text-3xl font-bold">我的配送</h1>
            <p className="mt-1 opacity-90">跟踪当前配送进度</p>
          </div>
          <div className="text-right">
            <Truck className="w-12 h-12 mb-2 mx-auto" />
            <div className="text-sm opacity-90">配送中</div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-lg shadow-md">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-900">当前配送</h2>
              <p className="text-gray-600 text-sm mt-1">进行中的订单</p>
            </div>

            {currentDeliveries.length === 0 ? (
              <div className="p-10 text-center">
                <Package className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-600">暂无进行中的配送</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {currentDeliveries.map((delivery) => (
                  <div key={delivery.orderId} className="p-6">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-3">
                          <h3 className="text-lg font-semibold text-gray-900">订单 #{delivery.orderNumber}</h3>
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusMap[delivery.orderStatus]?.color || 'bg-gray-100 text-gray-800'}`}>
                            {statusMap[delivery.orderStatus]?.label || delivery.orderStatus}
                          </span>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                          <div className="flex items-start gap-2">
                            <MapPin className="w-4 h-4 text-gray-400 mt-0.5" />
                            <div>
                              <div className="text-sm font-medium text-gray-900">取餐地址</div>
                              <div className="text-sm text-gray-600">{delivery.restaurantName}</div>
                              <div className="text-sm text-gray-600">{delivery.restaurantAddress}</div>
                            </div>
                          </div>
                          <div className="flex items-start gap-2">
                            <MapPin className="w-4 h-4 text-red-400 mt-0.5" />
                            <div>
                              <div className="text-sm font-medium text-gray-900">送餐地址</div>
                              <div className="text-sm text-gray-600">
                                {delivery.deliveryStreet} {delivery.deliveryCity} {delivery.deliveryState} {delivery.deliveryZipCode}
                              </div>
                              <div className="text-sm text-gray-600">{delivery.customerName}</div>
                            </div>
                          </div>
                        </div>
                        {delivery.specialInstructions && (
                          <div className="text-sm text-orange-600 mb-2">备注：{delivery.specialInstructions}</div>
                        )}
                        <div className="flex flex-wrap items-center gap-4 text-sm text-gray-600">
                          <span>下单时间: {formatDate(delivery.createdAt)}</span>
                          <span>预计送达: {formatDate(delivery.estimatedDelivery)}</span>
                        </div>
                      </div>
                      <div className="flex flex-col gap-2">
                        {getMapUrl(
                          delivery.restaurantAddress,
                          `${delivery.deliveryStreet} ${delivery.deliveryCity} ${delivery.deliveryState} ${delivery.deliveryZipCode}`
                        ) && (
                          <a
                            href={getMapUrl(
                              delivery.restaurantAddress,
                              `${delivery.deliveryStreet} ${delivery.deliveryCity} ${delivery.deliveryState} ${delivery.deliveryZipCode}`
                            )}
                            target="_blank"
                            rel="noreferrer"
                            className="px-4 py-2 bg-white text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-center"
                          >
                            查看路线
                          </a>
                        )}
                        {statusMap[delivery.orderStatus]?.action && (
                          <button
                            onClick={() => updateStatus(delivery.orderId, statusMap[delivery.orderStatus].action)}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                          >
                            {statusMap[delivery.orderStatus].actionLabel}
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

        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow-md">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-bold text-gray-900">今日完成</h2>
            </div>
            <div className="p-6">
              {completedDeliveries.length === 0 ? (
                <p className="text-sm text-gray-600">暂无已完成配送</p>
              ) : (
                <ul className="space-y-4">
                  {completedDeliveries.slice(0, 5).map((delivery) => (
                    <li key={delivery.orderId} className="text-sm text-gray-600">
                      <div className="font-medium text-gray-900">订单 #{delivery.orderNumber}</div>
                      <div>完成时间: {formatDate(delivery.actualDelivery)}</div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
            <div className="flex items-start gap-3">
              <Clock className="w-5 h-5 text-blue-600" />
              <div>
                <div className="text-sm font-semibold text-blue-900">配送提醒</div>
                <p className="text-sm text-blue-800 mt-1">确保及时取餐并安全送达。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DriverDeliveries;
