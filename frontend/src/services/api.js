import axios from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api';
const appBasePath = import.meta.env.BASE_URL || '/';

const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

const refreshClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

const decodeTokenExpiry = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1] || ''));
    return payload?.exp ? payload.exp * 1000 : null;
  } catch (error) {
    return null;
  }
};

let refreshPromise = null;

const refreshAccessToken = async (refreshToken) => {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post('/auth/refresh', null, { params: { refreshToken } })
      .then((response) => response.data?.accessToken)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

// Request interceptor to add token
api.interceptors.request.use(
  async (config) => {
    const rawToken = localStorage.getItem('token') || localStorage.getItem('accessToken');
    const token = rawToken && rawToken !== 'undefined' && rawToken !== 'null' ? rawToken : null;
    const refreshToken = localStorage.getItem('refreshToken');

    if (token && refreshToken) {
      const expiry = decodeTokenExpiry(token);
      if (expiry && expiry <= Date.now()) {
        try {
          const newAccessToken = await refreshAccessToken(refreshToken);
          if (newAccessToken) {
            localStorage.setItem('token', newAccessToken);
            localStorage.setItem('accessToken', newAccessToken);
            config.headers.Authorization = `Bearer ${newAccessToken}`;
            return config;
          }
        } catch (refreshError) {
          localStorage.removeItem('token');
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          window.location.href = `${appBasePath}login`;
          return config;
        }
      }
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config || {};
    const refreshToken = localStorage.getItem('refreshToken');

    if ((status === 401 || status === 403) && !originalRequest._retry && refreshToken) {
      originalRequest._retry = true;
      try {
        const refreshResponse = await refreshClient.post('/auth/refresh', null, {
          params: { refreshToken },
        });
        const newAccessToken = refreshResponse?.data?.accessToken;
        if (newAccessToken) {
          localStorage.setItem('token', newAccessToken);
          localStorage.setItem('accessToken', newAccessToken);
          originalRequest.headers = {
            ...(originalRequest.headers || {}),
            Authorization: `Bearer ${newAccessToken}`,
          };
          return api(originalRequest);
        }
      } catch (refreshError) {
        localStorage.removeItem('token');
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = `${appBasePath}login`;
        return Promise.reject(refreshError);
      }
    }

    if (status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      window.location.href = `${appBasePath}login`;
    }
    return Promise.reject(error);
  }
);

export default api;
