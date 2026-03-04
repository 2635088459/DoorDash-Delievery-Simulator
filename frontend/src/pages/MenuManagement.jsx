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
  return `Edit item #${editingItem?.id}`;
    }
  return 'Add menu item';
  }, [isEditing, editingItem]);

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.role !== 'RESTAURANT_OWNER') {
  setError('You do not have permission to access this page. This page is for restaurant owners only.');
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
  setError(err.response?.data?.message || 'Failed to load menu items');
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
  toast.error('Please upload an image file');
      return;
    }
    const maxSizeMb = 2;
    if (file.size > maxSizeMb * 1024 * 1024) {
  toast.error(`Image size cannot exceed ${maxSizeMb}MB`);
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
  toast.success('Image uploaded');
      } else {
  toast.error('Image upload failed, please try again');
      }
    } catch (err) {
      console.error('Failed to upload image:', err);
  toast.error(err.response?.data?.message || 'Image upload failed');
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
  toast.success('Menu item updated');
      } else {
        await menuItemService.create({ ...payload, restaurantId: restaurant.id });
  toast.success('Menu item created');
      }
      await refreshMenu();
      resetForm();
    } catch (err) {
      console.error('Failed to save menu item:', err);
  toast.error(err.response?.data?.message || 'Failed to save menu item');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (itemId) => {
  if (!confirm('Are you sure you want to delete this menu item?')) return;
    try {
      await menuItemService.remove(itemId);
  toast.success('Menu item deleted');
      await refreshMenu();
    } catch (err) {
      console.error('Failed to delete menu item:', err);
  toast.error(err.response?.data?.message || 'Failed to delete menu item');
    }
  };

  const toggleAvailability = async (item) => {
    try {
      await menuItemService.update(item.id, { isAvailable: !item.isAvailable });
      await refreshMenu();
    } catch (err) {
      console.error('Failed to toggle availability:', err);
  toast.error(err.response?.data?.message || 'Failed to update status');
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
            Back to home
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
              <div className="text-sm text-gray-500">Restaurant ID</div>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Item name</label>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Price (¥)</label>
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
                <label className="block text-sm font-medium text-gray-700 mb-1">Upload image</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  disabled={imageUploading}
                  className="w-full text-sm text-gray-600 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-red-50 file:text-red-700 hover:file:bg-red-100"
                />
                <p className="text-xs text-gray-500 mt-2">
                  {imageUploading ? 'Uploading, please wait...' : 'Supports JPG/PNG, max 2MB'}
                </p>
                {formState.imageUrl && (
                  <div className="mt-3 border border-gray-200 rounded-lg p-3 bg-gray-50">
                    <img
                      src={formState.imageUrl}
                      alt="Menu preview"
                      className="w-full h-40 object-cover rounded-md"
                    />
                    <button
                      type="button"
                      onClick={handleClearImage}
                      className="mt-2 text-sm text-red-600 hover:text-red-700"
                    >
                      Remove image
                    </button>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
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
                  Available
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isVegetarian"
                    checked={formState.isVegetarian}
                    onChange={handleChange}
                    className="rounded border-gray-300 text-red-600"
                  />
                  Vegetarian
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input
                    type="checkbox"
                    name="isVegan"
                    checked={formState.isVegan}
                    onChange={handleChange}
                    className="rounded border-gray-300 text-red-600"
                  />
                  Vegan
                </label>
                <div>
                  <label className="block text-sm text-gray-700 mb-1">Spice level</label>
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
                  {saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create item'}
                </button>
                {isEditing && (
                  <button
                    type="button"
                    onClick={resetForm}
                    className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                  >
                    Cancel edit
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>

        <div className="lg:col-span-2">
          <div className="bg-white rounded-lg shadow-md">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-xl font-bold text-gray-900">Menu items</h2>
              <p className="text-gray-600 text-sm mt-1">Manage your current menu</p>
            </div>

            {menuItems.length === 0 ? (
              <div className="p-12 text-center">
                <div className="text-gray-400 text-6xl mb-4">🍜</div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">No menu items</h3>
                <p className="text-gray-600">Create your first menu item to get started</p>
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
                            {item.isAvailable ? 'Available' : 'Unavailable'}
                          </span>
                        </div>
                        <p className="text-sm text-gray-600 mb-2">{item.description || 'No description'}</p>
                        <div className="flex flex-wrap items-center gap-3 text-sm text-gray-600">
                          <span className="font-medium text-red-600">¥{Number(item.price || 0).toFixed(2)}</span>
                          <span className="bg-gray-100 px-2 py-1 rounded">{item.category || 'Uncategorized'}</span>
                          {item.isVegetarian && <span className="bg-green-50 text-green-700 px-2 py-1 rounded">Vegetarian</span>}
                          {item.isVegan && <span className="bg-emerald-50 text-emerald-700 px-2 py-1 rounded">Vegan</span>}
                          {item.spicyLevel > 0 && (
                            <span className="bg-red-50 text-red-700 px-2 py-1 rounded">Spice {item.spicyLevel}</span>
                          )}
                        </div>
                      </div>

                      <div className="flex flex-col gap-2">
                        <button
                          onClick={() => handleEdit(item)}
                          className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => toggleAvailability(item)}
                          className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
                        >
                          {item.isAvailable ? 'Disable' : 'Enable'}
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="bg-white text-red-600 px-4 py-2 rounded-lg border border-red-300 hover:bg-red-50"
                        >
                          Delete
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
