import React from 'react';
import { useParams } from 'react-router-dom';
import { Package } from 'lucide-react';

const OrderDetail = () => {
  const { id } = useParams();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="text-center py-12">
        <Package className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h2 className="text-2xl font-bold text-gray-700 mb-2">
          订单详情页面
        </h2>
        <p className="text-gray-500">
          订单 ID: {id}
        </p>
        <p className="text-gray-400 mt-4">
          此功能正在开发中...
        </p>
      </div>
    </div>
  );
};

export default OrderDetail;
