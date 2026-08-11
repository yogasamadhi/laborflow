import Taro from 'virtual:taro/api'

const version = 1
const prefix = 'laborflow_field_draft_'

function key(name: string, scope: string): string {
  return `${prefix}${name}_${scope}`
}

export function loadDraft<T>(name: string, scope: string): T | null {
  const value = Taro.getStorageSync<unknown>(key(name, scope))
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return null
  const entry = value as { version?: unknown; data?: T }
  return entry.version === version && entry.data ? entry.data : null
}

export function saveDraft<T>(name: string, scope: string, data: T): void {
  Taro.setStorageSync(key(name, scope), { version, updatedAt: Date.now(), data })
}

export function clearDraft(name: string, scope: string): void {
  Taro.removeStorageSync(key(name, scope))
}
