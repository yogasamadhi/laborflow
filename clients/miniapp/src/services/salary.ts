import { get, getAllPages, post } from '../lib/request'
import type { ApiRecord } from '../lib/types'

export function myHistory(): Promise<ApiRecord> {
  return get('/statistics/salary/my-history')
}

export function monthlyList(params: ApiRecord = {}): Promise<ApiRecord> {
  return get('/salary/monthly', { page: 1, pageSize: 20, ...params })
}

export function monthlyDetails(monthlyId: string | number, params: ApiRecord = {}): Promise<ApiRecord[]> {
  return getAllPages(`/salary/monthly/${monthlyId}/details`, params)
}

export function detail(detailId: string | number): Promise<ApiRecord> {
  return get(`/salary/details/${detailId}`)
}

export function detailItems(detailId: string | number): Promise<ApiRecord[]> {
  return get(`/salary/details/${detailId}/items`)
}

export function confirmPayslip(detailId: string | number): Promise<ApiRecord> {
  return post(`/salary/details/${detailId}/payslip/confirm`)
}

export function feedbackPayslip(detailId: string | number, feedback: string): Promise<ApiRecord> {
  return post(`/salary/details/${detailId}/payslip/feedback`, { feedback })
}

export function monthlyAudits(monthlyId: string | number): Promise<ApiRecord[]> {
  return get(`/salary/monthly/${monthlyId}/audits`)
}

export function approveMonthly(monthlyId: string | number, remark: string): Promise<ApiRecord> {
  return post(`/salary/monthly/${monthlyId}/approve`, { remark })
}

export function rejectMonthly(monthlyId: string | number, remark: string): Promise<ApiRecord> {
  return post(`/salary/monthly/${monthlyId}/reject`, { remark })
}
