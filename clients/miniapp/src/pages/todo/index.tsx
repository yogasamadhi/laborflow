import { useCallback, useRef, useState } from 'react'
import Taro, { useDidShow, usePullDownRefresh } from 'virtual:taro/api'
import { Button, Text, View } from 'virtual:taro/components'
import { ClockOutlined, TodoListOutlined } from '@taroify/icons'
import { BottomTabBar, Card, GradientHeader, KeyValueRows, Notice, PageBody, PrimaryButton, RoundIcon, Screen, SectionTitle, SecondaryButton, StateView, StatusTag, TextAreaField, TextField } from '../../components/ui'
import { getUser, hasTodoRole } from '../../lib/auth'
import { dateTime, money, status, statusTone } from '../../lib/format'
import { arrayOf, textOf, type ApiRecord } from '../../lib/types'
import * as todo from '../../services/todo'

type ActionMode = '' | 'approve' | 'reject'

function detailRowsFor(item: ApiRecord, detail: ApiRecord): Array<{ label: string; value: string }> {
  const type = textOf(item, 'type')
  if (type === 'salary') {
    const header = detail.header && typeof detail.header === 'object' ? detail.header as ApiRecord : {}
    return [
      { label: '工资月份', value: textOf(header, 'salaryMonth', '-') },
      { label: '直属部门', value: textOf(header, 'departmentName', '历史全公司') },
      { label: '工资人数', value: textOf(header, 'employeeCount', '0') },
      { label: '应发合计', value: money(header.payableAmount) },
      { label: '扣款合计', value: money(header.deductionAmount) },
      { label: '实发合计', value: money(header.actualAmount) },
      { label: '审批记录', value: `${arrayOf(detail.audits).length} 条` }
    ]
  }
  if (type === 'report_pm' || type === 'report_final') {
    const report = detail.report && typeof detail.report === 'object' ? detail.report as ApiRecord : {}
    return [
      { label: '项目', value: textOf(detail, 'projectName', '-') },
      { label: '施工段', value: textOf(detail, 'sectionName', '-') },
      { label: '工序', value: textOf(detail, 'workProcessName', '-') },
      { label: '工作日期', value: textOf(report, 'workDate', '-') },
      { label: '工程量', value: `${textOf(report, 'completedQuantity', '0')}${textOf(report, 'unit')}` },
      { label: '定价依据', value: textOf(report, 'pricingBasis', textOf(report, 'pricingStatus') === 'unpriced' ? '未定价' : '-') },
      { label: '单价', value: money(report.unitPriceSnapshot) },
      { label: '产值', value: money(report.outputValue) },
      { label: '附件', value: `${arrayOf(detail.files).length} 个` }
    ]
  }
  if (type === 'variation') {
    const order = detail.order && typeof detail.order === 'object' ? detail.order as ApiRecord : {}
    return [
      { label: '项目', value: textOf(detail, 'projectName', '-') },
      { label: '施工段', value: textOf(detail, 'sectionName', '-') },
      { label: '签证类型', value: textOf(order, 'variationType', '-') },
      { label: '发生日期', value: textOf(order, 'occurrenceDate', '-') },
      { label: '原因', value: textOf(order, 'reason', '-') },
      { label: '预估金额', value: money(order.estimatedAmount) },
      { label: '附件', value: `${arrayOf(detail.files).length} 个` }
    ]
  }
  const settlement = detail.settlement && typeof detail.settlement === 'object' ? detail.settlement as ApiRecord : detail
  return [
    { label: '班组', value: textOf(settlement, 'teamName', '-') },
    { label: '结算月份', value: textOf(settlement, 'settlementMonth', '-') },
    { label: '确认工程量', value: textOf(settlement, 'confirmedQuantity', '0') },
    { label: '结算金额', value: money(settlement.settlementAmount) },
    { label: '实付金额', value: money(settlement.actualPayAmount) },
    { label: '明细行', value: `${arrayOf(detail.details).length} 条` }
  ]
}

