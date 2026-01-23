import React, { useEffect, useState } from 'react';
import { Bell, Check, CheckCheck, Trash2, AlertCircle } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import useNotificationStore from '../store/notificationStore';
import { notificationService } from '../services/apiService';
import toast from 'react-hot-toast';

const Notifications = () => {
  const { notifications, unreadCount, markAsRead, markAllAsRead, clearNotifications } = useNotificationStore();
  const [loading, setLoading] = useState(false);

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'URGENT':
        return 'border-red-500 bg-red-50';
      case 'HIGH':
        return 'border-orange-500 bg-orange-50';
      case 'NORMAL':
        return 'border-blue-500 bg-blue-50';
      case 'LOW':
        return 'border-gray-500 bg-gray-50';
      default:
        return 'border-gray-300 bg-white';
    }
  };

  const getPriorityBadge = (priority) => {
    const colors = {
      URGENT: 'bg-red-100 text-red-800',
      HIGH: 'bg-orange-100 text-orange-800',
      NORMAL: 'bg-blue-100 text-blue-800',
      LOW: 'bg-gray-100 text-gray-800',
    };
    return colors[priority] || colors.NORMAL;
  };

  const getNotificationIcon = (type) => {
    const icons = {
      ORDER_CREATED: '📝',
      ORDER_CONFIRMED: '✅',
      ORDER_PREPARING: '👨‍🍳',
      ORDER_READY: '🎉',
      DELIVERY_ASSIGNED: '🚴',
      DELIVERY_PICKED_UP: '📦',
      DELIVERY_IN_PROGRESS: '🚚',
      DELIVERY_NEAR: '⚠️',
      ORDER_DELIVERED: '✅',
      ORDER_CANCELLED: '❌',
      PAYMENT_SUCCESS: '💳',
      PAYMENT_FAILED: '⚠️',
      REFUND_PROCESSED: '💰',
      PROMOTION_AVAILABLE: '🎁',
      DRIVER_RATING_REQUEST: '⭐',
    };
    return icons[type] || '🔔';
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationService.markAsRead(notificationId);
      markAsRead(notificationId);
      toast.success('已标记为已读');
    } catch (error) {
      console.error('Error marking notification as read:', error);
      toast.error('操作失败');
    }
  };

  const handleMarkAllAsRead = async () => {
    setLoading(true);
    try {
      await notificationService.markAllAsRead();
      markAllAsRead();
      toast.success('已全部标记为已读');
    } catch (error) {
      console.error('Error marking all as read:', error);
      toast.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  const handleClearAll = () => {
    if (window.confirm('确定要清空所有通知吗？')) {
      clearNotifications();
      toast.success('通知已清空');
    }
  };

  const formatTime = (dateString) => {
    try {
      const date = new Date(dateString);
      return formatDistanceToNow(date, { addSuffix: true, locale: zhCN });
    } catch {
      return '刚刚';
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center space-x-3">
          <Bell className="w-8 h-8 text-primary-600" />
          <div>
            <h1 className="text-3xl font-bold text-gray-900">通知中心</h1>
            <p className="text-sm text-gray-600 mt-1">
              {unreadCount} 条未读通知
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2">
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllAsRead}
              disabled={loading}
              className="btn btn-outline flex items-center space-x-2"
            >
              <CheckCheck className="w-4 h-4" />
              <span className="hidden sm:inline">全部已读</span>
            </button>
          )}
          {notifications.length > 0 && (
            <button
              onClick={handleClearAll}
              className="btn btn-secondary flex items-center space-x-2"
            >
              <Trash2 className="w-4 h-4" />
              <span className="hidden sm:inline">清空</span>
            </button>
          )}
        </div>
      </div>

      {/* Notifications List */}
      {notifications.length === 0 ? (
        <div className="text-center py-12">
          <Bell className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500 text-lg">暂无通知</p>
          <p className="text-gray-400 text-sm mt-2">
            您的通知将在这里显示
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {notifications.map((notification) => (
            <div
              key={notification.id}
              className={`border-l-4 rounded-lg shadow-md p-4 transition-all duration-300 hover:shadow-lg ${
                getPriorityColor(notification.priority)
              } ${!notification.isRead ? 'bg-white' : 'opacity-75'}`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  {/* Header */}
                  <div className="flex items-start space-x-3 mb-2">
                    <span className="text-2xl" role="img" aria-label="notification">
                      {getNotificationIcon(notification.notificationType)}
                    </span>
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="text-lg font-bold text-gray-900">
                          {notification.title}
                        </h3>
                        <span className={`badge ${getPriorityBadge(notification.priority)}`}>
                          {notification.priority}
                        </span>
                      </div>
                      <p className="text-gray-700">{notification.message}</p>
                    </div>
                  </div>

                  {/* Footer */}
                  <div className="flex items-center justify-between mt-3 ml-11">
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <span>{formatTime(notification.createdAt)}</span>
                      {notification.notificationType && (
                        <span className="text-xs bg-gray-200 text-gray-700 px-2 py-1 rounded">
                          {notification.notificationType}
                        </span>
                      )}
                    </div>

                    {!notification.isRead && (
                      <button
                        onClick={() => handleMarkAsRead(notification.id)}
                        className="flex items-center space-x-1 text-primary-600 hover:text-primary-700 text-sm font-medium"
                      >
                        <Check className="w-4 h-4" />
                        <span>标记已读</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Info */}
      <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start">
          <AlertCircle className="w-5 h-5 text-blue-600 mt-0.5 mr-3" />
          <div className="text-sm text-blue-800">
            <p className="font-medium mb-1">关于通知</p>
            <p className="text-blue-700">
              您将实时收到订单状态更新、配送进度、支付确认等重要通知。
              未读通知会在导航栏显示数量提醒。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Notifications;
