import { get, post } from '../lib/request'
import { confirmDailyReport, confirmSettlement, confirmVariationOrder, dailyReportDetail, rejectDailyReport, rejectSettlement, rejectVariationOrder, settlementDetail, variationOrderDetail } from './project'
import { approveMonthly, monthlyAudits, monthlyDetails, rejectMonthly } from './salary'
import type { ApiRecord } from '../lib/types'

export function list(params: ApiRecord = {}): Promise<ApiRecord> {
  return get('/todos', { page: 1, pageSize: 20, ...params })
}

export async function detail(item: ApiRecord): Promise<ApiRecord> {
  const type = String(item.type || '')
  const businessId = String(item.businessId || '')
  if (type === 'salary') {
    const [header, details, audits] = await Promise.all([
      get<ApiRecord>(`/salary/monthly/${businessId}`),
      monthlyDetails(businessId),
      monthlyAudits(businessId)
    ])
    return { header, details, audits }
  }
  if (type === 'report_pm' || type === 'report_final') return dailyReportDetail(businessId)
  if (type === 'variation') return variationOrderDetail(businessId)
  if (type === 'settlement') return settlementDetail(businessId)
  throw new Error('当前待办暂无可用详情')
}

export function approve(item: ApiRecord, data: ApiRecord = {}): Promise<ApiRecord> {
  const type = String(item.type || '')
  const businessId = String(item.businessId || '')
  const remark = String(data.remark || '')
  if (type === 'salary') return approveMonthly(businessId, remark)
  if (type === 'report_pm' || type === 'report_final') {
    return confirmDailyReport(businessId, type === 'report_final' ? 'final' : 'pm', data)
  }
  if (type === 'variation') {
    const confirmedAmount = Number(data.confirmedAmount)
    if (!Number.isFinite(confirmedAmount) || confirmedAmount < 0) throw new Error('请输入非负确认金额')
    return confirmVariationOrder(businessId, { confirmedAmount, remark })
  }
  if (type === 'settlement') return confirmSettlement(businessId, data)
  throw new Error('不支持的待办类型')
}

export function reject(item: ApiRecord, remark: string): Promise<ApiRecord> {
  const type = String(item.type || '')
  const businessId = String(item.businessId || '')
  if (type === 'salary') return rejectMonthly(businessId, remark)
  if (type === 'report_pm' || type === 'report_final') return rejectDailyReport(businessId, { remark })
  if (type === 'variation') return rejectVariationOrder(businessId, { remark })
  if (type === 'settlement') return rejectSettlement(businessId, { remark })
  return post('/todos/reject', { id: businessId, remark })
}
