import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, ShoppingBag, ShoppingCart, User, LogOut, Menu, X } from 'lucide-react';
import useAuthStore from '../store/authStore';
import useNotificationStore from '../store/notificationStore';
import useCartStore from '../store/cartStore';
import websocketService from '../services/websocket';
import { notificationService } from '../services/apiService';
import toast from 'react-hot-toast';

const Navbar = () => {
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { user, isAuthenticated, logout } = useAuthStore();
  const { unreadCount, setNotifications, addNotification, setConnected, isConnected } = useNotificationStore();
  const { getTotalItems } = useCartStore();

  useEffect(() => {
    if (isAuthenticated && user?.email) {
      // Connect WebSocket
      websocketService.connect(
        user.email,
        (notification) => {
          addNotification(notification);
          toast.success(`新通知: ${notification.title}`, {
            icon: '🔔',
          });
        },
        () => {
          setConnected(true);
          console.log('WebSocket connected');
        },
        () => {
          setConnected(false);
          console.log('WebSocket disconnected');
        }
      );

      // Load initial notifications
      loadNotifications();

      return () => {
        websocketService.disconnect();
      };
    }
  }, [isAuthenticated, user?.email]);

  const loadNotifications = async () => {
    try {
      const notifications = await notificationService.getAll();
      setNotifications(notifications);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    }
  };

  const handleLogout = () => {
    websocketService.disconnect();
    logout();
    navigate('/login');
    toast.success('已退出登录');
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <nav className="bg-white shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <div className="w-10 h-10 bg-gradient-to-br from-primary-500 to-primary-700 rounded-lg flex items-center justify-center">
              <ShoppingBag className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">DoorDash</span>
          </Link>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-8">
            <Link to="/" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
              首页
            </Link>
            <Link to="/restaurants" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
              餐厅
            </Link>
            <Link to="/orders" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
              我的订单
            </Link>
          </div>

          {/* Right Side Icons */}
          <div className="flex items-center space-x-4">
            {/* WebSocket Status */}
            <div className="hidden md:flex items-center">
              <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-gray-300'}`} />
              <span className="ml-2 text-xs text-gray-500">
                {isConnected ? '已连接' : '未连接'}
              </span>
            </div>

            {/* Notifications */}
            <Link
              to="/notifications"
              className="relative p-2 text-gray-600 hover:text-primary-600 transition-colors"
            >
              <Bell className="w-6 h-6" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center animate-bounce-subtle">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </Link>

            {/* Shopping Cart */}
            <Link
              to="/cart"
              className="relative p-2 text-gray-600 hover:text-primary-600 transition-colors"
            >
              <ShoppingCart className="w-6 h-6" />
              {getTotalItems() > 0 && (
                <span className="absolute -top-1 -right-1 bg-primary-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {getTotalItems() > 99 ? '99+' : getTotalItems()}
                </span>
              )}
            </Link>

            {/* User Menu */}
            <div className="hidden md:flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <User className="w-5 h-5 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">
                  {user?.firstName} {user?.lastName}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-600 hover:text-red-600 transition-colors"
                title="退出登录"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>

            {/* Mobile Menu Button */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden p-2 text-gray-600"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-gray-200 animate-fade-in">
            <Link
              to="/"
              className="block py-2 text-gray-700 hover:text-primary-600"
              onClick={() => setMobileMenuOpen(false)}
            >
              首页
            </Link>
            <Link
              to="/restaurants"
              className="block py-2 text-gray-700 hover:text-primary-600"
              onClick={() => setMobileMenuOpen(false)}
            >
              餐厅
            </Link>
            <Link
              to="/orders"
              className="block py-2 text-gray-700 hover:text-primary-600"
              onClick={() => setMobileMenuOpen(false)}
            >
              我的订单
            </Link>
            <div className="pt-4 mt-4 border-t border-gray-200">
              <div className="flex items-center space-x-2 py-2">
                <User className="w-5 h-5 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">
                  {user?.firstName} {user?.lastName}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-2 py-2 text-red-600"
              >
                <LogOut className="w-5 h-5" />
                <span>退出登录</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
