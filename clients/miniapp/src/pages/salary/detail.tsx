import { useCallback, useEffect, useState } from 'react'
import Taro, { useRouter } from 'virtual:taro/api'
import { Text, View } from 'virtual:taro/components'
import { CashOutlined } from '@taroify/icons'
import { Card, KeyValueRows, Notice, PageBody, PageTitle, PrimaryButton, RoundIcon, Screen, SecondaryButton, StateView, StatusTag, TextAreaField } from '../../components/ui'
import { money, payslipStatus, status } from '../../lib/format'
import { arrayOf, textOf, type ApiRecord } from '../../lib/types'
import * as salary from '../../services/salary'

export default function SalaryDetailPage() {
  const router = useRouter()
  const detailId = String(router.params.detailId || '')
  const month = String(router.params.month || '')
  const [detail, setDetail] = useState<ApiRecord | null>(null)
  const [items, setItems] = useState<ApiRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    if (!detailId) {
      setError('工资条编号缺失')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const [nextDetail, nextItems] = await Promise.all([salary.detail(detailId), salary.detailItems(detailId)])
      setDetail(nextDetail)
      setItems(arrayOf(nextItems))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '工资条加载失败')
    } finally {
      setLoading(false)
    }
  }, [detailId])

  useEffect(() => {
    void load()
  }, [load])

  const canConfirm = Boolean(detail) && detail?.payslipStatus !== 'confirmed' && detail?.payslipStatus !== 'exception'
  const confirm = async () => {
    if (!canConfirm || processing) return
    const result = await Taro.showModal({ title: '确认工资条', content: '请确认工资项目和金额无误后再提交确认。', confirmText: '确认无误' })
    if (!result.confirm) return
    setProcessing(true)
    try {
      await salary.confirmPayslip(detailId)
      Taro.showToast({ title: '工资条已确认', icon: 'success' })
      await load()
    } catch (actionError) {
      Taro.showToast({ title: actionError instanceof Error ? actionError.message : '确认失败', icon: 'none' })
    } finally {
      setProcessing(false)
    }
  }
  const submitFeedback = async () => {
    const value = feedback.trim()
    if (!value) {
      Taro.showToast({ title: '请填写异议说明', icon: 'none' })
      return
    }
    setProcessing(true)
    try {
      await salary.feedbackPayslip(detailId, value)
      setFeedbackOpen(false)
      setFeedback('')
      Taro.showToast({ title: '异议已提交', icon: 'success' })
      await load()
    } catch (actionError) {
      Taro.showToast({ title: actionError instanceof Error ? actionError.message : '异议提交失败', icon: 'none' })
    } finally {
      setProcessing(false)
    }
  }

  if (loading) return <Screen><PageBody><PageTitle title="工资条" subtitle={month} /><StateView text="加载中…" /></PageBody></Screen>
  if (!detail) return <Screen><PageBody><PageTitle title="工资条" subtitle={month} /><Notice tone="danger">{error || '工资条不存在'}</Notice></PageBody></Screen>

  const notice = detail.payslipStatus === 'exception'
    ? `已提出异议：${textOf(detail, 'payslipFeedback', '等待后台处理')}`
    : detail.payslipStatus === 'confirmed' ? '本工资条已确认，无需重复操作。' : '确认前请核对工资项目、扣减和实发金额。'
  return (
    <Screen>
      <PageBody>
        <PageTitle title="工资条" subtitle={month || textOf(detail, 'salaryMonth')} />
        <Card className="mint-soft shadow-none"><View className="flex items-center justify-between"><View className="flex items-center gap-3"><RoundIcon className="bg-white text-[#39987d]"><CashOutlined size={25} /></RoundIcon><View><Text className="block text-sm text-[#77857e]">实发工资</Text><Text className="mt-1 block text-[34px] font-bold tracking-tight">{money(detail.actualAmount)}</Text></View></View><StatusTag text={payslipStatus(detail.payslipStatus)} tone={detail.payslipStatus === 'exception' ? 'danger' : detail.payslipStatus === 'confirmed' ? 'primary' : 'warning'} /></View></Card>
        <Notice tone={detail.payslipStatus === 'exception' ? 'warning' : 'info'}>{notice}</Notice>
        <Card><KeyValueRows rows={[{ label: '应发金额', value: money(detail.payableAmount) }, { label: '扣减金额', value: money(detail.deductionAmount) }, { label: '实发金额', value: money(detail.actualAmount) }, { label: '发放状态', value: status(detail.paymentStatus) }, { label: '发放日期', value: textOf(detail, 'paymentDate', '-') }]} /></Card>
        <Card><Text className="mb-3 block text-lg font-bold">工资项目</Text>{items.length ? items.map((item) => <View key={textOf(item, 'id', textOf(item, 'salaryItemName'))} className="flex justify-between border-b border-[#edf0ed] py-3 last:border-0"><View><Text className="block text-sm font-medium">{textOf(item, 'salaryItemName')}</Text><Text className="mt-1 block text-xs text-[#969d99]">{textOf(item, 'salaryItemType') === 'deduction' ? '扣减' : '应发'}</Text></View><Text className={`font-semibold ${textOf(item, 'salaryItemType') === 'deduction' ? 'text-[#d96759]' : 'text-[#398d73]'}`}>{money(item.amount)}</Text></View>) : <Text className="text-sm text-[#969d99]">暂无工资项目</Text>}</Card>
        {canConfirm ? <View className="flex gap-3"><View className="flex-1"><SecondaryButton disabled={processing} onClick={() => setFeedbackOpen(true)}>提出异议</SecondaryButton></View><View className="flex-1"><PrimaryButton loading={processing} onClick={() => void confirm()}>确认无误</PrimaryButton></View></View> : null}
        {feedbackOpen ? <Card><TextAreaField label="异议说明" value={feedback} required placeholder="请描述需要核对的工资项目或金额" onChange={setFeedback} /><View className="flex gap-3"><View className="flex-1"><SecondaryButton disabled={processing} onClick={() => setFeedbackOpen(false)}>取消</SecondaryButton></View><View className="flex-1"><PrimaryButton loading={processing} onClick={() => void submitFeedback()}>提交异议</PrimaryButton></View></View></Card> : null}
      </PageBody>
    </Screen>
  )
}
