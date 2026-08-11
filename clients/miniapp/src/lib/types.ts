export type ApiRecord = Record<string, unknown>

export interface PageResult {
  records?: ApiRecord[]
  total?: number
}

export interface SessionUser {
  userId: string
  employeeId: string | null
  username: string
  nickname: string
  roles: string[]
}

export interface Session {
  token: string
  user: SessionUser
}

export interface UploadedFile extends ApiRecord {
  id: string | number
  url?: string
  previewUrl?: string
  originalName?: string
}

export function isRecord(value: unknown): value is ApiRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function textOf(record: ApiRecord | undefined | null, key: string, fallback = ''): string {
  const value = record?.[key]
  return value === undefined || value === null ? fallback : String(value)
}

export function numberOf(record: ApiRecord | undefined | null, key: string, fallback = 0): number {
  const value = Number(record?.[key])
  return Number.isFinite(value) ? value : fallback
}

export function arrayOf(value: unknown): ApiRecord[] {
  return Array.isArray(value) ? value.filter(isRecord) : []
}

export function recordsOf(value: unknown): ApiRecord[] {
  if (Array.isArray(value)) return arrayOf(value)
  if (!isRecord(value)) return []
  return arrayOf(value.records)
}
