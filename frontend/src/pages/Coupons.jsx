import { useNavigate } from 'react-router-dom';

const Coupons = () => {
  const navigate = useNavigate();

  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">优惠券管理</h1>
        <p className="text-gray-600 mb-6">优惠券功能还在开发中，正在规划创建与投放流程。</p>

        <div className="bg-pink-50 border border-pink-200 rounded-lg p-4 mb-6">
          <h2 className="text-lg font-semibold text-pink-900 mb-2">计划支持</h2>
          <ul className="text-pink-800 space-y-1">
            <li>• 新客优惠券</li>
            <li>• 满减与折扣券</li>
            <li>• 限时活动与投放分析</li>
          </ul>
        </div>

        <button
          onClick={() => navigate('/restaurant-home')}
          className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
        >
          返回餐厅首页
        </button>
      </div>
    </div>
  );
};

export default Coupons;
