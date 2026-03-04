import { useNavigate } from 'react-router-dom';

const Coupons = () => {
  const navigate = useNavigate();

  return (
    <div className="max-w-5xl mx-auto p-6">
      <div className="bg-white rounded-lg shadow-md p-8">
  <h1 className="text-3xl font-bold text-gray-900 mb-2">Coupon Management</h1>
  <p className="text-gray-600 mb-6">Coupon features are under development. We are planning creation and distribution workflows.</p>

        <div className="bg-pink-50 border border-pink-200 rounded-lg p-4 mb-6">
          <h2 className="text-lg font-semibold text-pink-900 mb-2">Planned support</h2>
          <ul className="text-pink-800 space-y-1">
            <li>• New customer coupons</li>
            <li>• Threshold discounts and percentage off</li>
            <li>• Limited-time campaigns and distribution analytics</li>
          </ul>
        </div>

        <button
          onClick={() => navigate('/restaurant-home')}
          className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
        >
          Back to restaurant home
        </button>
      </div>
    </div>
  );
};

export default Coupons;
