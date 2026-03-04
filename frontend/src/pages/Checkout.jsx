import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, CreditCard, Wallet, ShoppingCart, ArrowLeft, Store } from 'lucide-react';
import useCartStore from '../store/cartStore';
import useAuthStore from '../store/authStore';
import { orderService } from '../services/apiService';
import toast from 'react-hot-toast';

const Checkout = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { items, restaurantId, restaurantName, getTotalPrice, clearCart } = useCartStore();
  const [loading, setLoading] = useState(false);
  const [tipAmount, setTipAmount] = useState('0');
  
  const [deliveryAddress, setDeliveryAddress] = useState({
    street: '',
    city: '',
    state: '',
    zipCode: '',
    instructions: '',
  });

  const [paymentMethod, setPaymentMethod] = useState('CREDIT_CARD'); // CREDIT_CARD, CASH

  const subtotal = getTotalPrice();
  const parsedTip = Number.isNaN(Number(tipAmount)) ? 0 : Math.max(0, Number(tipAmount));
  const totalWithTip = subtotal + parsedTip;

  // 如果购物车为空，重定向
  useEffect(() => {
    if (items.length === 0) {
      navigate('/cart');
    }
  }, [items.length, navigate]);

  const handlePlaceOrder = async () => {
    // 验证地址
    if (!deliveryAddress.street || !deliveryAddress.city || !deliveryAddress.state || !deliveryAddress.zipCode) {
  toast.error('Please complete the delivery address');
      return;
    }

    setLoading(true);

    try {
      // 构建订单数据
      const orderData = {
        restaurantId: restaurantId,
        items: items.map(item => ({
          menuItemId: item.id,
          quantity: item.quantity,
        })),
        deliveryAddress: {
          streetAddress: deliveryAddress.street,
          city: deliveryAddress.city,
          state: deliveryAddress.state,
          zipCode: deliveryAddress.zipCode,
          deliveryInstructions: deliveryAddress.instructions || null,
        },
        paymentMethod: paymentMethod,
        tipAmount: parsedTip,
      };

      console.log('Creating order:', orderData);

      // 调用后端 API 创建订单
      const order = await orderService.create(orderData);

      // 清空购物车
      clearCart();

      // 显示成功消息
  toast.success('Order created successfully!');

      // 跳转到订单详情页
      navigate(`/orders/${order.id}`);
    } catch (error) {
      console.error('Failed to create order:', error);
      console.error('Error response:', error.response?.data);
      
      // 显示详细的验证错误
      if (error.response?.data?.errors) {
        const validationErrors = error.response.data.errors;
        const errorMessages = Object.entries(validationErrors)
          .map(([field, message]) => `${field}: ${message}`)
          .join('\n');
        console.error('Validation errors:', validationErrors);
  toast.error(`Validation failed:\n${errorMessages}`);
      } else {
  const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Failed to create order. Please try again.';
        toast.error(errorMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center mb-6">
          <button
            onClick={() => navigate('/cart')}
            className="flex items-center text-gray-600 hover:text-gray-900 mr-6"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Back to cart
          </button>
          <h1 className="text-3xl font-bold text-gray-900">Review order</h1>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Forms */}
          <div className="lg:col-span-2 space-y-6">
            {/* Restaurant Info */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center mb-4">
                <Store className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-xl font-bold text-gray-900">Restaurant</h2>
              </div>
              <p className="text-gray-700">{restaurantName}</p>
            </div>

            {/* Delivery Address */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center mb-4">
                <MapPin className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-xl font-bold text-gray-900">Delivery address</h2>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Street address *
                  </label>
                  <input
                    type="text"
                    value={deliveryAddress.street}
                    onChange={(e) => setDeliveryAddress({ ...deliveryAddress, street: e.target.value })}
                    placeholder="e.g., 123 Main Street, Apt 4B"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      City *
                    </label>
                    <input
                      type="text"
                      value={deliveryAddress.city}
                      onChange={(e) => setDeliveryAddress({ ...deliveryAddress, city: e.target.value })}
                      placeholder="e.g., San Francisco"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      State/Province *
                    </label>
                    <input
                      type="text"
                      value={deliveryAddress.state}
                      onChange={(e) => setDeliveryAddress({ ...deliveryAddress, state: e.target.value })}
                      placeholder="e.g., CA"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    ZIP/Postal code *
                  </label>
                  <input
                    type="text"
                    value={deliveryAddress.zipCode}
                    onChange={(e) => setDeliveryAddress({ ...deliveryAddress, zipCode: e.target.value })}
                    placeholder="e.g., 94103"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Delivery instructions (optional)
                  </label>
                  <textarea
                    value={deliveryAddress.instructions}
                    onChange={(e) => setDeliveryAddress({ ...deliveryAddress, instructions: e.target.value })}
                    placeholder="e.g., Please ring the doorbell"
                    rows="3"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>

            {/* Payment Method */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center mb-4">
                <CreditCard className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-xl font-bold text-gray-900">Payment method</h2>
              </div>

              <div className="space-y-3">
                <div
                  onClick={() => setPaymentMethod('CREDIT_CARD')}
                  className={`flex items-center p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    paymentMethod === 'CREDIT_CARD'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <CreditCard className="w-6 h-6 text-gray-600 mr-3" />
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Credit/Debit card</p>
                    <p className="text-sm text-gray-500">Online payment</p>
                  </div>
                  {paymentMethod === 'CREDIT_CARD' && (
                    <div className="w-5 h-5 bg-primary-500 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-white rounded-full"></div>
                    </div>
                  )}
                </div>

                <div
                  onClick={() => setPaymentMethod('CASH')}
                  className={`flex items-center p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                    paymentMethod === 'CASH'
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <Wallet className="w-6 h-6 text-gray-600 mr-3" />
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">Cash on delivery</p>
                    <p className="text-sm text-gray-500">Cash payment</p>
                  </div>
                  {paymentMethod === 'CASH' && (
                    <div className="w-5 h-5 bg-primary-500 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-white rounded-full"></div>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Tip Amount */}
            <div className="bg-white rounded-xl shadow-sm p-6">
              <div className="flex items-center mb-4">
                <Wallet className="w-6 h-6 text-primary-600 mr-3" />
                <h2 className="text-xl font-bold text-gray-900">Tip your driver</h2>
              </div>
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-700">
                  Tip amount (optional)
                </label>
                <div className="flex items-center space-x-3">
                  <span className="text-gray-600">¥</span>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={tipAmount}
                    onChange={(e) => setTipAmount(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    placeholder="0.00"
                  />
                </div>
                <p className="text-xs text-gray-500">Tips go directly to the driver</p>
              </div>
            </div>
          </div>

          {/* Right Column - Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm p-6 sticky top-6">
              <h2 className="text-xl font-bold text-gray-900 mb-6">Order summary</h2>

              {/* Order Items */}
              <div className="space-y-3 mb-6 max-h-60 overflow-y-auto">
                {items.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <div className="flex-1">
                      <p className="font-medium text-gray-900">{item.name}</p>
                      <p className="text-gray-500">x{item.quantity}</p>
                    </div>
                    <p className="font-medium text-gray-900">
                      ¥{(item.price * item.quantity).toFixed(2)}
                    </p>
                  </div>
                ))}
              </div>

              <div className="border-t border-gray-200 pt-4 space-y-3">
                <div className="flex justify-between text-gray-700">
                  <span>Subtotal</span>
                  <span>¥{subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-gray-700">
                  <span>Delivery fee</span>
                  <span className="text-green-600">Free</span>
                </div>
                <div className="flex justify-between text-gray-700">
                  <span>Service fee</span>
                  <span>¥0.00</span>
                </div>
                <div className="flex justify-between text-gray-700">
                  <span>Tip</span>
                  <span>¥{parsedTip.toFixed(2)}</span>
                </div>
                <div className="border-t border-gray-200 pt-3">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-bold text-gray-900">Total</span>
                    <span className="text-2xl font-bold text-primary-600">
                      ¥{totalWithTip.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={handlePlaceOrder}
                disabled={loading}
                className="w-full bg-primary-600 text-white py-4 rounded-lg font-bold text-lg hover:bg-primary-700 transition-colors mt-6 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-2"></div>
                    Processing...
                  </div>
                ) : (
                  'Place order'
                )}
              </button>

              <p className="text-xs text-gray-500 text-center mt-4">
                By clicking "Place order" you agree to our terms and privacy policy.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;
