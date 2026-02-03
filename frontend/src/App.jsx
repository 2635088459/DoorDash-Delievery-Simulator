import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import useAuthStore from './store/authStore';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Restaurants from './pages/Restaurants';
import RestaurantDetail from './pages/RestaurantDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import Notifications from './pages/Notifications';
import RestaurantManagement from './pages/RestaurantManagement';
import RestaurantHome from './pages/RestaurantHome';
import DriverDashboard from './pages/DriverDashboard';
import DriverHome from './pages/DriverHome';

// Components
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';

function App() {
  const loadUser = useAuthStore((state) => state.loadUser);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Protected Routes - Different home pages based on role */}
          <Route path="/" element={<PrivateRoute><Home /></PrivateRoute>} />
          <Route path="/restaurant-home" element={<PrivateRoute><RestaurantHome /></PrivateRoute>} />
          <Route path="/driver-home" element={<PrivateRoute><DriverHome /></PrivateRoute>} />
          
          {/* Customer Routes */}
          <Route path="/restaurants" element={<PrivateRoute><Restaurants /></PrivateRoute>} />
          <Route path="/restaurants/:id" element={<PrivateRoute><RestaurantDetail /></PrivateRoute>} />
          <Route path="/cart" element={<PrivateRoute><Cart /></PrivateRoute>} />
          <Route path="/checkout" element={<PrivateRoute><Checkout /></PrivateRoute>} />
          <Route path="/orders" element={<PrivateRoute><Orders /></PrivateRoute>} />
          <Route path="/orders/:id" element={<PrivateRoute><OrderDetail /></PrivateRoute>} />
          
          {/* Common Routes */}
          <Route path="/notifications" element={<PrivateRoute><Notifications /></PrivateRoute>} />
          
          {/* Restaurant Owner Routes */}
          <Route path="/restaurant-management" element={<PrivateRoute><RestaurantManagement /></PrivateRoute>} />
          
          {/* Driver Routes */}
          <Route path="/driver-dashboard" element={<PrivateRoute><DriverDashboard /></PrivateRoute>} />
          
          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 3000,
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              duration: 4000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
      </div>
    </Router>
  );
}

export default App;
