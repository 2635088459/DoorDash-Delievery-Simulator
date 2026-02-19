import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { menuItemService, restaurantService, uploadService } from '../services/apiService';

const defaultFormState = {
  name: '',
  description: '',
  category: '',
  price: '',
  imageUrl: '',
  isAvailable: true,
  isVegetarian: false,
  isVegan: false,
  spicyLevel: 0,
};

const MenuManagement = () => {
  const navigate = useNavigate();
  const [restaurant, setRestaurant] = useState(null);
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [editingItem, setEditingItem] = useState(null);
  const [formState, setFormState] = useState(defaultFormState);
  const [imageUploading, setImageUploading] = useState(false);

  const isEditing = Boolean(editingItem);

  const formTitle = useMemo(() => {
    if (isEditing) {
      return `编辑菜品 #${editingItem?.id}`;
    }
    return '新增菜品';
  }, [isEditing, editingItem]);

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.role !== 'RESTAURANT_OWNER') {
      setError('您没有权限访问此页面。此页面仅供餐厅老板使用。');
      setLoading(false);
      return;
    }

    const loadMenu = async () => {
      try {
        setLoading(true);
        const ownerRestaurant = await restaurantService.getOwnerRestaurant();
        setRestaurant(ownerRestaurant);
        const items = await menuItemService.getAllByRestaurant(ownerRestaurant.id);
        setMenuItems(items);
        setError('');
      } catch (err) {
        console.error('Failed to load menu items:', err);
        setError(err.response?.data?.message || '加载菜品失败');
      } finally {
        setLoading(false);
      }
    };

    loadMenu();
  }, []);

  const resetForm = () => {
    setEditingItem(null);
    setFormState(defaultFormState);
  };

  const refreshMenu = async () => {
    if (!restaurant?.id) return;
    const items = await menuItemService.getAllByRestaurant(restaurant.id);
    setMenuItems(items);
  };

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFormState((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleImageUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      toast.error('请上传图片文件');
      return;
    }
    const maxSizeMb = 2;
    if (file.size > maxSizeMb * 1024 * 1024) {
      toast.error(`图片大小不能超过 ${maxSizeMb}MB`);
      return;
    }

    try {
      setImageUploading(true);
      const uploadResult = await uploadService.uploadMenuItemImage(file);
      if (uploadResult?.url) {
        setFormState((prev) => ({
          ...prev,
          imageUrl: uploadResult.url,
        }));
        toast.success('图片上传成功');
      } else {
        toast.error('图片上传失败，请重试');
      }
    } catch (err) {
      console.error('Failed to upload image:', err);
      toast.error(err.response?.data?.message || '图片上传失败');
    } finally {
      setImageUploading(false);
      event.target.value = '';
    }
  };

  const handleClearImage = () => {
    setFormState((prev) => ({
      ...prev,
      imageUrl: '',
    }));
  };

  const handleEdit = (item) => {
    setEditingItem(item);
    setFormState({
      name: item.name || '',
      description: item.description || '',
      category: item.category || '',
      price: item.price ?? '',
      imageUrl: item.imageUrl || '',
      isAvailable: item.isAvailable ?? true,
      isVegetarian: item.isVegetarian ?? false,
      isVegan: item.isVegan ?? false,
      spicyLevel: item.spicyLevel ?? 0,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!restaurant?.id) return;

    const payload = {
      name: formState.name.trim(),
      description: formState.description.trim(),
      category: formState.category.trim(),
      price: formState.price === '' ? undefined : Number(formState.price),
      imageUrl: formState.imageUrl.trim() || null,
      isAvailable: formState.isAvailable,
      isVegetarian: formState.isVegetarian,
      isVegan: formState.isVegan,
      spicyLevel: formState.spicyLevel === '' ? 0 : Number(formState.spicyLevel),
    };

    try {
      setSaving(true);
      if (isEditing) {
        await menuItemService.update(editingItem.id, payload);
        toast.success('菜品已更新');
      } else {
        await menuItemService.create({ ...payload, restaurantId: restaurant.id });
        toast.success('菜品已创建');
      }
      await refreshMenu();
      resetForm();
    } catch (err) {
      console.error('Failed to save menu item:', err);
      toast.error(err.response?.data?.message || '保存菜品失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (itemId) => {
    if (!confirm('确定要删除这个菜品吗？')) return;
    try {
      await menuItemService.remove(itemId);
      toast.success('菜品已删除');
      await refreshMenu();
    } catch (err) {
      console.error('Failed to delete menu item:', err);
      toast.error(err.response?.data?.message || '删除菜品失败');
    }
  };

  const toggleAvailability = async (item) => {
    try {
      await menuItemService.update(item.id, { isAvailable: !item.isAvailable });
      await refreshMenu();
    } catch (err) {
      console.error('Failed to toggle availability:', err);
      toast.error(err.response?.data?.message || '更新状态失败');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>
    );
  }

  if (error && !restaurant) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => navigate('/')}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto p-6">
      {restaurant && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{restaurant.name}</h1>
              <p className="text-gray-600 mt-1">{restaurant.cuisine} • {restaurant.address}</p>
            </div>
            <div className="text-right">
              <div className="text-sm text-gray-500">餐厅 ID</div>
              <div className="text-lg font-semibold text-gray-900">#{restaurant.id}</div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1">
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold text-gray-900 mb-4">{formTitle}</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">菜品名称</label>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">分类</label>
                <input
                  type="text"
                  name="category"
                  value={formState.category}
                  onChange={handleChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">价格 (¥)</label>
                <input
                  type="number"
                  name="price"
                  min="0"
                  step="0.01"
                  value={formState.price}
                  onChange={handleChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">上传图片</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  disabled={imageUploading}
                  className="w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-red-50 file:text-red-700 hover:file:bg-red-100"
                />
                <p className="text-xs text-gray-500 mt-2">
                  {imageUploading ? '上传中，请稍候...' : '支持 JPG/PNG，最大 2MB'}
                </p>
                {formState.imageUrl && (
                  <div className="mt-3 border border-gray-200 rounded-lg p-3 bg-gray-50">
                    <img
                      src={formState.imageUrl}
                      alt="菜单预览"
                      className="w-full h-40 object-cover rounded-md"
                    />
                    <button
                      type="button"
                      onClick={handleClearImage}
                      className="mt-2 text-sm text-red-600 hover:text-red-700"
                    >
                      移除图片
                    </button>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">描述</label>
                <textarea
                  name="description"
                  value={formState.description}
                  onChange={handleChange}
                  rows="3"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isAvailable"
                    checked={formState.isAvailable}
                    onChange={handleChange}
                    className="rounded border-gray-300 text-red-600"
                  />
                  可售
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isVegetarian"
                    checked={formState.isVegetarian}
                    onChange={handleChange}
                    className="rounded border-gray-300 text-red-600"
                  />
                  素食
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isVegan"
                    checked={formState.isVegan}
                    onChange={handleChange}
                    className="rounded border-gray-300 text-red-600"
                  />
                  纯素
                </label>
                <div>
                  <label className="block text-sm text-gray-700 mb-1">辣度</label>
                  <input
                    type="number"
                    name="spicyLevel"
                    min="0"
                    max="5"
                    value={formState.spicyLevel}
                    onChange={handleChange}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                  />
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="submit"
                  disabled={saving}
                  className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-60"
                >
                  {saving ? '保存中...' : isEditing ? '保存修改' : '创建菜品'}
                </button>
                {isEditing && (
                  <button
                    type="button"
                    onClick={resetForm}
                    className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                  >
                    取消编辑
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>

        <div className="lg:col-span-2">
          <div className="bg-white rounded-lg shadow-md">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-900">菜品列表</h2>
              <p className="text-gray-600 text-sm mt-1">管理您当前的菜单内容</p>
            </div>

            {menuItems.length === 0 ? (
              <div className="p-12 text-center">
                <div className="text-gray-400 text-6xl mb-4">🍜</div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">暂无菜品</h3>
                <p className="text-gray-600">请先创建您的第一道菜品</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {menuItems.map((item) => (
                  <div key={item.id} className="p-6 hover:bg-gray-50 transition-colors">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <h3 className="text-lg font-semibold text-gray-900">{item.name}</h3>
                          <span
                            className={`px-3 py-1 rounded-full text-xs font-medium ${item.isAvailable ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}
                          >
                            {item.isAvailable ? '可售' : '已下架'}
                          </span>
                        </div>
                        <p className="text-sm text-gray-600 mb-2">{item.description || '暂无描述'}</p>
                        <div className="flex flex-wrap items-center gap-3 text-sm text-gray-600">
                          <span className="font-medium text-red-600">¥{Number(item.price || 0).toFixed(2)}</span>
                          <span className="bg-gray-100 px-2 py-1 rounded">{item.category || '未分类'}</span>
                          {item.isVegetarian && <span className="bg-green-50 text-green-700 px-2 py-1 rounded">素食</span>}
                          {item.isVegan && <span className="bg-emerald-50 text-emerald-700 px-2 py-1 rounded">纯素</span>}
                          {item.spicyLevel > 0 && (
                            <span className="bg-red-50 text-red-700 px-2 py-1 rounded">辣度 {item.spicyLevel}</span>
                          )}
                        </div>
                      </div>

                      <div className="flex flex-col gap-2">
                        <button
                          onClick={() => handleEdit(item)}
                          className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                        >
                          编辑
                        </button>
                        <button
                          onClick={() => toggleAvailability(item)}
                          className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                        >
                          {item.isAvailable ? '下架' : '上架'}
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="bg-white text-red-600 px-4 py-2 rounded-lg border border-red-300 hover:bg-red-50"
                        >
                          删除
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MenuManagement;
