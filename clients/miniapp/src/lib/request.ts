import Taro from 'virtual:taro/api'
import { apiBaseUrl } from './env'
import { clearLogin, getToken } from './auth'
import { isRecord, recordsOf, type ApiRecord } from './types'

interface RequestOptions {
  skipAuth?: boolean
  skipSessionRecovery?: boolean
  header?: Record<string, string>
}

let redirecting = false

export function errorMessage(value: unknown, fallback: string): string {
  return isRecord(value) && typeof value.message === 'string' && value.message ? value.message : fallback
}

export function recoverExpiredSession(): void {
  clearLogin()
  if (redirecting) return
  redirecting = true
  void Taro.reLaunch({
    url: '/pages/login/index',
    complete: () => {
      redirecting = false
    }
  })
}

function urlFor(path: string): string {
  return /^https?:\/\//.test(path) ? path : `${apiBaseUrl}${path}`
}

export async function request<T = ApiRecord>(path: string, method: keyof Taro.request.Method = 'GET', data: ApiRecord = {}, options: RequestOptions = {}): Promise<T> {
  const token = options.skipAuth ? '' : getToken()
  try {
    const response = await Taro.request<ApiRecord>({
      url: urlFor(path),
      method,
      data,
      header: {
        'content-type': 'application/json',
        ...options.header,
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    })
    const body = response.data
    if (response.statusCode === 401 && token && !options.skipSessionRecovery) {
      recoverExpiredSession()
      throw new Error('登录已失效')
    }
    if (response.statusCode === 403) throw new Error(errorMessage(body, '当前账号无权执行此操作'))
    if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 0) return body.data as T
    throw new Error(errorMessage(body, '请求失败'))
  } catch (error) {
    if (error instanceof Error) throw error
    throw new Error('网络异常，请检查网络后重试')
  }
}

export function get<T = ApiRecord>(path: string, data: ApiRecord = {}, options?: RequestOptions): Promise<T> {
  return request<T>(path, 'GET', data, options)
}

export function post<T = ApiRecord>(path: string, data: ApiRecord = {}, options?: RequestOptions): Promise<T> {
  return request<T>(path, 'POST', data, options)
}

export function put<T = ApiRecord>(path: string, data: ApiRecord = {}, options?: RequestOptions): Promise<T> {
  return request<T>(path, 'PUT', data, options)
}

export function del<T = ApiRecord>(path: string, data: ApiRecord = {}, options?: RequestOptions): Promise<T> {
  return request<T>(path, 'DELETE', data, options)
}

export async function getAllPages(path: string, data: ApiRecord = {}, options?: RequestOptions): Promise<ApiRecord[]> {
  const records: ApiRecord[] = []
  let page = 1
  for (;;) {
    const result = await get<ApiRecord>(path, { ...data, page, pageSize: 20 }, options)
    const pageRows = recordsOf(result)
    records.push(...pageRows)
    const total = Number(result.total || 0)
    if (pageRows.length === 0 || records.length >= total) return records
    page += 1
  }
}
