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

export interface MenuItem {
  id: string;
  restaurantId: string;
  name: string;
  description: string;
  price: number;
  vegetarian: boolean;
  available: boolean;
}

export interface CartItem {
  itemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Cart {
  userId: string;
  restaurantId: string;
  restaurantName: string;
  items: CartItem[];
  subtotal: number;
  deliveryFee: number;
  discount: number;
  total: number;
  couponCode: string | null;
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

  menu: (restaurantId: string, params?: { q?: string; vegetarian?: boolean }) =>
    api.get<ApiResponse<MenuItem[]>>(`/search/menu`, { params: { restaurantId, ...params } }),
};

export const cartApi = {
  get: () => api.get<ApiResponse<Cart>>('/cart'),

  addItem: (data: { itemId: string; restaurantId: string; itemName: string; unitPrice: number; quantity: number }) =>
    api.post<ApiResponse<Cart>>('/cart/items', data),

  updateQuantity: (itemId: string, quantity: number) =>
    api.patch<ApiResponse<Cart>>(`/cart/items/${itemId}`, { quantity }),

  removeItem: (itemId: string) =>
    api.delete<ApiResponse<Cart>>(`/cart/items/${itemId}`),

  clear: () => api.delete<ApiResponse<null>>('/cart'),

  applyCoupon: (code: string) =>
    api.post<ApiResponse<Cart>>('/cart/coupon', { code }),

  removeCoupon: () => api.delete<ApiResponse<Cart>>('/cart/coupon'),

  checkout: () => api.get<ApiResponse<{ cart: Cart; taxAmount: number; finalTotal: number; canPlaceOrder: boolean; message: string }>>('/cart/checkout'),
};

export interface OrderItem {
  id: string;
  menuItemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Order {
  id: string;
  userId: string;
  restaurantId: string;
  deliveryAddressId: string;
  status: string;
  subtotal: number;
  deliveryFee: number;
  tax: number;
  discount: number;
  totalAmount: number;
  paymentStatus: string;
  paymentId: string | null;
  couponCode: string | null;
  notes: string;
  items: OrderItem[];
  statusHistory: { id: string; status: string; note: string; createdAt: string }[];
  createdAt: string;
}

export const orderApi = {
  placeOrder: (data: { deliveryAddressId: string; notes?: string; couponCode?: string }) =>
    api.post<ApiResponse<Order>>('/orders', data),

  getOrder: (id: string) =>
    api.get<ApiResponse<Order>>(`/orders/${id}`),

  getOrders: (page?: number, size?: number) =>
    api.get<ApiResponse<Order[]>>('/orders', { params: { page, size } }),

  cancelOrder: (id: string) =>
    api.patch<ApiResponse<Order>>(`/orders/${id}/cancel`),

  getStatus: (id: string) =>
    api.get<ApiResponse<string>>(`/orders/${id}/status`),
};
