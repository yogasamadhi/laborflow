import { useCallback, useRef, useState } from 'react'
import Taro, { useDidShow, usePullDownRefresh } from 'virtual:taro/api'
import { ScrollView, Text, View } from 'virtual:taro/components'
import { BarChartOutlined, CalendarOutlined, CashOutlined, Edit, NotesOutlined, TodoListOutlined } from '@taroify/icons'
import { BottomTabBar, Card, FloatingAction, GradientHeader, Metric, Notice, PageBody, RoundIcon, Screen, SectionTitle, StateView, StatusTag } from '../../components/ui'
import { getUser, hasTodoRole, roleText } from '../../lib/auth'
import { money, pageRecords, status, statusTone, today } from '../../lib/format'
import { numberOf, textOf, type ApiRecord, type SessionUser } from '../../lib/types'
import * as project from '../../services/project'
import * as salary from '../../services/salary'
import * as todo from '../../services/todo'

interface HomeState {
  user: SessionUser | null
  projects: ApiRecord[]
  todos: ApiRecord[]
  totalActual: string
  latestMonth: string
  latestStatus: string
  todayReport: ApiRecord | null
  todoCount: number
  draftCount: number
  rejectedCount: number
  variationCount: number
}

const initialState: HomeState = {
  user: null,
  projects: [],
  todos: [],
  totalActual: '¥0.00',
  latestMonth: '暂无',
  latestStatus: '暂无工资',
  todayReport: null,
  todoCount: 0,
  draftCount: 0,
  rejectedCount: 0,
  variationCount: 0
}

