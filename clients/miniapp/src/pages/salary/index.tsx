import { useCallback, useRef, useState } from 'react'
import Taro, { useDidShow, usePullDownRefresh } from 'virtual:taro/api'
import { Text, View } from 'virtual:taro/components'
import { CalendarOutlined, CashOutlined } from '@taroify/icons'
import { BottomTabBar, Card, GradientHeader, Notice, PageBody, RoundIcon, Screen, SectionTitle, StateView, StatusTag } from '../../components/ui'
import { getUser } from '../../lib/auth'
import { money, pageRecords, payslipStatus, status } from '../../lib/format'
import { textOf, type ApiRecord } from '../../lib/types'
import * as salary from '../../services/salary'

export default function SalaryPage() {
  const [records, setRecords] = useState<ApiRecord[]>([])
  const [employeeName, setEmployeeName] = useState('员工')
  const [total, setTotal] = useState('¥0.00')
  const [unbound, setUnbound] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const loadingRef = useRef(false)

  const load = useCallback(async () => {
    if (loadingRef.current) return
    const user = getUser()
    if (!user) {
      await Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    loadingRef.current = true
    if (!user.employeeId) {
      setUnbound(true)
      setRecords([])
      setEmployeeName('未绑定员工')
      setTotal('¥0.00')
      setError('')
      loadingRef.current = false
      setLoading(false)
      return
    }
    setUnbound(false)
    setLoading(true)
    setError('')
    try {
      const history = await salary.myHistory()
      setRecords(pageRecords(history.records))
      setEmployeeName(textOf(history, 'employeeName', '员工'))
      setTotal(money(history.totalActualAmount))
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : '工资记录加载失败'
      setError(message)
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      loadingRef.current = false
      setLoading(false)
    }
  }, [])

  useDidShow(() => {
    void load()
  })

  usePullDownRefresh(() => {
    void load().finally(() => Taro.stopPullDownRefresh())
  })

  if (unbound) return <Screen><GradientHeader title="我的工资" eyebrow="SALARY" subtitle="工资与工资条" compact /><PageBody lifted><StateView text="当前账号尚未绑定员工，绑定后可查看工资历史与工资条。" /></PageBody><BottomTabBar active="salary" /></Screen>

  return (
    <Screen>
      <GradientHeader title="我的工资" eyebrow="SALARY" subtitle={`${employeeName} · 发放记录与工资条`}>
        <View className="mt-8"><Text className="block text-sm text-white/70">累计实发</Text><Text className="mt-1 block text-2xl font-bold tracking-tight text-white">{total}</Text></View>
      </GradientHeader>
      <PageBody lifted>
        {error && records.length ? <Notice tone="danger">{error}</Notice> : null}
        <SectionTitle title="发放记录" subtitle={`共 ${records.length} 期`} />
        {error && !records.length && !loading ? <StateView text={error} actionText="重新加载" onAction={() => void load()} /> : records.length ? records.map((record) => {
          const detailId = textOf(record, 'detailId')
          const salaryMonth = textOf(record, 'salaryMonth')
          const [year, month] = salaryMonth.split('-')
          return <Card key={detailId || salaryMonth} className="p-0 shadow-none" onClick={() => void Taro.navigateTo({ url: `/pages/salary/detail?detailId=${detailId}&monthlyId=${textOf(record, 'monthlyId')}&month=${salaryMonth}` })}>
            <View className="flex gap-4 border-b border-[#edf0ed] py-5">
              <View className="w-20 shrink-0"><Text className="yellow-underline inline text-2xl font-bold leading-none">{month || '--'}</Text><Text className="ml-1 text-sm font-semibold">月</Text><Text className="mt-2 block text-xs text-[#a0a6a2]">{year || ''}</Text></View>
              <View className="min-w-0 flex-1"><View className="mb-3 flex items-start justify-between"><View className="flex items-center gap-2"><RoundIcon className="h-9 w-9 rounded-xl bg-[#eaf7f0] text-[#39987d]"><CashOutlined size={20} /></RoundIcon><Text className="font-bold">实发 {money(record.actualAmount)}</Text></View><StatusTag text={status(record.paymentStatus)} tone={textOf(record, 'paymentStatus') === 'paid' ? 'primary' : 'warning'} /></View><Text className="block text-sm text-[#8f9692]">应发 {money(record.payableAmount)} · 扣减 {money(record.deductionAmount)}</Text><View className="mt-3 flex items-center justify-between"><View className="flex items-center gap-1 text-[#a0a6a2]"><CalendarOutlined size={15} /><Text className="text-xs">{textOf(record, 'paymentDate', '未发放')}</Text></View><Text className="text-xs text-[#458d77]">工资条 · {payslipStatus(record.payslipStatus)}  ›</Text></View></View>
            </View>
          </Card>
        }) : !error ? <StateView text={loading ? '加载中…' : '暂无工资记录'} /> : null}
      </PageBody>
      <BottomTabBar active="salary" />
    </Screen>
  )
}
