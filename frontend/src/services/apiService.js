import api from './api';

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response.data;
  },

  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },
};

export const notificationService = {
  getAll: async () => {
    const response = await api.get('/notifications');
    return response.data;
  },

  getUnread: async () => {
    const response = await api.get('/notifications/unread');
    return response.data;
  },

  getUnreadCount: async () => {
    const response = await api.get('/notifications/unread/count');
    return response.data;
  },

  markAsRead: async (notificationId) => {
    const response = await api.put(`/notifications/${notificationId}/read`);
    return response.data;
  },

  markAllAsRead: async () => {
    const response = await api.put('/notifications/read-all');
    return response.data;
  },

  delete: async (notificationId) => {
    const response = await api.delete(`/notifications/${notificationId}`);
    return response.data;
  },
};

export const restaurantService = {
  getAll: async () => {
    const response = await api.get('/restaurants');
    return response.data;
  },

  getById: async (id) => {
    const response = await api.get(`/restaurants/${id}`);
    return response.data;
  },

  search: async (params) => {
    const response = await api.get('/restaurants/search', { params });
    return response.data;
  },
};

export const menuService = {
  getByRestaurant: async (restaurantId) => {
    const response = await api.get(`/restaurants/${restaurantId}/menu`);
    return response.data;
  },
};

export const menuItemService = {
  getByRestaurant: async (restaurantId) => {
    const response = await api.get(`/menu-items/restaurant/${restaurantId}/available`);
    return response.data;
  },

  getById: async (menuItemId) => {
    const response = await api.get(`/menu-items/${menuItemId}`);
    return response.data;
  },
};

export const orderService = {
  create: async (orderData) => {
    const response = await api.post('/orders', orderData);
    return response.data;
  },

  getMyOrders: async () => {
    const response = await api.get('/orders/my');
    return response.data;
  },

  getById: async (orderId) => {
    const response = await api.get(`/orders/${orderId}`);
    return response.data;
  },

  cancel: async (orderId) => {
    const response = await api.put(`/orders/${orderId}/cancel`);
    return response.data;
  },
};

export const deliveryService = {
  getByOrder: async (orderId) => {
    const response = await api.get(`/deliveries/order/${orderId}`);
    return response.data;
  },

  trackDelivery: async (deliveryId) => {
    const response = await api.get(`/deliveries/${deliveryId}/track`);
    return response.data;
  },
};
