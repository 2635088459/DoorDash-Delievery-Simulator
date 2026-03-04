import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingBag, Package, Loader } from 'lucide-react';
import { orderService } from '../services/apiService';
import toast from 'react-hot-toast';

const Orders = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = async () => {
    try {
      setLoading(true);
      const data = await orderService.getMyOrders();
      setOrders(data);
    } catch (error) {
      console.error('Failed to load orders:', error);
      toast.error('Failed to load orders');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      CONFIRMED: 'bg-blue-100 text-blue-800',
      PREPARING: 'bg-purple-100 text-purple-800',
      READY: 'bg-green-100 text-green-800',
      DELIVERING: 'bg-indigo-100 text-indigo-800',
      DELIVERED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
    };
    return badges[status] || badges.PENDING;
  };

  const getStatusText = (status) => {
    const texts = {
      PENDING: 'Pending confirmation',
      CONFIRMED: 'Confirmed',
      PREPARING: 'Preparing',
      READY: 'Ready for pickup',
      DELIVERING: 'Out for delivery',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return texts[status] || status;
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex items-center space-x-3 mb-8">
        <ShoppingBag className="w-8 h-8 text-primary-600" />
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My orders</h1>
          <p className="text-sm text-gray-600 mt-1">
            View your order history and status
          </p>
        </div>
      </div>

      {/* Loading State */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader className="w-8 h-8 text-primary-600 animate-spin" />
        </div>
      ) : (
        <>
          {/* Orders List */}
          {orders.length === 0 ? (
            <div className="text-center py-12">
              <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500 text-lg">No orders yet</p>
              <p className="text-gray-400 text-sm mt-2">
                Start ordering and your orders will show up here
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {orders.map((order) => (
            <div
              key={order.id}
              onClick={() => navigate(`/orders/${order.id}`)}
              className="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-shadow cursor-pointer"
            >
              <div className="flex items-start justify-between mb-4">
                <div>
                  <h3 className="text-lg font-bold text-gray-900 mb-1">
                    {order.restaurantName}
                  </h3>
                  <p className="text-sm text-gray-500">
                    Order #{order.orderNumber}
                  </p>
                </div>
                <span className={`badge ${getStatusBadge(order.status)}`}>
                  {getStatusText(order.status)}
                </span>
              </div>

              <div className="flex items-center justify-between text-sm">
                <div className="text-gray-600">
                  {order.items?.length || 0} items
                </div>
                <div className="flex items-center space-x-4">
                  <span className="text-gray-600">
                    {new Date(order.createdAt).toLocaleString('en-US')}
                  </span>
                  <span className="text-lg font-bold text-primary-600">
                    ¥{order.totalAmount?.toFixed(2) || '0.00'}
                  </span>
                </div>
              </div>

              <div className="mt-4 pt-4 border-t border-gray-200 flex items-center justify-end space-x-2">
                <button 
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/orders/${order.id}`);
                  }}
                  className="btn btn-outline text-sm"
                >
                  View details
                </button>
                {order.status === 'PENDING' && (
                  <button 
                    onClick={(e) => {
                      e.stopPropagation();
                      // TODO: 实现取消订单功能
                      toast.error('Cancel order is under development');
                    }}
                    className="btn btn-secondary text-sm"
                  >
                    Cancel order
                  </button>
                )}
              </div>
            </div>
            ))}
          </div>
        )}
      </>
      )}

      {/* Info */}
      <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="text-sm text-blue-800">
          <p className="font-medium mb-1">💡 Tip</p>
          <p className="text-blue-700">
            Order status updates in real time, and you'll receive notifications. Click an order to view details and delivery progress.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Orders;