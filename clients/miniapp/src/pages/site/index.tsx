import { useCallback, useState } from 'react'
import Taro, { useDidShow } from 'virtual:taro/api'
import { Button, Text, View } from 'virtual:taro/components'
import { DescriptionOutlined, Edit, FilterOutlined, NotesOutlined } from '@taroify/icons'
import { BottomTabBar, Card, DateField, FloatingAction, GradientHeader, KeyValueRows, Notice, PageBody, RoundIcon, Screen, SectionTitle, SelectField, StateView, StatusTag, TextField } from '../../components/ui'
import { detailFiles, money, pageRecords, status, statusTone } from '../../lib/format'
import { textOf, type ApiRecord } from '../../lib/types'
import * as project from '../../services/project'

type Category = 'report' | 'variation'

interface SiteRow extends ApiRecord {
  titleText: string
  subText: string
  dateText: string
  pricingText: string
  amountText: string
  searchText: string
  statusText: string
  canEdit: boolean
  detailRows: Array<{ label: string; value: string }>
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'draft' },
  { label: '已提交', value: 'submitted' },
  { label: '已驳回', value: 'rejected' },
  { label: '已确认', value: 'confirmed' }
]

export default function SitePage() {
  const [category, setCategory] = useState<Category>('report')
  const [selectedStatus, setSelectedStatus] = useState('')
  const [keyword, setKeyword] = useState('')
  const [date, setDate] = useState('')
  const [rows, setRows] = useState<SiteRow[]>([])
  const [page, setPage] = useState(1)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [active, setActive] = useState<SiteRow | null>(null)

  const mapRow = useCallback((record: ApiRecord, detail: ApiRecord, currentCategory: Category): SiteRow => {
    if (currentCategory === 'report') {
      const files = detailFiles(detail).map((file) => textOf(file, 'originalName')).filter(Boolean).join('、') || '无'
      return {
        ...record,
        titleText: `${textOf(detail, 'projectName', '-')} / ${textOf(detail, 'workProcessName', '-')}`,
        subText: `${textOf(detail, 'sectionName', '-')} · ${textOf(record, 'completedQuantity', '0')}${textOf(record, 'unit')}`,
        dateText: textOf(record, 'workDate', '-'),
        pricingText: textOf(record, 'pricingStatus') === 'unpriced' ? '未定价草稿' : textOf(record, 'pricingBasis', '已定价'),
        amountText: money(record.outputValue),
        searchText: [textOf(detail, 'projectName'), textOf(detail, 'sectionName'), textOf(detail, 'workProcessName'), textOf(record, 'reporterName')].join(' '),
        statusText: status(record.status),
        canEdit: ['draft', 'rejected'].includes(textOf(record, 'status')),
        detailRows: [
          { label: '项目', value: textOf(detail, 'projectName', '-') },
          { label: '施工段', value: textOf(detail, 'sectionName', '-') },
          { label: '工序', value: textOf(detail, 'workProcessName', '-') },
          { label: '班组', value: textOf(detail, 'teamName', '-') },
          { label: '工作日期', value: textOf(record, 'workDate', '-') },
          { label: '工程量', value: `${textOf(record, 'completedQuantity', '0')}${textOf(record, 'unit')}` },
          { label: '单价', value: money(record.unitPriceSnapshot) },
          { label: '产值', value: money(record.outputValue) },
          { label: '定价依据', value: textOf(record, 'pricingBasis', '未定价') },
          { label: '附件', value: files }
        ]
      }
    }
    const files = detailFiles(detail).map((file) => textOf(file, 'originalName')).filter(Boolean).join('、') || '无'
    return {
      ...record,
      titleText: `${textOf(detail, 'projectName', '-')} / ${textOf(record, 'variationType', '签证')}`,
      subText: `${textOf(detail, 'sectionName', '-')} · ${textOf(record, 'reason', '未填写原因')}`,
      dateText: textOf(record, 'occurrenceDate', '-'),
      pricingText: record.confirmedAmount === null || record.confirmedAmount === undefined ? '待确认金额' : '确认金额',
      amountText: money(record.confirmedAmount === null || record.confirmedAmount === undefined ? record.estimatedAmount : record.confirmedAmount),
      searchText: [textOf(detail, 'projectName'), textOf(detail, 'sectionName'), textOf(record, 'variationType'), textOf(record, 'reason')].join(' '),
      statusText: status(record.status),
      canEdit: ['draft', 'rejected'].includes(textOf(record, 'status')),
      detailRows: [
        { label: '项目', value: textOf(detail, 'projectName', '-') },
        { label: '施工段', value: textOf(detail, 'sectionName', '-') },
        { label: '签证类型', value: textOf(record, 'variationType', '-') },
        { label: '发生日期', value: textOf(record, 'occurrenceDate', '-') },
        { label: '原因', value: textOf(record, 'reason', '-') },
        { label: '预估金额', value: money(record.estimatedAmount) },
        { label: '确认金额', value: record.confirmedAmount === null || record.confirmedAmount === undefined ? '尚未确认' : money(record.confirmedAmount) },
        { label: '附件', value: files }
      ]
    }
  }, [])

  const load = useCallback(async (reset: boolean, requestedCategory = category) => {
    const nextPage = reset ? 1 : page + 1
    if (reset) setLoading(true)
    else setLoadingMore(true)
    setError('')
    try {
      let cursor = nextPage
      let records: ApiRecord[] = []
      let incoming: SiteRow[] = []
      do {
        const params = { page: cursor, pageSize: 20, ...(selectedStatus ? { status: selectedStatus } : {}) }
        const result = requestedCategory === 'report' ? await project.myDailyReports(params) : await project.myVariationOrders(params)
        records = pageRecords(result)
        const details = await Promise.all(records.map((record) => requestedCategory === 'report' ? project.dailyReportDetail(textOf(record, 'id')) : project.variationOrderDetail(textOf(record, 'id'))))
        incoming = records.map((record, index) => mapRow(record, details[index] || {}, requestedCategory))
          .filter((row) => !date || row.dateText === date)
          .filter((row) => !keyword.trim() || row.searchText.includes(keyword.trim()))
        if (!incoming.length && records.length === 20) cursor += 1
      } while (!incoming.length && records.length === 20)
      setRows((current) => reset ? incoming : [...current, ...incoming])
      setPage(cursor)
      setHasMore(records.length === 20)
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : '现场记录加载失败'
      setError(message)
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      setLoading(false)
      setLoadingMore(false)
    }
  }, [category, date, keyword, mapRow, page, selectedStatus])

  useDidShow(() => {
    void load(true)
  })

  const switchCategory = (next: Category) => {
    setCategory(next)
    void load(true, next)
  }
  const resetFilters = () => {
    setSelectedStatus('')
    setKeyword('')
    setDate('')
  }
  const applyFilters = () => void load(true)
  const create = () => void Taro.navigateTo({ url: category === 'report' ? '/pages/report/create' : '/pages/variation/create' })
  const edit = (record: SiteRow) => void Taro.navigateTo({ url: `${category === 'report' ? '/pages/report/create' : '/pages/variation/create'}?editId=${textOf(record, 'id')}` })

  return (
    <Screen>
      <GradientHeader title="现场记录" eyebrow="FIELD LOG" subtitle="日报与签证，按时间清晰归档">
        <View className="mt-7 flex gap-8"><View className={`pb-2 ${category === 'report' ? 'border-b-4 border-[#f4dc76]' : ''}`} onClick={() => switchCategory('report')}><Text className="text-base font-semibold text-white">工程日报</Text></View><View className={`pb-2 ${category === 'variation' ? 'border-b-4 border-[#f4dc76]' : ''}`} onClick={() => switchCategory('variation')}><Text className="text-base font-semibold text-white">现场签证</Text></View></View>
      </GradientHeader>
      <PageBody lifted>
        <SectionTitle title={category === 'report' ? '我的日报' : '我的签证'} subtitle={`${rows.length} 条已加载`} action={<FilterOutlined size={22} color="#4a8e79" />} />
        <Card className="bg-[#f7f8f4] shadow-none"><View className="grid grid-cols-2 gap-x-3"><SelectField label="状态" value={selectedStatus} range={statusOptions} onChange={setSelectedStatus} /><DateField label="日期" value={date} onChange={setDate} /></View><TextField label="关键词" value={keyword} placeholder="项目、施工段、工序或原因" onChange={setKeyword} /><View className="flex gap-2"><Button className="m-0 flex-1 rounded-full border-0 bg-white text-sm text-[#69726d]" onClick={resetFilters}>重置</Button><Button className="m-0 flex-1 rounded-full border-0 bg-[#3b9f83] text-sm text-white" onClick={applyFilters}>应用筛选</Button></View></Card>
        {error ? <Notice tone="danger">{error}</Notice> : null}
        {rows.map((record) => {
          const dateParts = record.dateText.split('-')
          return <View key={`${category}-${textOf(record, 'id')}`} className="flex gap-4 border-b border-[#edf0ed] py-6" onClick={() => setActive(record)}><View className="w-16 shrink-0"><Text className="yellow-underline inline text-[34px] font-bold leading-none">{dateParts[2] || '--'}</Text><Text className="mt-2 block text-xs text-[#969d99]">{dateParts.length === 3 ? `${dateParts[1]}月` : record.dateText}</Text></View><View className="min-w-0 flex-1"><View className="mb-3 flex items-start justify-between gap-3"><RoundIcon className={`h-10 w-10 rounded-xl ${category === 'report' ? 'bg-[#eaf7f0] text-[#39987d]' : 'bg-[#fff4d9] text-[#a7842f]'}`}>{category === 'report' ? <NotesOutlined size={21} /> : <DescriptionOutlined size={21} />}</RoundIcon><StatusTag text={record.statusText} tone={statusTone(record.status)} /></View><Text className="block text-lg font-bold leading-6">{record.titleText}</Text><Text className="mt-2 block text-sm leading-5 text-[#929995]">{record.subText}</Text><View className="mt-4 flex items-end justify-between"><View><Text className="block text-xs text-[#a0a6a2]">{record.pricingText}</Text><Text className="mt-1 block text-base font-bold text-[#398d73]">{record.amountText}</Text></View>{record.canEdit ? <Button className="m-0 rounded-full border-0 bg-[#eef6f2] px-3 text-xs text-[#397f69]" onClick={(event) => { event.stopPropagation(); edit(record) }}>编辑</Button> : <Text className="text-xl text-[#a4aaa6]">›</Text>}</View></View></View>
        })}
        {!rows.length && !loading ? <StateView text="暂无符合条件的现场记录" actionText="新建记录" onAction={create} /> : null}
        {loading ? <StateView text="加载中…" /> : null}
        {hasMore && rows.length ? <Button className="mb-4 rounded-full border-0 bg-[#eef5f1] text-sm text-[#4a806f]" loading={loadingMore} onClick={() => void load(false)}>加载更多</Button> : null}
      </PageBody>
      <FloatingAction onClick={create}><Edit size={26} /></FloatingAction>
      <BottomTabBar active="site" />
      {active ? <View className="fixed inset-0 z-20 flex items-end bg-black/35" onClick={() => setActive(null)}><View className="max-h-[84vh] w-full overflow-y-auto rounded-t-[32px] bg-white px-5 pb-8 pt-5" onClick={(event) => event.stopPropagation()}><View className="mx-auto mb-5 h-1 w-10 rounded-full bg-[#dce1de]" /><View className="mb-5 flex items-center justify-between"><Text className="text-xl font-bold">记录详情</Text><Text className="text-sm text-[#3a997d]" onClick={() => setActive(null)}>关闭</Text></View><KeyValueRows rows={active.detailRows} />{active.canEdit ? <Button className="mt-5 rounded-full border-0 bg-[#3a9d81] text-white" onClick={() => edit(active)}>编辑记录</Button> : null}</View></View> : null}
    </Screen>
  )
}
