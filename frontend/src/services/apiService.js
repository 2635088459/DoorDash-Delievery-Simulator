import api from './api';

const ACTIVE_RESTAURANT_KEY = 'activeRestaurantId';

const getActiveRestaurantId = () => {
  const stored = localStorage.getItem(ACTIVE_RESTAURANT_KEY);
  if (!stored) return null;
  const parsed = Number(stored);
  return Number.isNaN(parsed) ? null : parsed;
};

const setActiveRestaurantId = (restaurantId) => {
  if (!restaurantId) {
    localStorage.removeItem(ACTIVE_RESTAURANT_KEY);
    return;
  }
  localStorage.setItem(ACTIVE_RESTAURANT_KEY, String(restaurantId));
};

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response.data;
  },

  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
  },

  testSession: async () => {
    const response = await api.get('/auth/test');
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

  getMyRestaurants: async () => {
    const response = await api.get('/restaurants/my');
    return response.data;
  },

  getOwnerRestaurant: async () => {
    const response = await api.get('/restaurants/my');
    const restaurants = response.data || [];
    if (!restaurants.length) {
      const error = new Error("You haven't created a restaurant yet. Please create one first.");
      error.response = { status: 404, data: { message: "You haven't created a restaurant yet. Please create one first." } };
      throw error;
    }

    const activeId = getActiveRestaurantId();
    const selected = restaurants.find((item) => item.id === activeId) || restaurants[0];
    if (selected?.id) {
      setActiveRestaurantId(selected.id);
    }
    return selected;
  },

  getActiveRestaurantId,
  setActiveRestaurantId,

  update: async (restaurantId, payload) => {
    const response = await api.put(`/restaurants/${restaurantId}`, payload);
    return response.data;
  },

  create: async (payload) => {
    const response = await api.post('/restaurants', payload);
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

  getAllByRestaurant: async (restaurantId) => {
    const response = await api.get(`/menu-items/restaurant/${restaurantId}`);
    return response.data;
  },

  getById: async (menuItemId) => {
    const response = await api.get(`/menu-items/${menuItemId}`);
    return response.data;
  },

  create: async (menuItemData) => {
    const response = await api.post('/menu-items', menuItemData);
    return response.data;
  },

  update: async (menuItemId, menuItemData) => {
    const response = await api.put(`/menu-items/${menuItemId}`, menuItemData);
    return response.data;
  },

  remove: async (menuItemId) => {
    const response = await api.delete(`/menu-items/${menuItemId}`);
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

  getByIdAdmin: async (orderId) => {
    const response = await api.get(`/orders/admin/${orderId}`);
    return response.data;
  },

  cancel: async (orderId) => {
    const response = await api.put(`/orders/${orderId}/cancel`);
    return response.data;
  },

  getRestaurantOrders: async (restaurantId) => {
    const response = await api.get(`/orders/restaurant/${restaurantId}`);
    return response.data;
  },

  updateStatus: async (orderId, payload) => {
    const response = await api.put(`/orders/${orderId}/status`, payload);
    return response.data;
  },
};

export const deliveryService = {
  getAvailable: async () => {
    const response = await api.get('/deliveries/available');
    return response.data;
  },

  getMyDeliveries: async () => {
    const response = await api.get('/deliveries/my');
    return response.data;
  },

  acceptOrder: async (orderId) => {
    const response = await api.post(`/deliveries/accept/${orderId}`);
    return response.data;
  },

  markPickedUp: async (orderId) => {
    const response = await api.put(`/deliveries/${orderId}/picked-up`);
    return response.data;
  },

  markInTransit: async (orderId) => {
    const response = await api.put(`/deliveries/${orderId}/in-transit`);
    return response.data;
  },

  markDelivered: async (orderId) => {
    const response = await api.put(`/deliveries/${orderId}/delivered`);
    return response.data;
  },

  getByOrder: async (orderId) => {
    const response = await api.get(`/deliveries/order/${orderId}`);
    return response.data;
  },

  trackDelivery: async (deliveryId) => {
    const response = await api.get(`/deliveries/${deliveryId}/track`);
    return response.data;
  },
};

export const driverService = {
  getMe: async () => {
    const response = await api.get('/drivers/me');
    return response.data;
  },

  getEarnings: async () => {
    const response = await api.get('/drivers/earnings');
    return response.data;
  },
};

export const ticketService = {
  getAll: async () => {
    const response = await api.get('/tickets');
    return response.data;
  },

  getById: async (ticketId) => {
    const response = await api.get(`/tickets/${ticketId}`);
    return response.data;
  },

  create: async (payload) => {
    const response = await api.post('/tickets', payload);
    return response.data;
  },

  updateStatus: async (ticketId, payload) => {
    const response = await api.put(`/tickets/${ticketId}/status`, payload);
    return response.data;
  },

  addComment: async (ticketId, payload) => {
    const response = await api.post(`/tickets/${ticketId}/comments`, payload);
    return response.data;
  },

  getSamples: async (ticketId) => {
    const response = await api.get(`/tickets/${ticketId}/samples`);
    return response.data;
  },

  getSummary: async (ticketId) => {
    const response = await api.get(`/tickets/${ticketId}/summary`);
    return response.data;
  },

  executeAction: async (ticketId, payload) => {
    const response = await api.post(`/tickets/${ticketId}/actions`, payload);
    return response.data;
  },

  getActionLogs: async (ticketId) => {
    const response = await api.get(`/tickets/${ticketId}/actions/logs`);
    return response.data;
  },

  updateActionResult: async (ticketId, actionId, payload) => {
    const response = await api.patch(`/tickets/${ticketId}/actions/${actionId}`, payload);
    return response.data;
  },

  getAllActionLogs: async () => {
    const response = await api.get('/tickets/actions/logs');
    return response.data;
  },
};

export const userAdminService = {
  getAll: async () => {
    const response = await api.get('/users');
    return response.data;
  },

  updateRole: async (userId, payload) => {
    const response = await api.patch(`/users/${userId}/role`, payload);
    return response.data;
  },

  toggleStatus: async (userId, isActive) => {
    const response = await api.patch(`/users/${userId}/status`, null, {
      params: { isActive },
    });
    return response.data;
  },
};

export const reviewService = {
  getByRestaurant: async (restaurantId) => {
    const response = await api.get(`/reviews/restaurant/${restaurantId}`);
    return response.data;
  },

  getRestaurantRating: async (restaurantId) => {
    const response = await api.get(`/reviews/restaurant/${restaurantId}/rating`);
    return response.data;
  },

  replyToReview: async (reviewId, payload) => {
    const response = await api.put(`/reviews/${reviewId}/reply`, payload);
    return response.data;
  },
};

export const uploadService = {
  uploadMenuItemImage: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/uploads/menu-items', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },
};
