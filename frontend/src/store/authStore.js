import { create } from 'zustand';

const getStoredToken = () => {
  const token = localStorage.getItem('token');
  if (token && token !== 'undefined' && token !== 'null') {
    return token;
  }
  const legacyToken = localStorage.getItem('accessToken');
  if (legacyToken && legacyToken !== 'undefined' && legacyToken !== 'null') {
    localStorage.setItem('token', legacyToken);
    return legacyToken;
  }
  return null;
};

const getTokenExpiry = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1] || ''));
    return payload?.exp ? payload.exp * 1000 : null;
  } catch (error) {
    return null;
  }
};

const useAuthStore = create((set) => ({
  user: null,
  token: getStoredToken(),
  isAuthenticated: !!getStoredToken(),
  
  login: (userData, token, refreshToken) => {
    if (token) {
      localStorage.setItem('token', token);
      localStorage.setItem('accessToken', token);
    }
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }
    localStorage.setItem('user', JSON.stringify(userData));
    set({ user: userData, token, isAuthenticated: true });
  },
  
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    set({ user: null, token: null, isAuthenticated: false });
  },
  
  loadUser: () => {
    const token = getStoredToken();
    const userStr = localStorage.getItem('user');
    if (token) {
      const expiry = getTokenExpiry(token);
      if (expiry && expiry <= Date.now()) {
        localStorage.removeItem('token');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        set({ user: null, token: null, isAuthenticated: false });
        return;
      }
    }
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        set({ user, token, isAuthenticated: true });
      } catch (error) {
        console.error('Failed to load user:', error);
      }
    } else {
      set({ user: null, token: null, isAuthenticated: false });
    }
  },
}));

export default useAuthStore;
