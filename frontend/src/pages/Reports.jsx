import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderService, restaurantService } from '../services/apiService';

const getDateRange = (rangeKey) => {
  const end = new Date();
  end.setHours(23, 59, 59, 999);

  const start = new Date();
  start.setHours(0, 0, 0, 0);

  if (rangeKey === '7d') {
    start.setDate(start.getDate() - 6);
  }
  if (rangeKey === '30d') {
    start.setDate(start.getDate() - 29);
  }

  return { start, end };
};

const formatDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
  });
};

const Reports = () => {
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [range, setRange] = useState('7d');

  useEffect(() => {
    const loadReports = async () => {
      try {
        setLoading(true);
        const ownerRestaurant = await restaurantService.getOwnerRestaurant();
        setRestaurant(ownerRestaurant);
        const orderList = await orderService.getRestaurantOrders(ownerRestaurant.id);
        setOrders(orderList);
        setError('');
      } catch (err) {
        console.error('Failed to load reports:', err);
        setError(err.response?.data?.message || '加载报表失败');
      } finally {
        setLoading(false);
      }
    };

    loadReports();
  }, []);

  const filteredOrders = useMemo(() => {
    const { start, end } = getDateRange(range);
    return orders.filter((order) => {
      if (!order.createdAt) return false;
      const created = new Date(order.createdAt);
      return created >= start && created <= end;
    });
  }, [orders, range]);

  const summary = useMemo(() => {
    const totalOrders = filteredOrders.length;
    const totalRevenue = filteredOrders.reduce(
      (sum, order) => sum + Number(order.totalAmount || 0),
      0
    );
    const averageOrder = totalOrders === 0 ? 0 : totalRevenue / totalOrders;

    const itemMap = new Map();
    filteredOrders.forEach((order) => {
      order.items?.forEach((item) => {
        const key = item.menuItemName || '未知菜品';
        const current = itemMap.get(key) || { quantity: 0, revenue: 0 };
        itemMap.set(key, {
          quantity: current.quantity + Number(item.quantity || 0),
          revenue: current.revenue + Number(item.subtotal || 0),
        });
      });
    });

    const topItems = Array.from(itemMap.entries())
      .map(([name, value]) => ({ name, ...value }))
      .sort((a, b) => b.revenue - a.revenue)
      .slice(0, 5);

    const dailyMap = new Map();
    filteredOrders.forEach((order) => {
      const dayKey = formatDate(order.createdAt);
      const current = dailyMap.get(dayKey) || { orders: 0, revenue: 0 };
      dailyMap.set(dayKey, {
        orders: current.orders + 1,
        revenue: current.revenue + Number(order.totalAmount || 0),
      });
    });

    const dailyStats = Array.from(dailyMap.entries())
      .map(([date, value]) => ({ date, ...value }))
      .sort((a, b) => (a.date > b.date ? 1 : -1));

    return { totalOrders, totalRevenue, averageOrder, topItems, dailyStats };
  }, [filteredOrders]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>
    );
  }

  if (error && !restaurant) {
    return (
      <div className="max-w-5xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => navigate('/restaurant-home')}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            返回餐厅首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8 mb-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">营业报表</h1>
            <p className="text-gray-600">{restaurant?.name} · {restaurant?.city}</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setRange('today')}
              className={`px-4 py-2 rounded-lg border ${range === 'today' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              今日
            </button>
            <button
              onClick={() => setRange('7d')}
              className={`px-4 py-2 rounded-lg border ${range === '7d' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              近7天
            </button>
            <button
              onClick={() => setRange('30d')}
              className={`px-4 py-2 rounded-lg border ${range === '30d' ? 'bg-red-600 text-white border-red-600' : 'bg-white text-gray-700 border-gray-300'}`}
            >
              近30天
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">订单数量</div>
          <div className="text-3xl font-bold text-gray-900 mt-2">{summary.totalOrders}</div>
        </div>
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">总营业额</div>
          <div className="text-3xl font-bold text-gray-900 mt-2">¥{summary.totalRevenue.toFixed(2)}</div>
        </div>
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="text-sm text-gray-500">平均客单价</div>
          <div className="text-3xl font-bold text-gray-900 mt-2">¥{summary.averageOrder.toFixed(2)}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">热门菜品</h2>
          {summary.topItems.length === 0 ? (
            <p className="text-gray-500">暂无数据</p>
          ) : (
            <div className="space-y-3">
              {summary.topItems.map((item, index) => (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="text-gray-700">
                    {index + 1}. {item.name}
                  </div>
                  <div className="text-sm text-gray-500">{item.quantity} 份 · ¥{item.revenue.toFixed(2)}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">每日趋势</h2>
          {summary.dailyStats.length === 0 ? (
            <p className="text-gray-500">暂无数据</p>
          ) : (
            <div className="space-y-3">
              {summary.dailyStats.map((day) => (
                <div key={day.date} className="flex items-center justify-between">
                  <div className="text-gray-700">{day.date}</div>
                  <div className="text-sm text-gray-500">{day.orders} 单 · ¥{day.revenue.toFixed(2)}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="mt-6">
        <button
          onClick={() => navigate('/restaurant-home')}
          className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
        >
          返回餐厅首页
        </button>
      </div>
    </div>
  );
};

export default Reports;
