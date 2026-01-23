import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Store, MapPin, Star, Clock, ArrowLeft, ShoppingCart, Plus, Minus } from 'lucide-react';
import { restaurantService, menuItemService } from '../services/apiService';
import useCartStore from '../store/cartStore';
import toast from 'react-hot-toast';

const RestaurantDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState(null);
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // 使用全局购物车状态
  const { 
    items: cartItems, 
    addItem, 
    decreaseItem, 
    getItemQuantity, 
    getTotalPrice, 
    getTotalItems,
    restaurantId: cartRestaurantId 
  } = useCartStore();

  useEffect(() => {
    loadRestaurantDetails();
  }, [id]);

  const loadRestaurantDetails = async () => {
    try {
      setLoading(true);
      const [restaurantData, menuData] = await Promise.all([
        restaurantService.getById(id),
        menuItemService.getByRestaurant(id)
      ]);
      setRestaurant(restaurantData);
      setMenuItems(menuData);
    } catch (error) {
      console.error('Failed to load restaurant details:', error);
      toast.error('加载餐厅信息失败');
      navigate('/restaurants');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = (item) => {
    // 检查是否来自不同餐厅
    if (cartRestaurantId && cartRestaurantId !== parseInt(id)) {
      if (window.confirm(`购物车中有来自其他餐厅的商品，是否清空购物车并添加新商品？`)) {
        addItem(item, parseInt(id), restaurant.name);
        toast.success(`已添加 ${item.name} 到购物车`);
      }
    } else {
      addItem(item, parseInt(id), restaurant.name);
      toast.success(`已添加 ${item.name} 到购物车`);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!restaurant) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="text-center">
          <Store className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">餐厅不存在</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <button
            onClick={() => navigate('/restaurants')}
            className="flex items-center text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            返回餐厅列表
          </button>
        </div>
      </div>

      {/* Restaurant Info */}
      <div className="bg-gradient-to-br from-primary-100 to-primary-200 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-start space-x-6">
            <div className="flex-shrink-0">
              <div className="w-32 h-32 bg-white rounded-xl shadow-lg flex items-center justify-center">
                <Store className="w-16 h-16 text-primary-600" />
              </div>
            </div>
            <div className="flex-1">
              <h1 className="text-3xl font-bold text-gray-900 mb-2">
                {restaurant.name}
              </h1>
              <div className="flex items-center space-x-4 text-sm text-gray-700 mb-3">
                <div className="flex items-center">
                  <Star className="w-4 h-4 text-yellow-400 fill-current mr-1" />
                  <span className="font-medium">{restaurant.rating?.toFixed(1) || '暂无评分'}</span>
                </div>
                <span>•</span>
                <span>{restaurant.cuisineType || '其他'}</span>
                <span>•</span>
                <div className="flex items-center">
                  <Clock className="w-4 h-4 mr-1" />
                  <span>{restaurant.estimatedDeliveryTime || '30-40'} 分钟</span>
                </div>
              </div>
              {restaurant.address && (
                <div className="flex items-center text-sm text-gray-600 mb-2">
                  <MapPin className="w-4 h-4 mr-2" />
                  <span>{restaurant.address}</span>
                </div>
              )}
              <div className="text-sm text-gray-600">
                起送金额：¥{restaurant.minimumOrderAmount?.toFixed(2) || '0.00'}
              </div>
              {!restaurant.isActive && (
                <div className="mt-3 inline-block px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">
                  已打烊
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Menu Items */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">菜单</h2>

        {menuItems.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-xl">
            <Store className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600">该餐厅暂无菜品</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {menuItems.map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow"
              >
                <div className="p-6">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <h3 className="text-lg font-bold text-gray-900 mb-2">
                        {item.name}
                      </h3>
                      {item.description && (
                        <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                          {item.description}
                        </p>
                      )}
                      <div className="flex items-center justify-between">
                        <span className="text-xl font-bold text-primary-600">
                          ¥{item.price.toFixed(2)}
                        </span>
                        {!item.isAvailable && (
                          <span className="text-sm text-red-500">暂时售罄</span>
                        )}
                      </div>
                    </div>
                    <div className="ml-4">
                      {item.imageUrl ? (
                        <img
                          src={item.imageUrl}
                          alt={item.name}
                          className="w-24 h-24 object-cover rounded-lg"
                        />
                      ) : (
                        <div className="w-24 h-24 bg-gray-200 rounded-lg flex items-center justify-center">
                          <Store className="w-8 h-8 text-gray-400" />
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Add to Cart Button */}
                  <div className="mt-4">
                    {getItemQuantity(item.id) === 0 ? (
                      <button
                        onClick={() => handleAddToCart(item)}
                        disabled={!item.isAvailable || !restaurant.isActive}
                        className="w-full bg-primary-600 text-white py-2 rounded-lg font-medium hover:bg-primary-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors flex items-center justify-center"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        添加到购物车
                      </button>
                    ) : (
                      <div className="flex items-center justify-between bg-primary-50 rounded-lg p-2">
                        <button
                          onClick={() => decreaseItem(item.id)}
                          className="w-8 h-8 bg-white rounded-lg flex items-center justify-center hover:bg-gray-100"
                        >
                          <Minus className="w-4 h-4 text-primary-600" />
                        </button>
                        <span className="font-bold text-primary-600">
                          {getItemQuantity(item.id)}
                        </span>
                        <button
                          onClick={() => handleAddToCart(item)}
                          className="w-8 h-8 bg-white rounded-lg flex items-center justify-center hover:bg-gray-100"
                        >
                          <Plus className="w-4 h-4 text-primary-600" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Floating Cart Summary */}
      {getTotalItems() > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg z-20">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="bg-primary-600 text-white rounded-full w-10 h-10 flex items-center justify-center">
                  <ShoppingCart className="w-5 h-5" />
                </div>
                <div>
                  <div className="font-bold text-gray-900">
                    {getTotalItems()} 件商品
                  </div>
                  <div className="text-sm text-gray-600">
                    总计：¥{getTotalPrice().toFixed(2)}
                  </div>
                </div>
              </div>
              <button
                onClick={() => navigate('/cart')}
                className="bg-primary-600 text-white px-8 py-3 rounded-lg font-bold hover:bg-primary-700 transition-colors"
              >
                查看购物车
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RestaurantDetail;
