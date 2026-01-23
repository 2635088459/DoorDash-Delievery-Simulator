import { create } from 'zustand';

const useNotificationStore = create((set) => ({
  notifications: [],
  unreadCount: 0,
  isConnected: false,
  
  addNotification: (notification) => 
    set((state) => ({
      notifications: [notification, ...state.notifications],
      unreadCount: notification.isRead ? state.unreadCount : state.unreadCount + 1,
    })),
  
  setNotifications: (notifications) =>
    set({
      notifications,
      unreadCount: notifications.filter(n => !n.isRead).length,
    }),
  
  markAsRead: (notificationId) =>
    set((state) => ({
      notifications: state.notifications.map(n =>
        n.id === notificationId ? { ...n, isRead: true } : n
      ),
      unreadCount: Math.max(0, state.unreadCount - 1),
    })),
  
  markAllAsRead: () =>
    set((state) => ({
      notifications: state.notifications.map(n => ({ ...n, isRead: true })),
      unreadCount: 0,
    })),
  
  clearNotifications: () =>
    set({ notifications: [], unreadCount: 0 }),
  
  setConnected: (isConnected) =>
    set({ isConnected }),
}));

export default useNotificationStore;