export default function HomePage() {
  const [state, setState] = useState<HomeState>(initialState)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const loadingRef = useRef(false)

  const load = useCallback(async () => {
    if (loadingRef.current) return
    const user = getUser()
    if (!user) {
      await Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    loadingRef.current = true
    setState((current) => ({ ...current, user }))
    setLoading(true)
    setLoadError('')
    const failed: string[] = []
    const fallback = <T,>(scope: string, value: T) => (_error: unknown): T => {
      failed.push(scope)
      return value
    }
    try {
      const manager = hasTodoRole()
      const requestCount = 3 + (user.employeeId ? 1 : 0) + (manager ? 1 : 0)
      const [history, projects, todos, reports, variations] = await Promise.all([
        user.employeeId ? salary.myHistory().catch(fallback('salary', null)) : Promise.resolve(null),
        project.projects({ status: 'in_progress' }).catch(fallback('projects', [])),
        manager ? todo.list().catch(fallback('todos', { records: [], total: 0 })) : Promise.resolve({ records: [], total: 0 }),
        project.myDailyReports({ pageSize: 20 }).catch(fallback('reports', { records: [] })),
        project.myVariationOrders({ pageSize: 20 }).catch(fallback('variations', { records: [] }))
      ])
      const reportRows = pageRecords(reports)
      const variationRows = pageRecords(variations)
      const todoRows = pageRecords(todos)
      const currentReport = reportRows.find((item) => textOf(item, 'workDate') === today()) || null
      const latestPayment = history && typeof history === 'object' ? (history as ApiRecord).latestPaymentRecord : null
      const latestPaymentRecord = latestPayment && typeof latestPayment === 'object' ? latestPayment as ApiRecord : null
      setState({
        user,
        projects: projects.slice(0, 3),
        todos: todoRows.slice(0, 4),
        totalActual: history && typeof history === 'object' ? money((history as ApiRecord).totalActualAmount) : '¥0.00',
        latestMonth: textOf(latestPaymentRecord, 'salaryMonth', '暂无'),
        latestStatus: latestPaymentRecord ? status(latestPaymentRecord.paymentStatus) : '暂无工资',
        todayReport: currentReport,
        todoCount: numberOf(todos as ApiRecord, 'total', todoRows.length),
        draftCount: reportRows.filter((item) => textOf(item, 'status') === 'draft').length,
        rejectedCount: reportRows.filter((item) => textOf(item, 'status') === 'rejected').length + variationRows.filter((item) => textOf(item, 'status') === 'rejected').length,
        variationCount: variationRows.length
      })
      if (failed.length === requestCount) setLoadError('数据加载失败，请检查网络后重试。')
      else if (failed.length > 0) Taro.showToast({ title: '部分数据加载失败', icon: 'none' })
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

  const navigate = (url: string) => void Taro.navigateTo({ url })

  if (!state.user) return <Screen><StateView text="正在校验登录状态…" /></Screen>

  const now = new Date()
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  const monthDate = `${String(now.getMonth() + 1).padStart(2, '0')}月${String(now.getDate()).padStart(2, '0')}日`
  const shortcuts = [
    { label: '工资', icon: <CashOutlined size={28} />, action: () => void Taro.switchTab({ url: '/pages/salary/index' }) },
    { label: '填日报', icon: <NotesOutlined size={28} />, action: () => navigate('/pages/report/create') },
    { label: '报签证', icon: <Edit size={28} />, action: () => navigate('/pages/variation/create') },
    ...(hasTodoRole() ? [{ label: '办审批', icon: <TodoListOutlined size={28} />, action: () => void Taro.switchTab({ url: '/pages/todo/index' }) }] : [])
  ]

  return (
    <Screen>
      <GradientHeader title={`你好，${state.user?.nickname || state.user?.username || ''}`} eyebrow="LABORFLOW · 现场工作台" subtitle={`${roleText(state.user?.roles || [])}  ·  ${state.user?.employeeId ? '员工档案已绑定' : '员工档案未绑定'}`}>
        <View className={`mt-8 grid gap-3 ${shortcuts.length === 4 ? 'grid-cols-4' : 'grid-cols-3'}`}>
          {shortcuts.map((item) => <View key={item.label} className="flex flex-col items-center gap-2" onClick={item.action}><View className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/18 text-white shadow-sm">{item.icon}</View><Text className="text-xs font-medium text-white">{item.label}</Text></View>)}
        </View>
      </GradientHeader>
      <PageBody lifted>
        {loadError ? <View onClick={() => void load()}><Notice tone="danger">{loadError} 点击重试。</Notice></View> : null}
        <SectionTitle title="今日现场" subtitle="每天记录一点，项目进度更清楚" action={<Text className="text-sm text-[#39987d]" onClick={() => void Taro.switchTab({ url: '/pages/site/index' })}>全部记录  ›</Text>} />
        <View className="mb-6 flex items-baseline gap-2"><Text className="yellow-underline text-2xl font-bold leading-none tracking-tight">{monthDate}</Text><Text className="text-lg font-semibold">{weekday}</Text></View>
        <Card className="mint-soft flex items-center gap-4 p-4" onClick={() => state.todayReport ? void Taro.switchTab({ url: '/pages/site/index' }) : navigate('/pages/report/create')}>
          <RoundIcon className="bg-white text-[#39987d]"><CalendarOutlined size={26} /></RoundIcon>
          <View className="min-w-0 flex-1"><Text className="block text-base font-bold">{state.todayReport ? '今日日报已记录' : '今天还没有日报'}</Text><Text className="mt-1 block truncate text-sm text-[#7e8c85]">{state.todayReport ? `${textOf(state.todayReport, 'completedQuantity', '0')}${textOf(state.todayReport, 'unit')} · 点击查看现场记录` : '拍照并记录今日完成的工程量'}</Text></View>
          {state.todayReport ? <StatusTag text={status(state.todayReport.status)} tone={statusTone(state.todayReport.status)} /> : <Text className="text-xl text-[#4ca78e]">›</Text>}
        </Card>
        <View className="mb-7 grid grid-cols-3 gap-5 border-y border-[#edf0ed] py-5">
          <Metric value={state.draftCount} label="日报草稿" />
          <Metric value={state.rejectedCount} label="待修改" />
          <Metric value={state.variationCount} label="我的签证" />
        </View>
        <Card className="overflow-hidden bg-[#263c35] text-white">
          <View className="flex items-end justify-between gap-4"><View><Text className="block text-xs text-white/55">累计实发工资</Text><Text className="mt-1 block text-xl font-bold">{state.totalActual}</Text></View><View className="text-right"><CashOutlined size={26} color="#f3da74" /><Text className="mt-2 block text-xs text-white/65">{state.latestMonth} · {state.latestStatus}</Text></View></View>
        </Card>
        <SectionTitle title="进行中的项目" subtitle="最近活跃项目" />
        {state.projects.length ? <ScrollView scrollX enhanced showScrollbar={false} className="mb-7 w-full whitespace-nowrap"><View className="flex gap-3">{state.projects.map((item, index) => <View key={textOf(item, 'id')} className={`inline-block w-64 shrink-0 rounded-[22px] p-5 ${index % 2 ? 'bg-[#fff4d9]' : 'bg-[#e9f5ec]'}`}><View className="mb-8 flex items-start justify-between"><BarChartOutlined size={25} color={index % 2 ? '#a98530' : '#388d72'} /><Text className="text-xs text-[#8c958f]">进行中</Text></View><Text className="block truncate text-lg font-bold">{textOf(item, 'name', '未命名项目')}</Text><Text className="mt-2 block text-sm text-[#818983]">负责人 · {textOf(item, 'projectManager', '待设置')}</Text></View>)}</View></ScrollView> : <StateView text="暂无进行中的项目" />}
        {hasTodoRole() ? <><SectionTitle title="待办审批" subtitle={`${state.todoCount} 项等待处理`} action={<Text className="text-sm text-[#39987d]" onClick={() => void Taro.switchTab({ url: '/pages/todo/index' })}>查看全部  ›</Text>} />{state.todos.length ? state.todos.map((item) => <View key={textOf(item, 'id', textOf(item, 'businessId'))} className="mb-3 flex items-center gap-4 rounded-2xl bg-[#f7f8f4] p-4"><RoundIcon className="bg-white text-[#39987d]"><TodoListOutlined size={24} /></RoundIcon><View className="min-w-0 flex-1"><Text className="block truncate text-sm font-semibold">{textOf(item, 'title', '待办审批')}</Text><Text className="mt-1 block text-xs text-[#969d99]">{status(item.status)}</Text></View><Text className="text-lg text-[#929995]">›</Text></View>) : <Card className="bg-[#f7f8f4] text-sm text-[#929995]">暂无待办，今天也很顺利。</Card>}</> : null}
        {loading ? <Text className="block pb-3 text-center text-sm text-[#a0a6a2]">正在更新工作台…</Text> : null}
      </PageBody>
      <FloatingAction onClick={() => navigate('/pages/report/create?autoPhoto=1')}><Edit size={26} /></FloatingAction>
      <BottomTabBar active="home" />
    </Screen>
  )
}
