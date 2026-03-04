import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, MapPin, Star, Clock, Filter } from 'lucide-react';
import { restaurantService } from '../services/apiService';
import toast from 'react-hot-toast';

const Restaurants = () => {
  const navigate = useNavigate();
  const [restaurants, setRestaurants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCuisine, setSelectedCuisine] = useState('All');

  // 获取餐厅列表
  useEffect(() => {
    loadRestaurants();
  }, []);

  const loadRestaurants = async () => {
    try {
      setLoading(true);
      const data = await restaurantService.getAll();
      setRestaurants(data);
    } catch (error) {
      console.error('Failed to load restaurants:', error);
      toast.error('Failed to load restaurants');
    } finally {
      setLoading(false);
    }
  };

  // 获取所有菜系类型
  const cuisineTypes = ['All', ...new Set(restaurants.map(r => r.cuisineType).filter(Boolean))];

  // 筛选餐厅
  const filteredRestaurants = selectedCuisine === 'All'
    ? restaurants
    : restaurants.filter(r => r.cuisineType === selectedCuisine);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Nearby restaurants
        </h1>
        <p className="text-gray-600">
          Discover tasty restaurants near you
        </p>
      </div>

      {/* Filters */}
      <div className="mb-6 flex items-center space-x-4 overflow-x-auto pb-2">
        {cuisineTypes.map((cuisine) => (
          <button
            key={cuisine}
            onClick={() => setSelectedCuisine(cuisine)}
            className={`px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-colors ${
              selectedCuisine === cuisine
                ? 'bg-primary-600 text-white'
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            {cuisine}
          </button>
        ))}
      </div>

      {/* Loading State */}
      {loading && (
        <div className="flex justify-center items-center py-20">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      )}

      {/* Empty State */}
      {!loading && filteredRestaurants.length === 0 && (
        <div className="text-center py-20">
          <Store className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 className="text-xl font-bold text-gray-700 mb-2">
            No restaurants yet
          </h3>
          <p className="text-gray-500">
            {selectedCuisine === 'All'
              ? 'No restaurants available right now. Please try again later.'
              : `No ${selectedCuisine} restaurants`}
          </p>
        </div>
      )}

      {/* Restaurants Grid */}
      {!loading && filteredRestaurants.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredRestaurants.map((restaurant) => (
            <div
              key={restaurant.id}
              onClick={() => navigate(`/restaurants/${restaurant.id}`)}
              className="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-xl transition-shadow duration-300 cursor-pointer"
            >
              {/* Image */}
              <div className="h-48 bg-gradient-to-br from-primary-100 to-primary-200 flex items-center justify-center relative">
                <Store className="w-16 h-16 text-primary-600" />
                {!restaurant.isActive && (
                  <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                    <span className="text-white font-bold text-lg">Closed</span>
                  </div>
                )}
              </div>

              {/* Content */}
              <div className="p-5">
                <h3 className="text-xl font-bold text-gray-900 mb-2">
                  {restaurant.name}
                </h3>
                
                <div className="flex items-center space-x-2 mb-3">
                  <div className="flex items-center">
                    <Star className="w-4 h-4 text-yellow-400 fill-current" />
                    <span className="ml-1 text-sm font-medium text-gray-700">
                      {restaurant.rating?.toFixed(1) || 'No rating yet'}
                    </span>
                  </div>
                  <span className="text-gray-400">•</span>
                  <span className="text-sm text-gray-600">{restaurant.cuisineType || 'Other'}</span>
                </div>

                <div className="flex items-center justify-between text-sm text-gray-600">
                  <div className="flex items-center">
                    <Clock className="w-4 h-4 mr-1" />
                    <span>{restaurant.estimatedDeliveryTime || '30-40'} min</span>
                  </div>
                  <span className="font-medium text-primary-600">
                    Minimum ¥{restaurant.minimumOrderAmount?.toFixed(2) || '0.00'}
                  </span>
                </div>

                {restaurant.address && (
                  <div className="flex items-center mt-2 text-xs text-gray-500">
                    <MapPin className="w-3 h-3 mr-1" />
                    <span className="truncate">{restaurant.address}</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Coming Soon */}
      <div className="mt-12 text-center py-12 bg-gradient-to-br from-gray-50 to-gray-100 rounded-xl">
        <Store className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h3 className="text-xl font-bold text-gray-700 mb-2">
          More restaurants coming soon
        </h3>
        <p className="text-gray-500">
          We are adding more great restaurants. Stay tuned!
        </p>
      </div>
    </div>
  );
};

export default Restaurants;
