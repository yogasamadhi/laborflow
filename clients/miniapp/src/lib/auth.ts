import Taro from 'virtual:taro/api'
import { isRecord, type Session, type SessionUser } from './types'

const tokenKey = 'laborflow_token'
const userKey = 'laborflow_user'

function requiredText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}

function optionalId(value: unknown): string | null {
  const normalized = String(value ?? '').trim()
  return /^[1-9]\d*$/.test(normalized) ? normalized : null
}

function normalizeUser(value: unknown): SessionUser | null {
  if (!isRecord(value)) return null
  const userId = optionalId(value.userId)
  const username = requiredText(value.username)
  if (!userId || !username) return null
  return {
    userId,
    employeeId: optionalId(value.employeeId),
    username,
    nickname: requiredText(value.nickname),
    roles: Array.isArray(value.roles)
      ? value.roles.filter((role): role is string => typeof role === 'string').map((role) => role.trim()).filter(Boolean)
      : []
  }
}

export function clearLogin(): void {
  Taro.removeStorageSync(tokenKey)
  Taro.removeStorageSync(userKey)
}

export function getSession(): Session | null {
  const token = requiredText(Taro.getStorageSync<unknown>(tokenKey))
  const cachedUser = Taro.getStorageSync<unknown>(userKey)
  const user = normalizeUser(cachedUser)
  if (!token || !user) {
    if (token || cachedUser) clearLogin()
    return null
  }
  return { token, user }
}

export function getUser(): SessionUser | null {
  return getSession()?.user || null
}

export function getToken(): string {
  return getSession()?.token || ''
}

export function setLogin(payload: unknown): SessionUser {
  if (!isRecord(payload)) {
    clearLogin()
    throw new Error('登录信息不完整，请重新登录')
  }
  const token = requiredText(payload.token)
  const user = normalizeUser(payload)
  if (!token || !user) {
    clearLogin()
    throw new Error('登录信息不完整，请重新登录')
  }
  Taro.setStorageSync(tokenKey, token)
  Taro.setStorageSync(userKey, user)
  return user
}

export function refreshUser(payload: unknown): SessionUser {
  const session = getSession()
  if (!session) throw new Error('登录已失效')
  if (!isRecord(payload)) throw new Error('登录信息不完整，请重新登录')
  return setLogin({ ...payload, token: session.token })
}

export function hasAnyRole(expectedRoles: string[]): boolean {
  const roles = getUser()?.roles || []
  return expectedRoles.some((role) => roles.includes(role))
}

export function hasTodoRole(): boolean {
  return hasAnyRole(['ADMIN', 'BOSS', 'FINANCE', 'MANAGER'])
}

export function roleText(roles: string[]): string {
  if (roles.includes('ADMIN')) return '系统管理员'
  if (roles.includes('BOSS')) return '老板/管理层'
  if (roles.includes('FINANCE')) return '财务'
  if (roles.includes('MANAGER')) return '项目经理/管理人员'
  return '现场/员工'
}
