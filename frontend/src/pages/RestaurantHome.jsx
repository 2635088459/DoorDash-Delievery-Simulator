import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, ShoppingBag, Clock, Gift, TrendingUp, Users } from 'lucide-react';
import { menuItemService, orderService, restaurantService } from '../services/apiService';

const weekDayKeys = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];

const parseJsonValue = (value, fallback) => {
  if (!value) return fallback;
  try {
    return JSON.parse(value);
  } catch (err) {
    console.warn('Failed to parse schedule JSON:', err);
    return fallback;
  }
};

const countOpenDays = (schedule = {}) => {
  return weekDayKeys.reduce((count, key) => {
    if (schedule[key]?.isClosed) {
      return count;
    }
    return count + 1;
  }, 0);
};

const countRestDaysInWeek = (restDays = []) => {
  if (!Array.isArray(restDays) || restDays.length === 0) return 0;
  const today = new Date();
  const day = today.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  const weekStart = new Date(today);
  weekStart.setDate(today.getDate() + diffToMonday);
  weekStart.setHours(0, 0, 0, 0);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 6);

  return restDays.reduce((count, value) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return count;
    if (date >= weekStart && date <= weekEnd) return count + 1;
    return count;
  }, 0);
};

const RestaurantHome = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [stats, setStats] = useState({
    todayOrders: 0,
    todayRevenue: 0,
    menuCount: 0,
    rating: null,
    weeklyOpenDays: null,
  });
  const [hasRestaurant, setHasRestaurant] = useState(true);
  const [setupMessage, setSetupMessage] = useState('');
  const [restaurants, setRestaurants] = useState([]);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);

  const loadStatsForRestaurant = async (restaurant) => {
    const [menuItems, orders] = await Promise.all([
      menuItemService.getAllByRestaurant(restaurant.id),
      orderService.getRestaurantOrders(restaurant.id),
    ]);

    const weeklySchedule = parseJsonValue(restaurant.weeklyScheduleJson, {});
    const restDays = parseJsonValue(restaurant.restDaysJson, []);
    const openDays = countOpenDays(weeklySchedule);
    const restDaysThisWeek = countRestDaysInWeek(restDays);
    const weeklyOpenDays = Math.max(openDays - restDaysThisWeek, 0);

    const today = new Date().toDateString();
    const todayOrders = orders.filter((order) => {
      if (!order.createdAt) return false;
      return new Date(order.createdAt).toDateString() === today;
    });

    const todayRevenue = todayOrders.reduce(
      (sum, order) => sum + Number(order.totalAmount || 0),
      0
    );

    setStats({
      todayOrders: todayOrders.length,
      todayRevenue,
      menuCount: menuItems.length,
      rating: restaurant.rating ?? null,
      weeklyOpenDays,
    });
  };

  useEffect(() => {
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    setUser(storedUser);
  }, []);

  const handleRestaurantChange = (event) => {
    const nextId = Number(event.target.value);
    if (Number.isNaN(nextId)) return;
    const nextRestaurant = restaurants.find((item) => item.id === nextId);
    if (!nextRestaurant) return;
    setSelectedRestaurantId(nextId);
    restaurantService.setActiveRestaurantId(nextId);
    loadStatsForRestaurant(nextRestaurant);
  };

  useEffect(() => {
    const loadStats = async () => {
      try {
        const ownerRestaurants = await restaurantService.getMyRestaurants();
        setRestaurants(ownerRestaurants);
        if (!ownerRestaurants.length) {
          setHasRestaurant(false);
          setSetupMessage("You haven't created a restaurant yet. Please complete the setup first.");
          return;
        }

        setHasRestaurant(true);
        setSetupMessage('');

        const activeId = restaurantService.getActiveRestaurantId();
        const activeRestaurant =
          ownerRestaurants.find((item) => item.id === activeId) || ownerRestaurants[0];

        setSelectedRestaurantId(activeRestaurant.id);
        restaurantService.setActiveRestaurantId(activeRestaurant.id);

        await loadStatsForRestaurant(activeRestaurant);
      } catch (error) {
        console.error('Failed to load restaurant stats:', error);
        if (error.response?.status === 404) {
          setHasRestaurant(false);
          setSetupMessage("You haven't created a restaurant yet. Please complete the setup first.");
        }
      }
    };

    loadStats();
  }, []);

  const quickActions = [
    {
      icon: ShoppingBag,
      title: 'Order management',
      description: 'View and handle restaurant orders',
      color: 'bg-blue-500',
      hoverColor: 'hover:bg-blue-600',
      link: '/restaurant-management'
    },
    {
      icon: Store,
      title: 'Menu management',
      description: 'Add, edit, and remove menu items',
      color: 'bg-green-500',
      hoverColor: 'hover:bg-green-600',
      link: '/menu-management'
    },
    {
      icon: Clock,
      title: 'Business hours',
      description: 'Set restaurant opening hours',
      color: 'bg-purple-500',
      hoverColor: 'hover:bg-purple-600',
      link: '/business-hours?returnHome=true'
    },
    {
      icon: Gift,
      title: 'Coupons',
      description: 'Create and manage coupons',
      color: 'bg-pink-500',
      hoverColor: 'hover:bg-pink-600',
      link: '/coupons'
    },
    {
      icon: TrendingUp,
      title: 'Sales reports',
      description: 'View sales data and insights',
      color: 'bg-orange-500',
      hoverColor: 'hover:bg-orange-600',
      link: '/reports'
    },
    {
      icon: Users,
      title: 'Customer reviews',
      description: 'View and reply to customer reviews',
      color: 'bg-indigo-500',
      hoverColor: 'hover:bg-indigo-600',
      link: '/reviews'
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Welcome Header */}
        <div className="bg-gradient-to-r from-red-500 to-red-600 rounded-2xl shadow-xl p-8 mb-8 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold mb-2">
                Welcome back, {user?.firstName || 'Restaurant'} 👋
              </h1>
              <p className="text-red-100 text-lg">
                Manage your restaurant and deliver great service
              </p>
            </div>
            <Store className="w-24 h-24 opacity-20" />
          </div>
          {restaurants.length > 0 && (
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                <span className="text-sm text-red-100">Active restaurant</span>
                {restaurants.length > 1 ? (
                  <select
                    value={selectedRestaurantId || ''}
                    onChange={handleRestaurantChange}
                    className="bg-white/90 text-gray-800 border border-white/70 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-white"
                  >
                    {restaurants.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                ) : (
                  <span className="text-sm font-semibold text-white">
                    {restaurants[0]?.name}
                  </span>
                )}
              </div>
              <button
                onClick={() => navigate('/restaurant-setup')}
                className="bg-white/90 text-red-600 px-3 py-2 rounded-lg text-sm font-semibold hover:bg-white"
              >
                Add restaurant
              </button>
            </div>
          )}
        </div>

        {/* Quick Stats */}
        {!hasRestaurant && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-6 mb-8">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div>
                <div className="text-lg font-semibold text-yellow-900">No restaurant yet</div>
                <div className="text-yellow-800 mt-1">{setupMessage}</div>
              </div>
              <button
                onClick={() => navigate('/restaurant-setup')}
                className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
              >
                Create restaurant
              </button>
            </div>
          </div>
        )}
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-blue-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Orders today</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">{stats.todayOrders}</p>
              </div>
              <ShoppingBag className="w-12 h-12 text-blue-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-green-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Revenue today</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">¥{stats.todayRevenue.toFixed(2)}</p>
              </div>
              <TrendingUp className="w-12 h-12 text-green-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-purple-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Menu items</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">{stats.menuCount}</p>
              </div>
              <Store className="w-12 h-12 text-purple-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-orange-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Customer rating</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  {stats.rating === null ? '--' : Number(stats.rating).toFixed(1)}
                </p>
              </div>
              <Users className="w-12 h-12 text-orange-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-emerald-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Open days this week</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  {stats.weeklyOpenDays === null ? '--' : stats.weeklyOpenDays}
                </p>
              </div>
              <Clock className="w-12 h-12 text-emerald-500 opacity-20" />
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Quick actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {quickActions.map((action, index) => {
              const Icon = action.icon;
              return (
                <button
                  key={index}
                  onClick={() => navigate(action.link)}
                  className="bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 text-left group"
                >
                  <div className="flex items-start space-x-4">
                    <div className={`${action.color} ${action.hoverColor} rounded-lg p-3 group-hover:scale-110 transition-transform`}>
                      <Icon className="w-8 h-8 text-white" />
                    </div>
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-900 mb-1 group-hover:text-red-600 transition-colors">
                        {action.title}
                      </h3>
                      <p className="text-sm text-gray-600">
                        {action.description}
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Tips Section */}
        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-xl p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-3">💡 Operations tips</h3>
          <ul className="space-y-2 text-blue-800">
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Handle orders promptly to improve customer satisfaction</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Refresh your menu regularly and launch new items</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Run coupon campaigns to boost sales</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Monitor reviews and keep improving service</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default RestaurantHome;
