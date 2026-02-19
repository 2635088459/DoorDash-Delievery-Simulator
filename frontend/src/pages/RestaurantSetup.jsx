import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { restaurantService } from '../services/apiService';

const RestaurantSetup = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formState, setFormState] = useState({
    name: '',
    description: '',
    cuisineType: '',
    streetAddress: '',
    city: '',
    state: '',
    zipCode: '',
    phoneNumber: '',
  });

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormState((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      setLoading(true);
      const created = await restaurantService.create(formState);
      if (created?.id) {
        restaurantService.setActiveRestaurantId(created.id);
      }
      toast.success('餐厅创建成功');
      navigate('/restaurant-home');
    } catch (err) {
      console.error('Failed to create restaurant:', err);
      toast.error(err.response?.data?.message || '创建餐厅失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">创建餐厅</h1>
        <p className="text-gray-600 mb-6">请先创建餐厅信息，才能进入管理后台。</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">餐厅名称</label>
            <input
              type="text"
              name="name"
              value={formState.name}
              onChange={handleChange}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">菜系类型</label>
            <input
              type="text"
              name="cuisineType"
              value={formState.cuisineType}
              onChange={handleChange}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">联系电话</label>
            <input
              type="text"
              name="phoneNumber"
              value={formState.phoneNumber}
              onChange={handleChange}
              placeholder="+14155555678"
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">详细地址</label>
            <input
              type="text"
              name="streetAddress"
              value={formState.streetAddress}
              onChange={handleChange}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">城市</label>
              <input
                type="text"
                name="city"
                value={formState.city}
                onChange={handleChange}
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">州/省</label>
              <input
                type="text"
                name="state"
                value={formState.state}
                onChange={handleChange}
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">邮编</label>
              <input
                type="text"
                name="zipCode"
                value={formState.zipCode}
                onChange={handleChange}
                required
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">餐厅描述</label>
            <textarea
              name="description"
              value={formState.description}
              onChange={handleChange}
              rows="3"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
            />
          </div>

          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={loading}
              className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-60"
            >
              {loading ? '创建中...' : '创建餐厅'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/restaurant-home')}
              className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
            >
              返回首页
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RestaurantSetup;
