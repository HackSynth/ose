import axios, { type AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import type { ApiResponse } from '@/types';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('ose-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const request = async <T = any>(config: AxiosRequestConfig): Promise<T> => {
  try {
    const response = await client.request<ApiResponse<T>>(config);
    const payload = response.data;
    if (payload?.code !== 0) {
      ElMessage.error(payload?.message || '请求失败');
      throw new Error(payload?.message || '请求失败');
    }
    return payload.data;
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '网络请求失败';
    ElMessage.error(message);
    throw error;
  }
};

const http = {
  get: <T = any>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'GET', url }),
  post: <T = any>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'POST', url, data }),
  put: <T = any>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'PUT', url, data }),
  patch: <T = any>(url: string, data?: unknown, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'PATCH', url, data }),
  delete: <T = any>(url: string, config?: AxiosRequestConfig) => request<T>({ ...config, method: 'DELETE', url }),
};

export default http;
