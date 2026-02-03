import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bike, MapPin, DollarSign, Clock, TrendingUp, Award } from 'lucide-react';

const DriverHome = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    setUser(storedUser);
  }, []);

  const quickActions = [
    {
      icon: MapPin,
      title: '开始接单',
      description: '查看可接订单列表',
      color: 'bg-blue-500',
      hoverColor: 'hover:bg-blue-600',
      link: '/driver-dashboard'
    },
    {
      icon: Clock,
      title: '配送历史',
      description: '查看历史配送记录',
      color: 'bg-green-500',
      hoverColor: 'hover:bg-green-600',
      link: '/delivery-history'
    },
    {
      icon: DollarSign,
      title: '我的收入',
      description: '查看收入和提现',
      color: 'bg-yellow-500',
      hoverColor: 'hover:bg-yellow-600',
      link: '/earnings'
    },
    {
      icon: Award,
      title: '骑手等级',
      description: '查看等级和奖励',
      color: 'bg-purple-500',
      hoverColor: 'hover:bg-purple-600',
      link: '/driver-level'
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Welcome Header */}
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 rounded-2xl shadow-xl p-8 mb-8 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold mb-2">
                准备好接单了吗，{user?.firstName || 'Driver'} 🚗
              </h1>
              <p className="text-blue-100 text-lg">
                开始配送，赚取收入
              </p>
            </div>
            <Bike className="w-24 h-24 opacity-20" />
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-blue-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">今日配送</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">0</p>
                <p className="text-xs text-gray-400 mt-1">笔订单</p>
              </div>
              <MapPin className="w-12 h-12 text-blue-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-green-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">今日收入</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">¥0</p>
                <p className="text-xs text-gray-400 mt-1">配送费</p>
              </div>
              <DollarSign className="w-12 h-12 text-green-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-yellow-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">本周收入</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">¥0</p>
                <p className="text-xs text-gray-400 mt-1">总计</p>
              </div>
              <TrendingUp className="w-12 h-12 text-yellow-500 opacity-20" />
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-md p-6 border-l-4 border-purple-500">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm font-medium">骑手评分</p>
                <p className="text-3xl font-bold text-gray-900 mt-1">--</p>
                <p className="text-xs text-gray-400 mt-1">满分5.0</p>
              </div>
              <Award className="w-12 h-12 text-purple-500 opacity-20" />
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">快捷功能</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {quickActions.map((action, index) => {
              const Icon = action.icon;
              return (
                <button
                  key={index}
                  onClick={() => navigate(action.link)}
                  className="bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 text-left group"
                >
                  <div className="flex items-start space-x-4">
                    <div className={`${action.color} ${action.hoverColor} rounded-lg p-3 group-hover:scale-110 transition-transform`}>
                      <Icon className="w-8 h-8 text-white" />
                    </div>
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-900 mb-1 group-hover:text-blue-600 transition-colors">
                        {action.title}
                      </h3>
                      <p className="text-sm text-gray-600">
                        {action.description}
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Tips Section */}
        <div className="mt-8 bg-green-50 border border-green-200 rounded-xl p-6">
          <h3 className="text-lg font-semibold text-green-900 mb-3">💡 配送小贴士</h3>
          <ul className="space-y-2 text-green-800">
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>及时取餐，确保食物新鲜</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>使用导航软件，选择最优路线</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>保持礼貌，提供优质服务</span>
            </li>
            <li className="flex items-start">
              <span className="mr-2">•</span>
              <span>注意交通安全，安全第一</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default DriverHome;
