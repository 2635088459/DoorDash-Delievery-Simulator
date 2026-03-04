import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bike, MapPin, DollarSign, TrendingUp, Award, Package } from 'lucide-react';
import toast from 'react-hot-toast';
import { driverService } from '../services/apiService';

const DriverHome = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    todayDeliveries: 0,
    todayEarnings: 0,
    weekEarnings: 0,
    rating: null,
  });

  useEffect(() => {
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    setUser(storedUser);
  }, []);

  useEffect(() => {
    const loadStats = async () => {
      try {
        setLoading(true);
        const [earnings, driver] = await Promise.all([
          driverService.getEarnings(),
          driverService.getMe(),
        ]);
        setStats({
          todayDeliveries: earnings?.todayDeliveries ?? 0,
          todayEarnings: Number(earnings?.todayEarnings ?? 0),
          weekEarnings: Number(earnings?.weekEarnings ?? 0),
          rating: driver?.rating ?? null,
        });
      } catch (error) {
        console.error('Failed to load driver stats:', error);
  toast.error(error.response?.data?.message || 'Failed to load driver data');
      } finally {
        setLoading(false);
      }
    };

    loadStats();
  }, []);

  const quickActions = [
    {
      icon: MapPin,
  title: 'Start accepting',
  description: 'View available orders',
      color: 'bg-blue-500',
      hoverColor: 'hover:bg-blue-600',
      link: '/driver-dashboard'
    },
    {
      icon: Package,
  title: 'My deliveries',
  description: 'View current deliveries',
      color: 'bg-green-500',
      hoverColor: 'hover:bg-green-600',
      link: '/driver-deliveries'
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Welcome Header */}
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-2xl shadow-xl p-8 mb-8 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold mb-2">
                Ready to take orders, {user?.firstName || 'Driver'}? 🚗
              </h1>
              <p className="text-blue-100 text-lg">
                Start delivering and earn more
              </p>
            </div>
            <Bike className="w-24 h-24 opacity-20" />
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-blue-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Deliveries today</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  {loading ? '--' : stats.todayDeliveries}
                </p>
                <p className="text-xs text-gray-400 mt-1">orders</p>
              </div>
              <MapPin className="w-12 h-12 text-blue-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-green-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Earnings today</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  ¥{loading ? '--' : stats.todayEarnings.toFixed(2)}
                </p>
                <p className="text-xs text-gray-400 mt-1">delivery fees</p>
              </div>
              <DollarSign className="w-12 h-12 text-green-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-yellow-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Earnings this week</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  ¥{loading ? '--' : stats.weekEarnings.toFixed(2)}
                </p>
                <p className="text-xs text-gray-400 mt-1">total</p>
              </div>
              <TrendingUp className="w-12 h-12 text-yellow-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-purple-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">Driver rating</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">
                  {loading ? '--' : stats.rating?.toFixed(1) ?? '--'}
                </p>
                <p className="text-xs text-gray-400 mt-1">out of 5.0</p>
              </div>
              <Award className="w-12 h-12 text-purple-500 opacity-20" />
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Quick actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                      <h3 className="text-lg font-semibold text-gray-900 mb-1 group-hover:text-blue-600 transition-colors">
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
        <div className="mt-8 bg-green-50 border border-green-200 rounded-xl p-6">
          <h3 className="text-lg font-semibold text-green-900 mb-3">💡 Delivery tips</h3>
          <ul className="space-y-2 text-green-800">
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Pick up promptly to keep food fresh</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Use navigation to choose the best route</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Be polite and provide great service</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>Prioritize road safety</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default DriverHome;
