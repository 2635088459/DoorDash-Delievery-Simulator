import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShoppingCart, Plus, Minus, Trash2, ArrowLeft, Store } from 'lucide-react';
import useCartStore from '../store/cartStore';
import toast from 'react-hot-toast';

const Cart = () => {
  const navigate = useNavigate();
  const {
    items,
    restaurantId,
    restaurantName,
    addItem,
    decreaseItem,
    removeItem,
    clearCart,
    getTotalPrice,
    getTotalItems,
  } = useCartStore();

  const handleCheckout = () => {
    if (items.length === 0) {
      toast.error('购物车是空的');
      return;
    }
    // TODO: 跳转到结算页面
    navigate('/checkout');
  };

  const handleClearCart = () => {
    if (window.confirm('确定要清空购物车吗？')) {
      clearCart();
      toast.success('购物车已清空');
    }
  };

  if (items.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center mb-6">
          <button
            onClick={() => navigate('/restaurants')}
            className="flex items-center text-gray-600 hover:text-gray-900"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            返回餐厅列表
          </button>
        </div>

        <div className="text-center py-20">
          <ShoppingCart className="w-24 h-24 text-gray-300 mx-auto mb-6" />
          <h2 className="text-2xl font-bold text-gray-700 mb-2">
            购物车是空的
          </h2>
          <p className="text-gray-500 mb-8">
            去浏览餐厅，添加一些美味的食物吧！
          </p>
          <button
            onClick={() => navigate('/restaurants')}
            className="bg-primary-600 text-white px-8 py-3 rounded-lg font-bold hover:bg-primary-700 transition-colors"
          >
            浏览餐厅
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center">
            <button
              onClick={() => navigate(`/restaurants/${restaurantId}`)}
              className="flex items-center text-gray-600 hover:text-gray-900 mr-6"
            >
              <ArrowLeft className="w-5 h-5 mr-2" />
              继续点餐
            </button>
            <h1 className="text-3xl font-bold text-gray-900">购物车</h1>
          </div>
          <button
            onClick={handleClearCart}
            className="flex items-center text-red-600 hover:text-red-700"
          >
            <Trash2 className="w-5 h-5 mr-2" />
            清空购物车
          </button>
        </div>

        {/* Restaurant Info */}
        <div className="bg-white rounded-xl shadow-sm p-6 mb-6">
          <div className="flex items-center">
            <Store className="w-6 h-6 text-primary-600 mr-3" />
            <div>
              <h3 className="font-bold text-gray-900">{restaurantName}</h3>
              <p className="text-sm text-gray-500">来自这家餐厅</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {items.map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition-shadow"
              >
                <div className="p-6">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h3 className="text-lg font-bold text-gray-900 mb-1">
                        {item.name}
                      </h3>
                      {item.description && (
                        <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                          {item.description}
                        </p>
                      )}
                      <div className="flex items-center space-x-4">
                        <span className="text-xl font-bold text-primary-600">
                          ¥{item.price.toFixed(2)}
                        </span>
                        {item.spicyLevel > 0 && (
                          <span className="text-xs px-2 py-1 bg-red-100 text-red-700 rounded">
                            🌶️ 辣度 {item.spicyLevel}
                          </span>
                        )}
                        {item.isVegetarian && (
                          <span className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded">
                            🥬 素食
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Quantity Controls */}
                    <div className="ml-6 flex flex-col items-end space-y-3">
                      <button
                        onClick={() => removeItem(item.id)}
                        className="text-red-500 hover:text-red-700"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                      <div className="flex items-center bg-gray-100 rounded-lg">
                        <button
                          onClick={() => decreaseItem(item.id)}
                          className="w-10 h-10 flex items-center justify-center hover:bg-gray-200 rounded-l-lg"
                        >
                          <Minus className="w-4 h-4 text-gray-700" />
                        </button>
                        <span className="w-12 text-center font-bold text-gray-900">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => addItem(item, restaurantId, restaurantName)}
                          className="w-10 h-10 flex items-center justify-center hover:bg-gray-200 rounded-r-lg"
                        >
                          <Plus className="w-4 h-4 text-gray-700" />
                        </button>
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-gray-500">小计</p>
                        <p className="text-lg font-bold text-gray-900">
                          ¥{(item.price * item.quantity).toFixed(2)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm p-6 sticky top-6">
              <h3 className="text-xl font-bold text-gray-900 mb-6">订单摘要</h3>

              <div className="space-y-4 mb-6">
                <div className="flex justify-between text-gray-700">
                  <span>商品总计（{getTotalItems()}件）</span>
                  <span>¥{getTotalPrice().toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-gray-700">
                  <span>配送费</span>
                  <span className="text-green-600">免费</span>
                </div>
                <div className="border-t border-gray-200 pt-4">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-bold text-gray-900">总计</span>
                    <span className="text-2xl font-bold text-primary-600">
                      ¥{getTotalPrice().toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={handleCheckout}
                className="w-full bg-primary-600 text-white py-4 rounded-lg font-bold text-lg hover:bg-primary-700 transition-colors"
              >
                去结算
              </button>

              <p className="text-xs text-gray-500 text-center mt-4">
                点击"去结算"即表示您同意我们的服务条款
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Cart;
