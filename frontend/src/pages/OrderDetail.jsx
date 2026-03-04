import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Package, 
  ArrowLeft, 
  Store, 
  MapPin, 
  CreditCard, 
  Wallet,
  Clock,
  CheckCircle,
  Truck,
  User,
  Phone,
  Calendar
} from 'lucide-react';
import { orderService } from '../services/apiService';
import toast from 'react-hot-toast';

const OrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadOrderDetail();
  }, [id]);

  const loadOrderDetail = async () => {
    try {
      setLoading(true);
      const data = await orderService.getById(id);
      setOrder(data);
    } catch (error) {
      console.error('Failed to load order:', error);
      toast.error('Failed to load order details');
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Pending confirmation' },
      CONFIRMED: { bg: 'bg-blue-100', text: 'text-blue-800', label: 'Confirmed' },
      PREPARING: { bg: 'bg-purple-100', text: 'text-purple-800', label: 'Preparing' },
      READY: { bg: 'bg-green-100', text: 'text-green-800', label: 'Ready for pickup' },
      PICKED_UP: { bg: 'bg-indigo-100', text: 'text-indigo-800', label: 'Out for delivery' },
      DELIVERED: { bg: 'bg-green-100', text: 'text-green-800', label: 'Delivered' },
      CANCELLED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Cancelled' },
    };
    const badge = badges[status] || { bg: 'bg-gray-100', text: 'text-gray-800', label: status };
    return (
      <span className={`px-3 py-1 rounded-full text-sm font-medium ${badge.bg} ${badge.text}`}>
        {badge.label}
      </span>
    );
  };

  const getPaymentStatusBadge = (status) => {
    const badges = {
      PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Payment pending' },
      PAID: { bg: 'bg-green-100', text: 'text-green-800', label: 'Paid' },
      FAILED: { bg: 'bg-red-100', text: 'text-red-800', label: 'Payment failed' },
      REFUNDED: { bg: 'bg-gray-100', text: 'text-gray-800', label: 'Refunded' },
    };
    const badge = badges[status] || { bg: 'bg-gray-100', text: 'text-gray-800', label: status };
    return (
      <span className={`px-3 py-1 rounded-full text-sm font-medium ${badge.bg} ${badge.text}`}>
        {badge.label}
      </span>
    );
  };

  const getPaymentMethodLabel = (method) => {
    const labels = {
      CREDIT_CARD: 'Credit card',
      DEBIT_CARD: 'Debit card',
      CASH: 'Cash',
      DIGITAL_WALLET: 'Digital wallet',
    };
    return labels[method] || method;
  };

  const getPaymentMethodIcon = (method) => {
    return method === 'CASH' ? Wallet : CreditCard;
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getOrderTimeline = () => {
    if (!order) return [];
    
    const timeline = [
      { status: 'PENDING', label: 'Order created', time: order.createdAt, completed: true },
    ];

    const statusOrder = ['CONFIRMED', 'PREPARING', 'READY', 'PICKED_UP', 'DELIVERED'];
    const currentIndex = statusOrder.indexOf(order.status);

    statusOrder.forEach((status, index) => {
      const labels = {
        CONFIRMED: 'Restaurant confirmed',
        PREPARING: 'Preparing',
        READY: 'Ready for pickup',
        PICKED_UP: 'Driver picked up',
        DELIVERED: 'Delivered',
      };

      timeline.push({
        status,
        label: labels[status],
        time: null, // TODO: 从后端获取每个状态的时间戳
        completed: index <= currentIndex,
      });
    });

    if (order.status === 'CANCELLED') {
      timeline.push({
        status: 'CANCELLED',
        label: 'Order cancelled',
        time: null,
        completed: true,
      });
    }

    return timeline;
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading order details...</p>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Package className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-700 mb-2">Order not found</h2>
          <p className="text-gray-500 mb-4">We couldn't find that order.</p>
          <button
            onClick={() => navigate('/orders')}
            className="text-primary-600 hover:text-primary-700 font-medium"
          >
            Back to orders
          </button>
        </div>
      </div>
    );
  }

  const PaymentIcon = getPaymentMethodIcon(order.paymentMethod);
  const timeline = getOrderTimeline();

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => navigate('/orders')}
            className="flex items-center text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Back to orders
          </button>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Order details</h1>
              <p className="text-gray-500 mt-1">Order #{order.orderNumber}</p>
            </div>
            {getStatusBadge(order.status)}
          </div>
        </div>

        {/* Order Timeline */}
        <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
          <h2 className="text-lg font-bold text-gray-900 mb-6 flex items-center">
            <Clock className="w-5 h-5 mr-2 text-primary-600" />
            Order timeline
          </h2>
          <div className="space-y-4">
            {timeline.map((step, index) => (
              <div key={step.status} className="flex items-start">
                <div className="flex flex-col items-center mr-4">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center ${
                      step.completed
                        ? 'bg-primary-600 text-white'
                        : 'bg-gray-200 text-gray-400'
                    }`}
                  >
                    {step.completed ? (
                      <CheckCircle className="w-5 h-5" />
                    ) : (
                      <div className="w-2 h-2 bg-gray-400 rounded-full"></div>
                    )}
                  </div>
                  {index < timeline.length - 1 && (
                    <div
                      className={`w-0.5 h-12 ${
                        step.completed ? 'bg-primary-600' : 'bg-gray-200'
                      }`}
                    ></div>
                  )}
                </div>
                <div className="flex-1 pb-8">
                  <p
                    className={`font-medium ${
                      step.completed ? 'text-gray-900' : 'text-gray-400'
                    }`}
                  >
                    {step.label}
                  </p>
                  {step.time && (
                    <p className="text-sm text-gray-500 mt-1">
                      {formatDateTime(step.time)}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Order Items */}
          <div className="lg:col-span-2 space-y-6">
            {/* Restaurant Info */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                <Store className="w-5 h-5 mr-2 text-primary-600" />
                Restaurant
              </h2>
              <div>
                <p className="font-medium text-gray-900">{order.restaurantName}</p>
                {order.restaurantAddress && (
                  <p className="text-sm text-gray-500 mt-1">{order.restaurantAddress}</p>
                )}
              </div>
            </div>

            {/* Order Items */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                <Package className="w-5 h-5 mr-2 text-primary-600" />
                Items
              </h2>
              <div className="space-y-4">
                {order.items?.map((item, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between py-3 border-b border-gray-100 last:border-0"
                  >
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">{item.menuItemName}</p>
                      <p className="text-sm text-gray-500">
                        ¥{item.unitPrice} × {item.quantity}
                      </p>
                    </div>
                    <p className="font-medium text-gray-900">
                      ¥{item.subtotal}
                    </p>
                  </div>
                ))}
              </div>

              {order.specialInstructions && (
                <div className="mt-4 pt-4 border-t border-gray-200">
                  <p className="text-sm text-gray-500">Notes</p>
                  <p className="text-gray-900 mt-1">{order.specialInstructions}</p>
                </div>
              )}
            </div>

            {/* Delivery Address */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                <MapPin className="w-5 h-5 mr-2 text-primary-600" />
                Delivery address
              </h2>
              <div>
                <p className="text-gray-900">{order.deliveryAddress}</p>
              </div>
            </div>

            {/* Delivery Info */}
            {order.delivery && (
              <div className="bg-white rounded-xl shadow-sm p-6">
                <h2 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <Truck className="w-5 h-5 mr-2 text-primary-600" />
                  Delivery
                </h2>
                <div className="space-y-3">
                  <div className="flex items-center">
                    <User className="w-4 h-4 text-gray-400 mr-2" />
                    <span className="text-gray-600">Driver:</span>
                    <span className="ml-2 text-gray-900">{order.delivery.driverName || 'Unassigned'}</span>
                  </div>
                  {order.delivery.driverPhone && (
                    <div className="flex items-center">
                      <Phone className="w-4 h-4 text-gray-400 mr-2" />
                      <span className="text-gray-600">Phone:</span>
                      <a
                        href={`tel:${order.delivery.driverPhone}`}
                        className="ml-2 text-primary-600 hover:text-primary-700"
                      >
                        {order.delivery.driverPhone}
                      </a>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Right Column - Summary */}
          <div className="space-y-6">
            {/* Order Summary */}
            <div className="bg-white rounded-xl shadow-sm p-6 sticky top-6">
              <h2 className="text-lg font-bold text-gray-900 mb-4">Order summary</h2>
              
              <div className="space-y-3 mb-4">
                <div className="flex justify-between text-gray-600">
                  <span>Subtotal</span>
                  <span>¥{order.subtotal}</span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Delivery fee</span>
                  <span className={order.deliveryFee === 0 ? 'text-green-600' : ''}>
                    {order.deliveryFee === 0 ? 'Free' : `¥${order.deliveryFee}`}
                  </span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Service fee</span>
                  <span>¥{order.serviceFee || 0}</span>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <div className="flex justify-between items-center mb-4">
                  <span className="text-lg font-bold text-gray-900">Total</span>
                  <span className="text-2xl font-bold text-primary-600">
                    ¥{order.totalAmount}
                  </span>
                </div>

                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center text-gray-600">
                    <PaymentIcon className="w-4 h-4 mr-2" />
                    {getPaymentMethodLabel(order.paymentMethod)}
                  </div>
                  {getPaymentStatusBadge(order.paymentStatus)}
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-gray-200 space-y-2 text-sm text-gray-500">
                <div className="flex items-center">
                  <Calendar className="w-4 h-4 mr-2" />
                  <span>Placed at: {formatDateTime(order.createdAt)}</span>
                </div>
                {order.estimatedDeliveryTime && (
                  <div className="flex items-center">
                    <Clock className="w-4 h-4 mr-2" />
                    <span>Estimated delivery: {formatDateTime(order.estimatedDeliveryTime)}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderDetail;
