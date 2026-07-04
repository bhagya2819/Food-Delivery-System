import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface User {
  id: string;
  email: string;
  phone: string;
  fullName: string;
  role: string;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Restaurant {
  id: string;
  name: string;
  cuisineType: string;
  city: string;
  state: string;
  rating: number;
  distanceKm: number;
}

export const authApi = {
  register: (data: { email: string; password: string; fullName: string; phone?: string }) =>
    api.post<ApiResponse<AuthResponse>>('/users/register', data),

  login: (data: { email: string; password: string }) =>
    api.post<ApiResponse<AuthResponse>>('/users/login', data),
};

export const searchApi = {
  restaurants: (params: { q?: string; cuisine?: string; city?: string }) =>
    api.get<ApiResponse<Restaurant[]>>('/search/restaurants', { params }),
};
