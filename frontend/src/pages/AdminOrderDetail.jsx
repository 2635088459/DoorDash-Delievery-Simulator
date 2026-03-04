import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, PackageCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import useAuthStore from '../store/authStore';
import { orderService } from '../services/apiService';

const AdminOrderDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadOrder = async () => {
    try {
      setLoading(true);
      const data = await orderService.getByIdAdmin(id);
      setOrder(data);
    } catch (error) {
      console.error('Failed to load order:', error);
  toast.error(error.response?.data?.message || 'Failed to load order');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
  toast.error('Only admins can access order details');
      navigate('/');
      return;
    }
    loadOrder();
  }, [user?.role, id]);

  if (loading) {
  return <div className="p-10 text-center text-gray-500">Loading...</div>;
  }

  if (!order) {
  return <div className="p-10 text-center text-gray-500">Order not found</div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
        >
          <ArrowLeft className="w-4 h-4" />
          Back
        </button>

        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mt-6">
          <div className="flex items-center gap-3">
            <PackageCheck className="w-6 h-6 text-indigo-600" />
            <h1 className="text-2xl font-bold text-gray-900">Order {order.orderNumber}</h1>
            <span className="text-sm px-2 py-1 rounded-full bg-slate-100 text-slate-700">
              {order.status}
            </span>
          </div>
          <div className="grid md:grid-cols-2 gap-6 mt-6 text-sm text-gray-600">
            <div>
              <p className="font-semibold text-gray-900">Customer</p>
              <p>{order.customerName}</p>
              <p>{order.customerEmail}</p>
              <p>Address: {order.deliveryAddressStreet} {order.deliveryAddressCity}</p>
            </div>
            <div>
              <p className="font-semibold text-gray-900">Restaurant</p>
              <p>{order.restaurantName}</p>
              <p>Restaurant ID: {order.restaurantId}</p>
            </div>
          </div>

          <div className="mt-6 grid md:grid-cols-3 gap-4 text-sm text-gray-600">
            <div className="border border-gray-100 rounded-lg p-4">
              <p className="text-xs text-gray-500">Delivery fee</p>
              <p className="text-lg font-semibold">¥{Number(order.deliveryFee || 0).toFixed(2)}</p>
            </div>
            <div className="border border-gray-100 rounded-lg p-4">
              <p className="text-xs text-gray-500">Tip</p>
              <p className="text-lg font-semibold">¥{Number(order.tipAmount || 0).toFixed(2)}</p>
            </div>
            <div className="border border-gray-100 rounded-lg p-4">
              <p className="text-xs text-gray-500">Total</p>
              <p className="text-lg font-semibold">¥{Number(order.totalAmount || 0).toFixed(2)}</p>
            </div>
          </div>

          <div className="mt-6 text-sm text-gray-500">
            <p>Created at: {order.createdAt ? new Date(order.createdAt).toLocaleString() : '--'}</p>
            <p>Delivered at: {order.actualDelivery ? new Date(order.actualDelivery).toLocaleString() : '--'}</p>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mt-6">
          <h2 className="text-lg font-semibold text-gray-900">Order items</h2>
          <div className="mt-4 space-y-3">
            {(order.items || []).map((item) => (
              <div key={item.id} className="flex items-center justify-between border border-gray-100 rounded-lg p-3">
                <div>
                  <p className="text-sm font-semibold text-gray-900">{item.menuItemName}</p>
                  <p className="text-xs text-gray-500">Qty: {item.quantity}</p>
                </div>
                <p className="text-sm font-semibold">¥{Number(item.subtotal || 0).toFixed(2)}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminOrderDetail;
