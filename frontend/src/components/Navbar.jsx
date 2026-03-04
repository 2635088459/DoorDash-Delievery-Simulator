import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, ShoppingBag, ShoppingCart, User, LogOut, Menu, X, ShieldCheck, Trash2 } from 'lucide-react';
import useAuthStore from '../store/authStore';
import useNotificationStore from '../store/notificationStore';
import useCartStore from '../store/cartStore';
import websocketService from '../services/websocket';
import { authService, notificationService } from '../services/apiService';
import toast from 'react-hot-toast';

const Navbar = () => {
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { user, isAuthenticated, logout } = useAuthStore();
  const { unreadCount, setNotifications, addNotification, setConnected, isConnected } = useNotificationStore();
  const { getTotalItems } = useCartStore();
  const [tokenRemaining, setTokenRemaining] = useState('');

  useEffect(() => {
    if (isAuthenticated && user?.email) {
      // Connect WebSocket
      websocketService.connect(
        user.email,
        (notification) => {
          addNotification(notification);
          toast.success(`New notification: ${notification.title}`, {
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

  useEffect(() => {
    if (!isAuthenticated) return undefined;

    const updateTokenRemaining = () => {
      const rawToken = localStorage.getItem('token') || localStorage.getItem('accessToken');
      if (!rawToken || rawToken === 'undefined' || rawToken === 'null') {
        setTokenRemaining('Not signed in');
        return;
      }

      try {
        const payload = JSON.parse(atob(rawToken.split('.')[1] || ''));
        if (!payload?.exp) {
          setTokenRemaining('Unknown');
          return;
        }
        const diffMs = payload.exp * 1000 - Date.now();
        if (diffMs <= 0) {
          setTokenRemaining('Expired');
          return;
        }
        const minutes = Math.floor(diffMs / 60000);
        const seconds = Math.floor((diffMs % 60000) / 1000);
        setTokenRemaining(`${minutes}m ${seconds}s`);
      } catch (error) {
        setTokenRemaining('Parse error');
      }
    };

    updateTokenRemaining();
    const timer = setInterval(updateTokenRemaining, 1000);
    return () => clearInterval(timer);
  }, [isAuthenticated]);

  const loadNotifications = async () => {
    try {
      const notifications = await notificationService.getAll();
      setNotifications(notifications);
    } catch (error) {
      // 只在非 403 错误时记录（403 表示未登录，这是正常的）
      if (error.response?.status !== 403) {
        console.error('Failed to load notifications:', error);
      }
    }
  };

  const handleLogout = () => {
    websocketService.disconnect();
    logout();
    navigate('/login');
    toast.success('Signed out');
  };

  const handleSessionCheck = async () => {
    try {
      const response = await authService.testSession();
      toast.success(`Session valid: ${response}`);
    } catch (error) {
      console.error('Session check failed:', error);
      toast.error(error.response?.data || 'Session invalid. Please sign in again.');
    }
  };

  const handleClearToken = () => {
    websocketService.disconnect();
    localStorage.removeItem('token');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    toast.success('Token cleared. Please sign in again.');
    navigate('/login');
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
            {user?.role === 'CUSTOMER' && (
              <>
                <Link to="/" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Home
                </Link>
                <Link to="/restaurants" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Restaurants
                </Link>
                <Link to="/orders" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  My orders
                </Link>
              </>
            )}
            
            {user?.role === 'RESTAURANT_OWNER' && (
              <>
                <Link to="/restaurant-home" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Home
                </Link>
                <Link to="/restaurant-management" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Orders
                </Link>
                <Link to="/menu-management" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Menu
                </Link>
              </>
            )}
            
            {user?.role === 'DRIVER' && (
              <>
                <Link to="/driver-home" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Home
                </Link>
                <Link to="/driver-dashboard" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Driver dashboard
                </Link>
                <Link to="/driver-deliveries" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  My deliveries
                </Link>
              </>
            )}

            {user?.role === 'ADMIN' && (
              <>
                <Link to="/admin/tickets" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Ticket board
                </Link>
                <Link to="/admin/audit" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Audit logs
                </Link>
                <Link to="/admin/users" className="text-gray-700 hover:text-primary-600 font-medium transition-colors">
                  Access management
                </Link>
              </>
            )}
          </div>

          {/* Right Side Icons */}
          <div className="flex items-center space-x-4">
            {/* WebSocket Status */}
            <div className="hidden md:flex items-center">
              <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-gray-300'}`} />
              <span className="ml-2 text-xs text-gray-500">
                {isConnected ? 'Connected' : 'Disconnected'}
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

            {/* Shopping Cart - Only for CUSTOMER */}
            {user?.role === 'CUSTOMER' && (
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
            )}

            {/* User Menu */}
            <div className="hidden md:flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <User className="w-5 h-5 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">
                  {user?.firstName} {user?.lastName}
                </span>
              </div>
              {isAuthenticated && (
                <span className="text-xs text-gray-400">
                  Token: {tokenRemaining || '--'}
                </span>
              )}
              {user?.role === 'ADMIN' && (
                <button
                  onClick={handleSessionCheck}
                  className="p-2 text-gray-600 hover:text-primary-600 transition-colors"
                  title="Check session"
                >
                  <ShieldCheck className="w-5 h-5" />
                </button>
              )}
              <button
                onClick={handleClearToken}
                className="p-2 text-gray-600 hover:text-amber-600 transition-colors"
                title="Clear token"
              >
                <Trash2 className="w-5 h-5" />
              </button>
              <button
                onClick={handleLogout}
                className="p-2 text-gray-600 hover:text-red-600 transition-colors"
                title="Sign out"
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
            {user?.role === 'CUSTOMER' && (
              <>
                <Link
                  to="/"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Home
                </Link>
                <Link
                  to="/restaurants"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Restaurants
                </Link>
                <Link
                  to="/orders"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  My orders
                </Link>
              </>
            )}

            {user?.role === 'RESTAURANT_OWNER' && (
              <>
                <Link
                  to="/restaurant-home"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Home
                </Link>
                <Link
                  to="/restaurant-management"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Orders
                </Link>
                <Link
                  to="/menu-management"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Menu
                </Link>
              </>
            )}

            {user?.role === 'DRIVER' && (
              <>
                <Link
                  to="/driver-home"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Home
                </Link>
                <Link
                  to="/driver-dashboard"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Driver dashboard
                </Link>
                <Link
                  to="/driver-deliveries"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  My deliveries
                </Link>
              </>
            )}

            {user?.role === 'ADMIN' && (
              <>
                <Link
                  to="/admin/tickets"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Ticket board
                </Link>
                <Link
                  to="/admin/audit"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Audit logs
                </Link>
                <Link
                  to="/admin/users"
                  className="block py-2 text-gray-700 hover:text-primary-600"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Access management
                </Link>
              </>
            )}
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
                <span>Sign out</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
