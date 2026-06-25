import axios from "axios";
import { clearToken, getToken } from "./auth";

export const http = axios.create({
  baseURL: "/api",
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
      window.location.href = "/login";
    }
    return Promise.reject(error.response?.data ?? error);
  }
);

export type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
};

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const result = await http.get<unknown, ApiResult<T>>(url, { params });
  return result.data;
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const result = await http.post<unknown, ApiResult<T>>(url, data);
  return result.data;
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const result = await http.put<unknown, ApiResult<T>>(url, data);
  return result.data;
}

export async function deleteData<T>(url: string): Promise<T> {
  const result = await http.delete<unknown, ApiResult<T>>(url);
  return result.data;
}

