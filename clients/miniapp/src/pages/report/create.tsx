import { useCallback, useEffect, useRef, useState } from 'react'
import Taro, { useRouter } from 'virtual:taro/api'
import { Text, View } from 'virtual:taro/components'
import { Card, DateField, Notice, PageBody, PageTitle, PrimaryButton, Screen, SecondaryButton, SelectField, StateView, TextAreaField, TextField } from '../../components/ui'
import { PhotoUploader } from '../../components/photo-uploader'
import { getUser } from '../../lib/auth'
import { clearDraft, loadDraft, saveDraft } from '../../lib/draft'
import { maxPhotoCount, money, today } from '../../lib/format'
import { textOf, type ApiRecord, type UploadedFile } from '../../lib/types'
import { detailFiles } from '../../lib/format'
import { chooseAndUpload, UploadError } from '../../services/file'
import * as project from '../../services/project'

interface ReportForm {
  workDate: string
  projectId: string
  projectSectionId: string
  contractItemId: string
  workProcessId: string
  completedQuantity: string
  unit: string
  teamId: string
  workerCount: string
  machineryUsage: string
  exceptionNote: string
  reporterName: string
}

interface ReportDraft {
  editId: string
  form: ReportForm
  uploadedFiles: UploadedFile[]
  attachedFileIds: string[]
}

const emptyForm = (): ReportForm => ({
  workDate: today(), projectId: '', projectSectionId: '', contractItemId: '', workProcessId: '', completedQuantity: '', unit: '', teamId: '', workerCount: '', machineryUsage: '', exceptionNote: '', reporterName: ''
})

function reportFormFrom(record: ApiRecord): ReportForm {
  return {
    workDate: textOf(record, 'workDate', today()),
    projectId: textOf(record, 'projectId'),
    projectSectionId: textOf(record, 'projectSectionId'),
    contractItemId: textOf(record, 'contractItemId'),
    workProcessId: textOf(record, 'workProcessId'),
    completedQuantity: textOf(record, 'completedQuantity'),
    unit: textOf(record, 'unit'),
    teamId: textOf(record, 'teamId'),
    workerCount: textOf(record, 'workerCount'),
    machineryUsage: textOf(record, 'machineryUsage'),
    exceptionNote: textOf(record, 'exceptionNote'),
    reporterName: textOf(record, 'reporterName')
  }
}