export default function TodoPage() {
  const [allowed, setAllowed] = useState<boolean | null>(null)
  const [rows, setRows] = useState<ApiRecord[]>([])
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [active, setActive] = useState<ApiRecord | null>(null)
  const [detailRows, setDetailRows] = useState<Array<{ label: string; value: string }>>([])
  const [detailLoading, setDetailLoading] = useState(false)
  const [actionMode, setActionMode] = useState<ActionMode>('')
  const [remark, setRemark] = useState('')
  const [confirmedAmount, setConfirmedAmount] = useState('')
  const [processing, setProcessing] = useState(false)
  const requestVersion = useRef(0)
  const loadingMoreRef = useRef(false)
  const detailLoadingRef = useRef(false)
  const processingRef = useRef(false)

  const load = useCallback(async (reset: boolean) => {
    if (!hasTodoRole()) return
    if (!reset && loadingMoreRef.current) return
    const version = ++requestVersion.current
    const nextPage = reset ? 1 : page + 1
    if (reset) setLoading(true)
    else {
      loadingMoreRef.current = true
      setLoadingMore(true)
    }
    setError('')
    try {
      const result = await todo.list({ page: nextPage, pageSize: 20 })
      const incoming = arrayOf(result.records)
      if (version !== requestVersion.current) return
      setRows((current) => reset ? incoming : [...current, ...incoming])
      setPage(nextPage)
      setHasMore(incoming.length === 20)
    } catch (loadError) {
      if (version !== requestVersion.current) return
      const message = loadError instanceof Error ? loadError.message : '待办加载失败'
      setError(message)
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      if (!reset) loadingMoreRef.current = false
      if (version === requestVersion.current) {
        setLoading(false)
        setLoadingMore(false)
      }
    }
  }, [page])

  useDidShow(() => {
    const user = getUser()
    if (!user) {
      void Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    const canReview = hasTodoRole()
    setAllowed(canReview)
    if (canReview) void load(true)
  })

  usePullDownRefresh(() => {
    if (!hasTodoRole()) {
      void Taro.stopPullDownRefresh()
      return
    }
    void load(true).finally(() => Taro.stopPullDownRefresh())
  })

  const openDetail = async (item: ApiRecord) => {
    if (detailLoadingRef.current || processingRef.current) return
    detailLoadingRef.current = true
    setActive(item)
    setDetailRows([])
    setActionMode('')
    setRemark('')
    setConfirmedAmount('')
    setDetailLoading(true)
    try {
      const detail = await todo.detail(item)
      setDetailRows(detailRowsFor(item, detail))
    } catch (detailError) {
      Taro.showToast({ title: detailError instanceof Error ? detailError.message : '详情加载失败', icon: 'none' })
      setActive(null)
    } finally {
      detailLoadingRef.current = false
      setDetailLoading(false)
    }
  }

  const confirmAction = async () => {
    if (!active || !actionMode || processingRef.current) return
    const normalizedRemark = remark.trim()
    if (actionMode === 'reject' && !normalizedRemark) {
      Taro.showToast({ title: '驳回时请填写处理原因', icon: 'none' })
      return
    }
    if (actionMode === 'approve' && textOf(active, 'type') === 'variation' && !confirmedAmount) {
      Taro.showToast({ title: '请输入确认金额', icon: 'none' })
      return
    }
    if (actionMode === 'approve' && textOf(active, 'type') === 'variation' && (!Number.isFinite(Number(confirmedAmount)) || Number(confirmedAmount) < 0)) {
      Taro.showToast({ title: '请输入非负确认金额', icon: 'none' })
      return
    }
    processingRef.current = true
    setProcessing(true)
    try {
      if (actionMode === 'approve') await todo.approve(active, { remark: normalizedRemark, confirmedAmount })
      else await todo.reject(active, normalizedRemark)
      Taro.showToast({ title: actionMode === 'approve' ? '已同意' : '已驳回', icon: 'success' })
      setActive(null)
      setActionMode('')
      await load(true)
    } catch (actionError) {
      Taro.showToast({ title: actionError instanceof Error ? actionError.message : '处理失败，请重试', icon: 'none' })
    } finally {
      processingRef.current = false
      setProcessing(false)
    }
  }

  if (allowed === null) return <Screen><GradientHeader title="待办审批" eyebrow="WORKFLOW" subtitle="审批任务与处理记录" compact /><PageBody lifted><StateView text="正在校验审批权限…" /></PageBody><BottomTabBar active="todo" /></Screen>
  if (!allowed) return <Screen><GradientHeader title="待办审批" eyebrow="WORKFLOW" subtitle="审批任务与处理记录" compact /><PageBody lifted><StateView text="当前账号没有待办处理权限。" /></PageBody><BottomTabBar active="todo" /></Screen>
  return (
    <Screen>
      <GradientHeader title="待办审批" eyebrow="WORKFLOW" subtitle="需要你确认的业务流转">
        <View className="mt-8 flex items-end gap-3"><Text className="text-2xl font-bold leading-none text-white">{rows.length}</Text><Text className="pb-1 text-sm text-white/70">项待处理任务</Text></View>
      </GradientHeader>
      <PageBody lifted>
        {error && rows.length ? <Notice tone="danger">{error}</Notice> : null}
        <SectionTitle title="等待处理" subtitle="审批范围由服务端实时校验" />
        {rows.map((item) => <Card key={textOf(item, 'id', textOf(item, 'businessId'))} className="flex items-center gap-4 p-4" onClick={() => void openDetail(item)}><RoundIcon className="bg-[#eaf7f0] text-[#39987d]"><TodoListOutlined size={24} /></RoundIcon><View className="min-w-0 flex-1"><View className="mb-2 flex items-start justify-between gap-3"><Text className="block truncate text-base font-bold">{textOf(item, 'title', '待办审批')}</Text><StatusTag text={status(item.status)} tone={statusTone(item.status)} /></View><View className="flex items-center gap-1 text-[#9aa19d]"><ClockOutlined size={14} /><Text className="text-xs">{dateTime(item.occurredAt)} · {textOf(item, 'type') === 'salary' ? textOf(item, 'departmentName', '历史全公司') : textOf(item, 'type')}</Text></View></View><Text className="text-xl text-[#a1a8a4]">›</Text></Card>)}
        {error && !rows.length && !loading ? <StateView text={error} actionText="重新加载" onAction={() => void load(true)} /> : null}
        {!error && !rows.length && !loading ? <StateView text="暂无待办，所有审批都已处理。" /> : null}
        {loading ? <StateView text="加载中…" /> : null}
        {hasMore && rows.length ? <Button className="mb-4 rounded-full border-0 bg-[#eef5f1] text-sm text-[#4a806f]" loading={loadingMore} disabled={loadingMore || loading} onClick={() => void load(false)}>加载更多</Button> : null}
      </PageBody>
      <BottomTabBar active="todo" />
      {active ? <View className="fixed inset-0 z-20 flex items-end bg-black/35" onClick={() => !processing && setActive(null)}><View className="max-h-[86vh] w-full overflow-y-auto rounded-t-[32px] bg-white px-5 pb-8 pt-5" onClick={(event) => event.stopPropagation()}><View className="mx-auto mb-5 h-1 w-10 rounded-full bg-[#dce1de]" /><View className="mb-5 flex items-center justify-between"><Text className="text-xl font-bold">待办详情</Text><Text className="text-sm text-[#3a997d]" onClick={() => !processing && setActive(null)}>关闭</Text></View>{detailLoading ? <StateView text="详情加载中…" /> : <KeyValueRows rows={detailRows} />} {!actionMode ? <View className="mt-6 flex gap-3"><View className="flex-1"><SecondaryButton tone="danger" disabled={processing || detailLoading} onClick={() => setActionMode('reject')}>驳回</SecondaryButton></View><View className="flex-1"><PrimaryButton disabled={detailLoading} onClick={() => setActionMode('approve')}>同意</PrimaryButton></View></View> : <View className="mt-6"><Text className="mb-4 block text-lg font-bold">{actionMode === 'approve' ? '确认同意' : '确认驳回'}</Text>{actionMode === 'approve' && textOf(active, 'type') === 'variation' ? <TextField label="确认金额" value={confirmedAmount} required type="digit" placeholder="请输入非负金额" onChange={setConfirmedAmount} /> : null}<TextAreaField label={actionMode === 'reject' ? '处理原因' : '审批备注'} value={remark} required={actionMode === 'reject'} placeholder={actionMode === 'reject' ? '请填写驳回原因' : '选填'} onChange={setRemark} /><View className="flex gap-3"><View className="flex-1"><SecondaryButton disabled={processing} onClick={() => setActionMode('')}>取消</SecondaryButton></View><View className="flex-1"><PrimaryButton loading={processing} onClick={() => void confirmAction()}>{actionMode === 'approve' ? '确认同意' : '确认驳回'}</PrimaryButton></View></View></View>}</View></View> : null}
    </Screen>
  )
}
