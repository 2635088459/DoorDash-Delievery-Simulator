import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { restaurantService } from '../services/apiService';

const weekDays = [
  { key: 'mon', label: '周一' },
  { key: 'tue', label: '周二' },
  { key: 'wed', label: '周三' },
  { key: 'thu', label: '周四' },
  { key: 'fri', label: '周五' },
  { key: 'sat', label: '周六' },
  { key: 'sun', label: '周日' },
];

const normalizeTime = (value) => {
  if (!value) return '';
  if (value.length >= 5) {
    return value.slice(0, 5);
  }
  return value;
};

const parseJsonValue = (value, fallback) => {
  if (!value) return fallback;
  try {
    return JSON.parse(value);
  } catch (err) {
    console.warn('Failed to parse schedule JSON:', err);
    return fallback;
  }
};

const BusinessHours = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [restaurant, setRestaurant] = useState(null);
  const [formState, setFormState] = useState({
    openingTime: '09:00',
    closingTime: '22:00',
    isActive: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [weeklySchedule, setWeeklySchedule] = useState(() => {
    return weekDays.reduce((acc, day) => {
      acc[day.key] = {
        isClosed: false,
        openingTime: '09:00',
        closingTime: '22:00',
      };
      return acc;
    }, {});
  });
  const [restDays, setRestDays] = useState([]);
  const [newRestDay, setNewRestDay] = useState('');

  const shouldReturnHome = searchParams.get('returnHome') === 'true';

  useEffect(() => {
    const loadRestaurant = async () => {
      try {
        setLoading(true);
        const ownerRestaurant = await restaurantService.getOwnerRestaurant();
        setRestaurant(ownerRestaurant);
        const storedWeekly = parseJsonValue(ownerRestaurant.weeklyScheduleJson, null);
        const storedRestDays = parseJsonValue(ownerRestaurant.restDaysJson, null);
        setFormState({
          openingTime: normalizeTime(ownerRestaurant.openingTime) || '09:00',
          closingTime: normalizeTime(ownerRestaurant.closingTime) || '22:00',
          isActive: ownerRestaurant.isActive ?? true,
        });
        if (storedWeekly) {
          setWeeklySchedule((prev) => ({
            ...prev,
            ...storedWeekly,
          }));
        }
        if (storedRestDays) {
          setRestDays(storedRestDays);
        }
        setError('');
      } catch (err) {
        console.error('Failed to load restaurant:', err);
        setError(err.response?.data?.message || '加载餐厅信息失败');
      } finally {
        setLoading(false);
      }
    };

    loadRestaurant();
  }, []);

  const getNextRestDay = () => {
    if (restDays.length === 0) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const upcoming = restDays
      .map((day) => new Date(day))
      .filter((date) => !Number.isNaN(date.getTime()))
      .sort((a, b) => a - b);

    const next = upcoming.find((date) => date >= today);
    if (!next) return restDays[0];
    return next.toISOString().slice(0, 10);
  };

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFormState((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!restaurant?.id) return;

    try {
      setSaving(true);
      const payload = {
        openingTime: formState.openingTime,
        closingTime: formState.closingTime,
        isActive: formState.isActive,
        weeklyScheduleJson: JSON.stringify(weeklySchedule),
        restDaysJson: JSON.stringify(restDays),
      };
      const updated = await restaurantService.update(restaurant.id, payload);
      setRestaurant(updated);
      toast.success('营业时间已更新');
      if (shouldReturnHome) {
        navigate('/restaurant-home', { replace: true });
      }
    } catch (err) {
      console.error('Failed to update business hours:', err);
      toast.error(err.response?.data?.message || '更新失败');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>
    );
  }

  const updateDaySchedule = (dayKey, field, value) => {
    setWeeklySchedule((prev) => ({
      ...prev,
      [dayKey]: {
        ...prev[dayKey],
        [field]: value,
      },
    }));
  };

  const addRestDay = () => {
    if (!newRestDay) return;
    if (restDays.includes(newRestDay)) {
      toast.error('该日期已在休息日中');
      return;
    }
    setRestDays((prev) => [...prev, newRestDay].sort());
    setNewRestDay('');
  };

  const removeRestDay = (day) => {
    setRestDays((prev) => prev.filter((item) => item !== day));
  };

  if (error && !restaurant) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-800">{error}</p>
          <button
            onClick={() => navigate('/restaurant-home')}
            className="mt-4 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            返回餐厅首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">营业时间</h1>
        <p className="text-gray-600 mb-6">设置餐厅的每日营业时间与营业状态。</p>

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="text-blue-900 font-semibold">当前餐厅</div>
              <div className="text-blue-800 mt-1">
                {restaurant?.name} · {restaurant?.city}
              </div>
            </div>
            <span
              className={`px-3 py-1 rounded-full text-sm font-medium ${formState.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}
            >
              {formState.isActive ? '营业中' : '暂停营业'}
            </span>
          </div>
          <div className="text-sm text-blue-700 mt-3">
            今日营业时间：{formState.openingTime || '--:--'} - {formState.closingTime || '--:--'}
          </div>
          <div className="text-sm text-blue-700 mt-1">
            下次休息日：{getNextRestDay() || '暂无设置'}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">开始营业时间</label>
              <input
                type="time"
                name="openingTime"
                value={formState.openingTime}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">结束营业时间</label>
              <input
                type="time"
                name="closingTime"
                value={formState.closingTime}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
                required
              />
            </div>
          </div>

          <label className="flex items-center gap-3 text-sm text-gray-700">
            <input
              type="checkbox"
              name="isActive"
              checked={formState.isActive}
              onChange={handleChange}
              className="rounded border-gray-300 text-red-600"
            />
            当前营业中
          </label>

          <div className="border border-gray-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">按周设置营业时间</h2>
            <div className="space-y-4">
              {weekDays.map((day) => (
                <div key={day.key} className="flex flex-col md:flex-row md:items-center gap-3">
                  <div className="w-16 text-sm font-medium text-gray-700">{day.label}</div>
                  <label className="flex items-center gap-2 text-sm text-gray-600">
                    <input
                      type="checkbox"
                      checked={weeklySchedule[day.key]?.isClosed || false}
                      onChange={(event) => updateDaySchedule(day.key, 'isClosed', event.target.checked)}
                      className="rounded border-gray-300 text-red-600"
                    />
                    休息
                  </label>
                  <div className="flex flex-1 items-center gap-2">
                    <input
                      type="time"
                      value={weeklySchedule[day.key]?.openingTime || ''}
                      onChange={(event) => updateDaySchedule(day.key, 'openingTime', event.target.value)}
                      disabled={weeklySchedule[day.key]?.isClosed}
                      className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500 disabled:bg-gray-100"
                    />
                    <span className="text-gray-400">-</span>
                    <input
                      type="time"
                      value={weeklySchedule[day.key]?.closingTime || ''}
                      onChange={(event) => updateDaySchedule(day.key, 'closingTime', event.target.value)}
                      disabled={weeklySchedule[day.key]?.isClosed}
                      className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500 disabled:bg-gray-100"
                    />
                  </div>
                </div>
              ))}
            </div>
            <p className="text-xs text-gray-500 mt-3">按周配置已保存到数据库，并会同步到所有设备。</p>
          </div>

          <div className="border border-gray-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">休息日配置</h2>
            <div className="flex flex-col md:flex-row gap-3">
              <input
                type="date"
                value={newRestDay}
                onChange={(event) => setNewRestDay(event.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-red-500"
              />
              <button
                type="button"
                onClick={addRestDay}
                className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
              >
                添加休息日
              </button>
            </div>
            {restDays.length === 0 ? (
              <p className="text-sm text-gray-500 mt-3">暂无休息日配置</p>
            ) : (
              <div className="flex flex-wrap gap-2 mt-3">
                {restDays.map((day) => (
                  <span key={day} className="inline-flex items-center gap-2 px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">
                    {day}
                    <button
                      type="button"
                      onClick={() => removeRestDay(day)}
                      className="text-gray-400 hover:text-red-600"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
            <p className="text-xs text-gray-500 mt-3">休息日配置已保存到数据库。</p>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={saving}
              className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 disabled:opacity-60"
            >
              {saving ? '保存中...' : '保存设置'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/restaurant-home')}
              className="bg-white text-gray-700 px-4 py-2 rounded-lg border border-gray-300 hover:bg-gray-50"
            >
              返回餐厅首页
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BusinessHours;
