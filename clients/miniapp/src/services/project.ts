import { del, get, getAllPages, post, put } from '../lib/request'
import type { ApiRecord } from '../lib/types'

export function projects(params: ApiRecord = {}): Promise<ApiRecord[]> {
  return getAllPages('/projects', params)
}

export function sections(projectId: string | number): Promise<ApiRecord[]> {
  return getAllPages(`/projects/${projectId}/sections`)
}

export function workProcesses(): Promise<ApiRecord[]> {
  return getAllPages('/work-processes', { status: 'active' })
}

export function teams(): Promise<ApiRecord[]> {
  return getAllPages('/teams', { status: 'active' })
}

export function dailyReports(params: ApiRecord = {}): Promise<ApiRecord> {
  return get('/daily-work-reports', { page: 1, pageSize: 20, ...params })
}

export function myDailyReports(params: ApiRecord = {}): Promise<ApiRecord> {
  return dailyReports({ mine: true, ...params })
}

export function createDailyReport(data: ApiRecord): Promise<ApiRecord> {
  return post('/daily-work-reports', data)
}

export function dailyReportDetail(id: string | number): Promise<ApiRecord> {
  return get(`/daily-work-reports/${id}`)
}

export function contractItems(params: ApiRecord = {}): Promise<ApiRecord[]> {
  return getAllPages('/project-contract-items', { status: 'active', ...params })
}

export function previewDailyReportPricing(data: ApiRecord): Promise<ApiRecord> {
  return post('/daily-work-reports/pricing-preview', data)
}

export function updateDailyReport(id: string | number, data: ApiRecord): Promise<ApiRecord> {
  return put(`/daily-work-reports/${id}`, data)
}

export function submitDailyReport(id: string | number): Promise<ApiRecord> {
  return post(`/daily-work-reports/${id}/submit`)
}

export function confirmDailyReport(id: string | number, stage: 'pm' | 'final', data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/daily-work-reports/${id}/${stage === 'final' ? 'final-confirm' : 'pm-confirm'}`, data)
}

export function rejectDailyReport(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/daily-work-reports/${id}/reject`, data)
}

export function attachDailyReportFile(id: string | number, fileId: string | number): Promise<ApiRecord> {
  return post(`/daily-work-reports/${id}/files`, { fileId, fileType: 'photo' })
}

export function dailyReportConfirms(id: string | number): Promise<ApiRecord> {
  return get(`/daily-work-reports/${id}/confirms`)
}

export function dailyReportFiles(id: string | number): Promise<ApiRecord> {
  return get(`/daily-work-reports/${id}/files`)
}

export function removeDailyReportFile(id: string | number, fileId: string | number): Promise<ApiRecord> {
  return del(`/daily-work-reports/${id}/files/${fileId}`)
}

export function variationOrders(params: ApiRecord = {}): Promise<ApiRecord> {
  return get('/variation-orders', { page: 1, pageSize: 20, ...params })
}

export function myVariationOrders(params: ApiRecord = {}): Promise<ApiRecord> {
  return variationOrders({ mine: true, ...params })
}

export function createVariationOrder(data: ApiRecord): Promise<ApiRecord> {
  return post('/variation-orders', data)
}

export function variationOrderDetail(id: string | number): Promise<ApiRecord> {
  return get(`/variation-orders/${id}`)
}

export function updateVariationOrder(id: string | number, data: ApiRecord): Promise<ApiRecord> {
  return put(`/variation-orders/${id}`, data)
}

export function submitVariationOrder(id: string | number): Promise<ApiRecord> {
  return post(`/variation-orders/${id}/submit`)
}

export function confirmVariationOrder(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/variation-orders/${id}/confirm`, data)
}

export function rejectVariationOrder(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/variation-orders/${id}/reject`, data)
}

export function attachVariationFile(id: string | number, fileId: string | number): Promise<ApiRecord> {
  return post(`/variation-orders/${id}/files`, { fileId, fileType: 'photo' })
}

export function variationConfirms(id: string | number): Promise<ApiRecord> {
  return get(`/variation-orders/${id}/confirms`)
}

export function variationFiles(id: string | number): Promise<ApiRecord> {
  return get(`/variation-orders/${id}/files`)
}

export function removeVariationFile(id: string | number, fileId: string | number): Promise<ApiRecord> {
  return del(`/variation-orders/${id}/files/${fileId}`)
}

export function settlements(params: ApiRecord = {}): Promise<ApiRecord> {
  return get('/team-settlements', { page: 1, pageSize: 20, ...params })
}

export function settlementDetail(id: string | number): Promise<ApiRecord> {
  return get(`/team-settlements/${id}`)
}

export function confirmSettlement(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/team-settlements/${id}/confirm`, data)
}

export function rejectSettlement(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/team-settlements/${id}/reject`, data)
}

export function markSettlementPaid(id: string | number, data: ApiRecord = {}): Promise<ApiRecord> {
  return post(`/team-settlements/${id}/mark-paid`, data)
}
