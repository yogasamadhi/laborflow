import { get, post } from '../lib/request'
import type { ApiRecord } from '../lib/types'

export function login(data: ApiRecord): Promise<ApiRecord> {
  return post('/auth/login', data, { skipAuth: true, skipSessionRecovery: true })
}

export function wechatSession(code: string): Promise<ApiRecord> {
  return post('/auth/wechat/session', { code }, { skipAuth: true, skipSessionRecovery: true })
}

export function me(): Promise<ApiRecord> {
  return get('/auth/me')
}

export function logout(): Promise<ApiRecord> {
  return post('/auth/logout', {}, { skipSessionRecovery: true })
}
