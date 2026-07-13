import axios from "axios";
import { clearToken, getToken } from "./auth";

type ErrorPayload = { message?: string };

export const REQUEST_ERROR_EVENT = "laborflow:request-error";

export type RequestErrorDetail = {
  status?: number;
  message: string;
};

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;
  if (axios.isAxiosError<ErrorPayload>(error)) {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || "请求失败";
    return new ApiError(message, status);
  }
  return error instanceof Error ? new ApiError(error.message) : new ApiError("请求失败");
}

function emitRequestError(detail: RequestErrorDetail) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent<RequestErrorDetail>(REQUEST_ERROR_EVENT, { detail }));
  }
}

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
    const apiError = toApiError(error);
    if (apiError.status === 401) {
      clearToken();
      if (typeof window !== "undefined" && window.location.pathname !== "/login") {
        const next = `${window.location.pathname}${window.location.search}${window.location.hash}`;
        window.location.assign(`/login?reason=session-expired&next=${encodeURIComponent(next)}`);
      }
    } else {
      emitRequestError({
        status: apiError.status,
        message: apiError.message || (apiError.status === 403 ? "当前账号无权执行此操作" : "请求失败，请稍后重试")
      });
    }
    return Promise.reject(apiError);
  }
);

export type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type PageResult<T> = {
  records: T[];
  total: string;
  page: string;
  pageSize: string;
};

function unwrapResult<T>(result: ApiResult<T>): T {
  if (result.code !== 0) {
    throw new ApiError(result.message || "请求失败");
  }
  return result.data;
}

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const result = await http.get<unknown, ApiResult<T>>(url, { params });
  return unwrapResult(result);
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const result = await http.post<unknown, ApiResult<T>>(url, data);
  return unwrapResult(result);
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const result = await http.put<unknown, ApiResult<T>>(url, data);
  return unwrapResult(result);
}

export async function deleteData<T>(url: string): Promise<T> {
  const result = await http.delete<unknown, ApiResult<T>>(url);
  return unwrapResult(result);
}

export async function downloadFile(url: string, filename: string, params?: Record<string, unknown>) {
  const blob = await http.get<unknown, Blob>(url, { params, responseType: "blob" });
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(objectUrl);
}

export async function uploadFile(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return postData<{ id: string; filePath: string; originalName: string }>("/files/upload", formData);
}
