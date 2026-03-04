import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingBag, Truck, Bell, Star } from 'lucide-react';
import useAuthStore from '../store/authStore';
import useNotificationStore from '../store/notificationStore';

const Home = () => {
  const user = useAuthStore((state) => state.user);
  const { unreadCount, isConnected } = useNotificationStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      navigate('/admin/tickets', { replace: true });
    }
  }, [user?.role, navigate]);

  if (user?.role === 'ADMIN') {
    return null;
  }

  const features = [
    {
      icon: ShoppingBag,
      title: 'Browse restaurants',
      description: 'Discover tasty restaurants nearby',
      color: 'from-blue-500 to-blue-600',
      link: '/restaurants',
    },
    {
      icon: Truck,
      title: 'My orders',
      description: 'View and track your orders',
      color: 'from-green-500 to-green-600',
      link: '/orders',
    },
    {
      icon: Bell,
      title: 'Notifications',
      description: `${unreadCount} unread notifications`,
      color: 'from-purple-500 to-purple-600',
      link: '/notifications',
    },
    {
      icon: Star,
      title: 'Member perks',
      description: 'Enjoy more deals and perks',
      color: 'from-yellow-500 to-yellow-600',
      link: '#',
    },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Welcome Section */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          Welcome back, {user?.firstName}! 👋
        </h1>
        <p className="text-xl text-gray-600">
          What would you like to eat?
        </p>
        
        {/* WebSocket Status */}
        <div className="mt-4 inline-flex items-center space-x-2 px-4 py-2 bg-white rounded-full shadow-md">
          <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-gray-300'}`} />
          <span className="text-sm font-medium text-gray-700">
            {isConnected ? 'Live notifications connected' : 'Connecting...'}
          </span>
        </div>
      </div>

      {/* Features Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
        {features.map((feature, index) => (
          <Link
            key={index}
            to={feature.link}
            className="bg-white rounded-2xl shadow-lg p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1"
          >
            <div className={`w-12 h-12 bg-gradient-to-br ${feature.color} rounded-xl flex items-center justify-center mb-4`}>
              <feature.icon className="w-6 h-6 text-white" />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">
              {feature.title}
            </h3>
            <p className="text-sm text-gray-600">
              {feature.description}
            </p>
          </Link>
        ))}
      </div>

      {/* Quick Actions */}
      <div className="bg-gradient-to-br from-primary-500 to-primary-700 rounded-2xl shadow-xl p-8 text-white">
        <div className="flex flex-col md:flex-row items-center justify-between">
          <div className="mb-6 md:mb-0">
            <h2 className="text-2xl font-bold mb-2">
              Ready to order?
            </h2>
            <p className="text-primary-100">
              Browse nearby restaurants and discover great food
            </p>
          </div>
          <Link
            to="/restaurants"
            className="px-8 py-3 bg-white text-primary-600 rounded-lg font-bold hover:bg-gray-100 transition-colors shadow-lg"
          >
            Browse restaurants
          </Link>
        </div>
      </div>

      {/* Stats Section */}
      <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl shadow-md p-6 text-center">
          <div className="text-3xl font-bold text-primary-600 mb-2">
            {unreadCount}
          </div>
          <div className="text-gray-600">Unread notifications</div>
        </div>
        <div className="bg-white rounded-xl shadow-md p-6 text-center">
          <div className="text-3xl font-bold text-green-600 mb-2">
            {isConnected ? 'Online' : 'Offline'}
          </div>
          <div className="text-gray-600">Connection status</div>
        </div>
        <div className="bg-white rounded-xl shadow-md p-6 text-center">
          <div className="text-3xl font-bold text-blue-600 mb-2">
            ⭐⭐⭐⭐⭐
          </div>
          <div className="text-gray-600">Membership tier</div>
        </div>
      </div>

      {/* Info Section */}
      <div className="mt-12 bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded-lg">
        <div className="flex">
          <div className="flex-shrink-0">
            <Bell className="h-5 w-5 text-yellow-400" />
          </div>
          <div className="ml-3">
            <p className="text-sm text-yellow-700">
              <strong>Real-time notifications are enabled!</strong> You'll receive live updates on order status, delivery, and more.
              {unreadCount > 0 && ` You have ${unreadCount} unread notifications.`}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
