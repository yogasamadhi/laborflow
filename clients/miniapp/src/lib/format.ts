import { arrayOf, isRecord, recordsOf, type ApiRecord } from './types'

const statusMap: Record<string, string> = {
  draft: '草稿',
  pending_approval: '待审核',
  approved: '已审核',
  paid: '已发放',
  unpaid: '未发放',
  submitted: '待确认',
  pm_confirmed: '项目经理已确认',
  confirmed: '已确认',
  rejected: '已驳回',
  not_started: '未开工',
  in_progress: '施工中',
  paused: '暂停',
  completed: '已完工'
}

export const maxPhotoCount = 6

export function money(value: unknown): string {
  const number = Number(value || 0)
  return `¥${Number.isFinite(number) ? number.toFixed(2) : '0.00'}`
}

export function status(value: unknown): string {
  const normalized = value === undefined || value === null ? '' : String(value)
  return statusMap[normalized] || normalized || '-'
}

export function statusTone(value: unknown): 'primary' | 'warning' | 'danger' {
  if (value === 'rejected') return 'danger'
  if (['draft', 'submitted', 'pending_approval', 'pm_confirmed'].includes(String(value))) return 'warning'
  return 'primary'
}

export function payslipStatus(value: unknown): string {
  if (value === 'confirmed') return '已确认'
  if (value === 'exception') return '有异常'
  return '待确认'
}

export function today(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const date = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${date}`
}

export function pageRecords(value: unknown): ApiRecord[] {
  return recordsOf(value)
}

export function detailFiles(value: unknown): ApiRecord[] {
  if (!isRecord(value)) return []
  return arrayOf(value.files)
    .map((binding) => binding.file)
    .filter(isRecord)
}

export function dateTime(value: unknown): string {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}