export default function ReportCreatePage() {
  const router = useRouter()
  const routeEditId = String(router.params.editId || '')
  const [user] = useState(() => getUser())
  const scope = user?.userId || 'anonymous'
  const [initialDraft] = useState<ReportDraft | null>(() => routeEditId ? null : loadDraft<ReportDraft>('daily-report', scope))
  const [form, setForm] = useState<ReportForm>(() => initialDraft?.form ? { ...emptyForm(), ...initialDraft.form } : emptyForm())
  const [editId, setEditId] = useState(routeEditId || initialDraft?.editId || '')
  const [projects, setProjects] = useState<ApiRecord[]>([])
  const [sections, setSections] = useState<ApiRecord[]>([])
  const [processes, setProcesses] = useState<ApiRecord[]>([])
  const [contractItems, setContractItems] = useState<ApiRecord[]>([])
  const [teams, setTeams] = useState<ApiRecord[]>([])
  const [files, setFiles] = useState<UploadedFile[]>(initialDraft?.uploadedFiles || [])
  const [attachedIds, setAttachedIds] = useState<string[]>(initialDraft?.attachedFileIds || [])
  const [lookupLoading, setLookupLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [uploadText, setUploadText] = useState(`可拍摄或从相册选择，最多 ${maxPhotoCount} 张`)
  const [saving, setSaving] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [pricing, setPricing] = useState<ApiRecord | null>(null)
  const [pricingLoading, setPricingLoading] = useState(false)
  const [pricingText, setPricingText] = useState('填写业务数据后预览价格')
  const latest = useRef({ form, files, attachedIds, editId })
  const autoPhotoStarted = useRef(false)
  latest.current = { form, files, attachedIds, editId }

  const persistLocal = useCallback((snapshot = latest.current) => {
    const hasContent = Boolean(snapshot.editId || snapshot.files.length || Object.entries(snapshot.form).some(([key, value]) => key !== 'workDate' && Boolean(value)))
    if (hasContent) saveDraft('daily-report', scope, { editId: snapshot.editId, form: snapshot.form, uploadedFiles: snapshot.files, attachedFileIds: snapshot.attachedIds })
  }, [scope])

  useEffect(() => () => persistLocal(), [persistLocal])

  const loadSections = useCallback(async (projectId: string) => {
    if (!projectId) {
      setSections([])
      return
    }
    try {
      setSections(await project.sections(projectId))
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '施工段加载失败', icon: 'none' })
    }
  }, [])

  const loadContractItems = useCallback(async (projectId: string) => {
    if (!projectId) {
      setContractItems([])
      return
    }
    try {
      setContractItems(await project.contractItems({ projectId }))
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '合同清单加载失败', icon: 'none' })
    }
  }, [])

  const refreshPricing = useCallback(async (currentForm = latest.current.form): Promise<ApiRecord | null> => {
    if (!currentForm.workDate || !currentForm.projectId || !currentForm.projectSectionId || !currentForm.workProcessId || !currentForm.unit || !currentForm.completedQuantity) {
      setPricing(null)
      setPricingText('填写日期、项目、施工段、工序、数量和单位后预览价格')
      return null
    }
    setPricingLoading(true)
    try {
      const nextPricing = await project.previewDailyReportPricing({
        workDate: currentForm.workDate,
        projectId: currentForm.projectId,
        projectSectionId: currentForm.projectSectionId,
        contractItemId: currentForm.contractItemId || null,
        workProcessId: currentForm.workProcessId,
        completedQuantity: Number(currentForm.completedQuantity),
        unit: currentForm.unit.trim()
      })
      setPricing(nextPricing)
      setPricingText(nextPricing.selectionRequired ? '匹配到多个合同清单项，请明确选择' : nextPricing.priced ? `${textOf(nextPricing, 'basis', '已定价')}；单价 ${money(nextPricing.unitPrice)}；预计产值 ${money(nextPricing.outputValue)}` : '当前日期没有有效价格：可保存草稿，但不能提交')
      return nextPricing
    } catch (error) {
      setPricing(null)
      setPricingText(error instanceof Error ? error.message : '定价预览失败')
      return null
    } finally {
      setPricingLoading(false)
    }
  }, [])

  const loadEdit = useCallback(async (id: string) => {
    const detail = await project.dailyReportDetail(id)
    const report = detail.report && typeof detail.report === 'object' ? detail.report as ApiRecord : detail
    const nextForm = reportFormFrom(report)
    const nextFiles = detailFiles(detail).filter((file): file is UploadedFile => Boolean(file.id)) as UploadedFile[]
    setForm(nextForm)
    setFiles(nextFiles)
    setAttachedIds(nextFiles.map((file) => String(file.id)))
    setUploadText(nextFiles.length ? `已加载 ${nextFiles.length} 个已有附件` : '尚未添加现场照片')
    if (nextForm.projectId) {
      void loadSections(nextForm.projectId)
      void loadContractItems(nextForm.projectId)
    }
    void refreshPricing(nextForm)
  }, [loadContractItems, loadSections, refreshPricing])

  useEffect(() => {
    if (!user) {
      void Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    const load = async () => {
      try {
        const [nextProjects, nextProcesses, nextTeams] = await Promise.all([project.projects(), project.workProcesses(), project.teams()])
        setProjects(nextProjects)
        setProcesses(nextProcesses)
        setTeams(nextTeams)
        if (routeEditId) await loadEdit(routeEditId)
        else if (initialDraft?.form.projectId) {
          void loadSections(initialDraft.form.projectId)
          void loadContractItems(initialDraft.form.projectId)
          void refreshPricing(initialDraft.form)
        }
      } catch (error) {
        Taro.showToast({ title: error instanceof Error ? error.message : '基础数据加载失败', icon: 'none' })
      } finally {
        setLookupLoading(false)
      }
    }
    void load()
  }, [initialDraft?.form.projectId, loadContractItems, loadEdit, loadSections, refreshPricing, routeEditId, user])

  useEffect(() => {
    if (router.params.autoPhoto === '1' && !autoPhotoStarted.current) {
      autoPhotoStarted.current = true
      const timer = setTimeout(() => void uploadPhotos(), 450)
      return () => clearTimeout(timer)
    }
    return undefined
  }, [router.params.autoPhoto])

  const updateForm = (patch: Partial<ReportForm>, shouldPrice = false) => {
    const next = { ...latest.current.form, ...patch }
    setForm(next)
    persistLocal({ ...latest.current, form: next })
    if (shouldPrice) void refreshPricing(next)
  }

  const selectProject = (projectId: string) => {
    updateForm({ projectId, projectSectionId: '', contractItemId: '' }, true)
    setSections([])
    setContractItems([])
    void loadSections(projectId)
    void loadContractItems(projectId)
  }
  const selectContractItem = (contractItemId: string) => {
    const item = contractItems.find((row) => textOf(row, 'id') === contractItemId)
    updateForm({
      contractItemId,
      projectSectionId: item ? textOf(item, 'projectSectionId', form.projectSectionId) : form.projectSectionId,
      workProcessId: item ? textOf(item, 'workProcessId', form.workProcessId) : form.workProcessId,
      unit: item ? textOf(item, 'unit', form.unit) : form.unit
    }, true)
  }
  const selectProcess = (workProcessId: string) => {
    const item = processes.find((row) => textOf(row, 'id') === workProcessId)
    updateForm({ workProcessId, unit: item ? textOf(item, 'unit', form.unit) : form.unit }, true)
  }

  const uploadPhotos = async () => {
    if (uploading || saving || submitting) return
    const remaining = maxPhotoCount - latest.current.files.length
    if (remaining <= 0) {
      Taro.showToast({ title: `最多可添加 ${maxPhotoCount} 张现场照片`, icon: 'none' })
      return
    }
    setUploading(true)
    setUploadText('正在选择现场照片…')
    try {
      const nextFiles = [...latest.current.files, ...(await chooseAndUpload({ maxCount: remaining, onProgress: ({ current, total, progress }) => setUploadText(`正在上传第 ${current}/${total} 张（${progress}%）`) }))]
      setFiles(nextFiles)
      setUploadText(nextFiles.length ? `已添加 ${nextFiles.length} 张照片` : '未添加照片')
      persistLocal({ ...latest.current, files: nextFiles })
    } catch (error) {
      const partial = error instanceof UploadError ? error.uploadedFiles : []
      const nextFiles = [...latest.current.files, ...partial]
      setFiles(nextFiles)
      setUploadText(nextFiles.length ? '部分照片已上传，可继续添加' : '上传未完成，可稍后重试')
      persistLocal({ ...latest.current, files: nextFiles })
      Taro.showToast({ title: error instanceof Error ? error.message : '上传未完成，可稍后重试', icon: 'none' })
    } finally {
      setUploading(false)
    }
  }

  const removePhoto = async (index: number) => {
    if (uploading || saving || submitting) return
    const file = latest.current.files[index]
    if (!file) return
    if (latest.current.editId && latest.current.attachedIds.includes(String(file.id))) {
      try {
        await project.removeDailyReportFile(latest.current.editId, file.id)
      } catch (error) {
        Taro.showToast({ title: error instanceof Error ? error.message : '附件移除失败', icon: 'none' })
        return
      }
    }
    const nextFiles = latest.current.files.filter((_, itemIndex) => itemIndex !== index)
    const nextAttachedIds = latest.current.attachedIds.filter((id) => id !== String(file.id))
    setFiles(nextFiles)
    setAttachedIds(nextAttachedIds)
    setUploadText(nextFiles.length ? `已保留 ${nextFiles.length} 张照片` : '尚未添加现场照片')
    persistLocal({ ...latest.current, files: nextFiles, attachedIds: nextAttachedIds })
  }

  const validate = (): string => {
    const current = latest.current.form
    if (!current.projectId || !current.projectSectionId || !current.workProcessId) return '请选择项目、施工段和工序'
    if (!(Number(current.completedQuantity) > 0)) return '完成工程量需大于 0'
    if (!current.unit.trim()) return '请输入工程量单位'
    if (!(Number(current.workerCount) > 0)) return '施工人数需大于 0'
    return ''
  }
  const payload = (): ApiRecord => {
    const current = latest.current.form
    return { workDate: current.workDate, projectId: current.projectId, projectSectionId: current.projectSectionId, contractItemId: current.contractItemId || null, workProcessId: current.workProcessId, completedQuantity: Number(current.completedQuantity), unit: current.unit.trim(), teamId: current.teamId || null, workerCount: Number(current.workerCount), machineryUsage: current.machineryUsage, exceptionNote: current.exceptionNote, reporterName: current.reporterName }
  }
  const saveRemote = async (): Promise<string> => {
    const currentId = latest.current.editId
    const saved = currentId ? await project.updateDailyReport(currentId, payload()) : await project.createDailyReport(payload())
    const id = textOf(saved, 'id', currentId)
    if (!id) throw new Error('日报保存结果缺少编号')
    if (!currentId) {
      setEditId(id)
      persistLocal({ ...latest.current, editId: id })
    }
    return id
  }
  const attachPending = async (reportId: string) => {
    let nextAttachedIds = latest.current.attachedIds
    for (const file of latest.current.files) {
      if (nextAttachedIds.includes(String(file.id))) continue
      await project.attachDailyReportFile(reportId, file.id)
      nextAttachedIds = [...nextAttachedIds, String(file.id)]
      setAttachedIds(nextAttachedIds)
      persistLocal({ ...latest.current, attachedIds: nextAttachedIds, editId: reportId })
    }
  }
  const persist = async (action: 'draft' | 'submit') => {
    if (saving || submitting || uploading) return
    const message = validate()
    if (message) {
      Taro.showToast({ title: message, icon: 'none' })
      return
    }
    action === 'submit' ? setSubmitting(true) : setSaving(true)
    try {
      if (action === 'submit') {
        const latestPricing = await refreshPricing()
        if (!latestPricing || !latestPricing.priced || latestPricing.selectionRequired) throw new Error('日报尚未完成有效定价，只能保存草稿')
      }
      const id = await saveRemote()
      await attachPending(id)
      if (action === 'submit') await project.submitDailyReport(id)
      clearDraft('daily-report', scope)
      Taro.showToast({ title: action === 'submit' ? '日报已提交' : '日报草稿已保存', icon: 'success' })
      await Taro.navigateBack()
    } catch (error) {
      persistLocal()
      Taro.showToast({ title: error instanceof Error ? error.message : '保存失败，已保留本机草稿', icon: 'none' })
    } finally {
      setSaving(false)
      setSubmitting(false)
    }
  }
  const discardLocal = () => {
    clearDraft('daily-report', scope)
    void Taro.reLaunch({ url: '/pages/report/create' })
  }

  if (!user) return <Screen><PageBody><StateView text="正在校验登录状态" /></PageBody></Screen>
  const projectOptions = projects.map((item) => ({ label: textOf(item, 'name', '未命名项目'), value: textOf(item, 'id') }))
  const sectionOptions = sections.map((item) => ({ label: textOf(item, 'name', '未命名施工段'), value: textOf(item, 'id') }))
  const processOptions = processes.map((item) => ({ label: `${textOf(item, 'name', '未命名工序')}${textOf(item, 'unit') ? ` / ${textOf(item, 'unit')}` : ''}`, value: textOf(item, 'id') }))
  const contractOptions = [{ label: '自动匹配', value: '' }, ...contractItems.map((item) => ({ label: `${textOf(item, 'itemCode') ? `${textOf(item, 'itemCode')} / ` : ''}${textOf(item, 'itemName', '未命名清单')}`, value: textOf(item, 'id') }))]
  const teamOptions = teams.map((item) => ({ label: textOf(item, 'name', '未命名班组'), value: textOf(item, 'id') }))
  const draftRestored = Boolean(initialDraft && !routeEditId)
  return (
    <Screen>
      <PageBody>
        <PageTitle title={editId ? '编辑日报' : '现场填报'} subtitle="记录当天工程量，完成定价后提交确认" />
        {draftRestored ? <Notice tone="warning">已恢复本机草稿。</Notice> : null}
        {lookupLoading ? <StateView text="正在加载项目与工序…" /> : <>
          <Card><DateField label="工作日期" value={form.workDate} required onChange={(value) => updateForm({ workDate: value }, true)} /><SelectField label="项目" value={form.projectId} range={projectOptions} required onChange={selectProject} /><SelectField label="施工段" value={form.projectSectionId} range={sectionOptions} required disabled={!form.projectId} onChange={(value) => updateForm({ projectSectionId: value }, true)} /><SelectField label="合同清单" value={form.contractItemId} range={contractOptions} onChange={selectContractItem} /><SelectField label="工序" value={form.workProcessId} range={processOptions} required onChange={selectProcess} /><TextField label="完成工程量" value={form.completedQuantity} required type="digit" placeholder="请输入大于 0 的数量" onChange={(value) => updateForm({ completedQuantity: value }, true)} /><TextField label="工程量单位" value={form.unit} required placeholder="如：m³、㎡、吨" onChange={(value) => updateForm({ unit: value }, true)} /><SelectField label="施工班组" value={form.teamId} range={teamOptions} onChange={(value) => updateForm({ teamId: value })} /><TextField label="施工人数" value={form.workerCount} required type="number" placeholder="请输入人数" onChange={(value) => updateForm({ workerCount: value })} /><TextField label="机械使用" value={form.machineryUsage} placeholder="选填" onChange={(value) => updateForm({ machineryUsage: value })} /><TextField label="填报人" value={form.reporterName} placeholder="选填" onChange={(value) => updateForm({ reporterName: value })} /><TextAreaField label="异常说明" value={form.exceptionNote} placeholder="选填" onChange={(value) => updateForm({ exceptionNote: value })} /></Card>
          <Card className="mint-soft shadow-none"><Text className="mb-2 block text-lg font-bold">价格预览</Text><Text className="block text-sm leading-6 text-[#607069]">{pricingLoading ? '正在预览…' : pricingText}</Text>{pricing ? <Text className="mt-2 block text-xs text-[#8a9690]">{pricing.priced && !pricing.selectionRequired ? '提交时将再次校验定价。' : '草稿可以保存，未完成有效定价时不能提交。'}</Text> : null}</Card>
          <Card><PhotoUploader files={files} uploading={uploading} text={uploadText} onUpload={() => void uploadPhotos()} onRemove={(index) => void removePhoto(index)} /></Card>
          <View className="mb-4 grid grid-cols-3 gap-2"><SecondaryButton disabled={saving || submitting || uploading} onClick={() => { persistLocal(); Taro.showToast({ title: '已暂存到本机', icon: 'success' }) }}>本机暂存</SecondaryButton><SecondaryButton disabled={saving || submitting || uploading} onClick={discardLocal}>丢弃草稿</SecondaryButton><PrimaryButton loading={saving} disabled={submitting || uploading} onClick={() => void persist('draft')}>保存草稿</PrimaryButton></View>
          <PrimaryButton loading={submitting} disabled={saving || uploading} onClick={() => void persist('submit')}>提交日报</PrimaryButton>
        </>}
      </PageBody>
    </Screen>
  )
}
