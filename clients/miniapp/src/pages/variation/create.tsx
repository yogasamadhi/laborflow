import { useCallback, useEffect, useRef, useState } from 'react'
import Taro, { useRouter } from 'virtual:taro/api'
import { View } from 'virtual:taro/components'
import { Card, DateField, Notice, PageBody, PageTitle, PrimaryButton, Screen, SecondaryButton, SelectField, StateView, TextAreaField, TextField } from '../../components/ui'
import { PhotoUploader } from '../../components/photo-uploader'
import { getUser } from '../../lib/auth'
import { clearDraft, loadDraft, saveDraft } from '../../lib/draft'
import { maxPhotoCount, today } from '../../lib/format'
import { textOf, type ApiRecord, type UploadedFile } from '../../lib/types'
import { detailFiles } from '../../lib/format'
import { chooseAndUpload, UploadError } from '../../services/file'
import * as project from '../../services/project'

interface VariationForm {
  projectId: string
  projectSectionId: string
  occurrenceDate: string
  variationType: string
  reason: string
  responsibleParty: string
  increasedQuantity: string
  estimatedAmount: string
  laborCount: string
  machineryShift: string
  materialNote: string
}

interface VariationDraft {
  editId: string
  form: VariationForm
  uploadedFiles: UploadedFile[]
  attachedFileIds: string[]
}

const variationTypes = ['设计变更', '现场签证', '工程量增减', '工期影响', '其他']

const emptyForm = (): VariationForm => ({
  projectId: '', projectSectionId: '', occurrenceDate: today(), variationType: '', reason: '', responsibleParty: '', increasedQuantity: '', estimatedAmount: '', laborCount: '', machineryShift: '', materialNote: ''
})

function formFrom(record: ApiRecord): VariationForm {
  return {
    projectId: textOf(record, 'projectId'),
    projectSectionId: textOf(record, 'projectSectionId'),
    occurrenceDate: textOf(record, 'occurrenceDate', today()),
    variationType: textOf(record, 'variationType'),
    reason: textOf(record, 'reason'),
    responsibleParty: textOf(record, 'responsibleParty'),
    increasedQuantity: textOf(record, 'increasedQuantity'),
    estimatedAmount: textOf(record, 'estimatedAmount'),
    laborCount: textOf(record, 'laborCount'),
    machineryShift: textOf(record, 'machineryShift'),
    materialNote: textOf(record, 'materialNote')
  }
}

export default function VariationCreatePage() {
  const router = useRouter()
  const routeEditId = String(router.params.editId || '')
  const [user] = useState(() => getUser())
  const scope = user?.userId || 'anonymous'
  const [initialDraft] = useState<VariationDraft | null>(() => routeEditId ? null : loadDraft<VariationDraft>('variation-order', scope))
  const [form, setForm] = useState<VariationForm>(() => initialDraft?.form ? { ...emptyForm(), ...initialDraft.form } : emptyForm())
  const [editId, setEditId] = useState(routeEditId || initialDraft?.editId || '')
  const [projects, setProjects] = useState<ApiRecord[]>([])
  const [sections, setSections] = useState<ApiRecord[]>([])
  const [files, setFiles] = useState<UploadedFile[]>(initialDraft?.uploadedFiles || [])
  const [attachedIds, setAttachedIds] = useState<string[]>(initialDraft?.attachedFileIds || [])
  const [lookupLoading, setLookupLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [uploadText, setUploadText] = useState(`可拍摄或从相册选择，最多 ${maxPhotoCount} 张`)
  const [saving, setSaving] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const latest = useRef({ form, files, attachedIds, editId })
  latest.current = { form, files, attachedIds, editId }

  const persistLocal = useCallback((snapshot = latest.current) => {
    const hasContent = Boolean(snapshot.editId || snapshot.files.length || Object.entries(snapshot.form).some(([key, value]) => key !== 'occurrenceDate' && Boolean(value)))
    if (hasContent) saveDraft('variation-order', scope, { editId: snapshot.editId, form: snapshot.form, uploadedFiles: snapshot.files, attachedFileIds: snapshot.attachedIds })
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

  const loadEdit = useCallback(async (id: string) => {
    const detail = await project.variationOrderDetail(id)
    const order = detail.order && typeof detail.order === 'object' ? detail.order as ApiRecord : detail
    const nextForm = formFrom(order)
    const nextFiles = detailFiles(detail).filter((file): file is UploadedFile => Boolean(file.id)) as UploadedFile[]
    setForm(nextForm)
    setFiles(nextFiles)
    setAttachedIds(nextFiles.map((file) => String(file.id)))
    setUploadText(nextFiles.length ? `已加载 ${nextFiles.length} 个已有附件` : '尚未添加现场照片')
    if (nextForm.projectId) void loadSections(nextForm.projectId)
  }, [loadSections])

  useEffect(() => {
    if (!user) {
      void Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    const load = async () => {
      try {
        setProjects(await project.projects())
        if (routeEditId) await loadEdit(routeEditId)
        else if (initialDraft?.form.projectId) void loadSections(initialDraft.form.projectId)
      } catch (error) {
        Taro.showToast({ title: error instanceof Error ? error.message : '项目数据加载失败', icon: 'none' })
      } finally {
        setLookupLoading(false)
      }
    }
    void load()
  }, [initialDraft?.form.projectId, loadEdit, loadSections, routeEditId, user])

  const updateForm = (patch: Partial<VariationForm>) => {
    const next = { ...latest.current.form, ...patch }
    setForm(next)
    persistLocal({ ...latest.current, form: next })
  }
  const selectProject = (projectId: string) => {
    updateForm({ projectId, projectSectionId: '' })
    setSections([])
    void loadSections(projectId)
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
      const uploaded = await chooseAndUpload({ maxCount: remaining, onProgress: ({ current, total, progress }) => setUploadText(`正在上传第 ${current}/${total} 张（${progress}%）`) })
      const nextFiles = [...latest.current.files, ...uploaded]
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
        await project.removeVariationFile(latest.current.editId, file.id)
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
    if (!current.projectId || !current.projectSectionId) return '请选择项目和施工段'
    if (!current.variationType) return '请选择签证类型'
    if (!current.reason.trim()) return '请输入发生原因'
    if (!(Number(current.increasedQuantity) > 0)) return '工程量需大于 0'
    return ''
  }
  const payload = (): ApiRecord => {
    const current = latest.current.form
    return { projectId: current.projectId, projectSectionId: current.projectSectionId, occurrenceDate: current.occurrenceDate, variationType: current.variationType, reason: current.reason.trim(), responsibleParty: current.responsibleParty, increasedQuantity: Number(current.increasedQuantity), estimatedAmount: Number(current.estimatedAmount || 0), laborCount: Number(current.laborCount || 0), machineryShift: current.machineryShift, materialNote: current.materialNote }
  }
  const saveRemote = async (): Promise<string> => {
    const currentId = latest.current.editId
    const saved = currentId ? await project.updateVariationOrder(currentId, payload()) : await project.createVariationOrder(payload())
    const id = textOf(saved, 'id', currentId)
    if (!id) throw new Error('签证保存结果缺少编号')
    if (!currentId) {
      setEditId(id)
      persistLocal({ ...latest.current, editId: id })
    }
    return id
  }
  const attachPending = async (orderId: string) => {
    let nextAttachedIds = latest.current.attachedIds
    for (const file of latest.current.files) {
      if (nextAttachedIds.includes(String(file.id))) continue
      await project.attachVariationFile(orderId, file.id)
      nextAttachedIds = [...nextAttachedIds, String(file.id)]
      setAttachedIds(nextAttachedIds)
      persistLocal({ ...latest.current, attachedIds: nextAttachedIds, editId: orderId })
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
      const id = await saveRemote()
      await attachPending(id)
      if (action === 'submit') await project.submitVariationOrder(id)
      clearDraft('variation-order', scope)
      Taro.showToast({ title: action === 'submit' ? '签证已提交' : '签证草稿已保存', icon: 'success' })
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
    clearDraft('variation-order', scope)
    void Taro.reLaunch({ url: '/pages/variation/create' })
  }

  if (!user) return <Screen><PageBody><StateView text="正在校验登录状态" /></PageBody></Screen>
  const projectOptions = projects.map((item) => ({ label: textOf(item, 'name', '未命名项目'), value: textOf(item, 'id') }))
  const sectionOptions = sections.map((item) => ({ label: textOf(item, 'name', '未命名施工段'), value: textOf(item, 'id') }))
  const typeOptions = variationTypes.map((item) => ({ label: item, value: item }))
  const draftRestored = Boolean(initialDraft && !routeEditId)
  return (
    <Screen>
      <PageBody>
        <PageTitle title={editId ? '编辑签证' : '现场上报'} subtitle="记录现场变化、影响范围与预估金额" />
        {draftRestored ? <Notice tone="warning">已恢复本机草稿。</Notice> : null}
        {lookupLoading ? <StateView text="正在加载项目…" /> : <>
          <Card><SelectField label="项目" value={form.projectId} range={projectOptions} required onChange={selectProject} /><SelectField label="施工段" value={form.projectSectionId} range={sectionOptions} required disabled={!form.projectId} onChange={(value) => updateForm({ projectSectionId: value })} /><DateField label="发生日期" value={form.occurrenceDate} required onChange={(value) => updateForm({ occurrenceDate: value })} /><SelectField label="签证类型" value={form.variationType} range={typeOptions} required onChange={(value) => updateForm({ variationType: value })} /><TextAreaField label="发生原因" value={form.reason} required placeholder="请说明现场情况与原因" onChange={(value) => updateForm({ reason: value })} /><TextField label="责任方" value={form.responsibleParty} placeholder="选填" onChange={(value) => updateForm({ responsibleParty: value })} /><TextField label="增加工程量" value={form.increasedQuantity} required type="digit" placeholder="请输入大于 0 的数量" onChange={(value) => updateForm({ increasedQuantity: value })} /><TextField label="预估金额" value={form.estimatedAmount} type="digit" placeholder="选填" onChange={(value) => updateForm({ estimatedAmount: value })} /><TextField label="人工数量" value={form.laborCount} type="number" placeholder="选填" onChange={(value) => updateForm({ laborCount: value })} /><TextField label="机械台班" value={form.machineryShift} placeholder="选填" onChange={(value) => updateForm({ machineryShift: value })} /><TextAreaField label="材料说明" value={form.materialNote} placeholder="选填" onChange={(value) => updateForm({ materialNote: value })} /></Card>
          <Card><PhotoUploader files={files} uploading={uploading} text={uploadText} onUpload={() => void uploadPhotos()} onRemove={(index) => void removePhoto(index)} /></Card>
          <View className="mb-4 grid grid-cols-3 gap-2"><SecondaryButton disabled={saving || submitting || uploading} onClick={() => { persistLocal(); Taro.showToast({ title: '已暂存到本机', icon: 'success' }) }}>本机暂存</SecondaryButton><SecondaryButton disabled={saving || submitting || uploading} onClick={discardLocal}>丢弃草稿</SecondaryButton><PrimaryButton loading={saving} disabled={submitting || uploading} onClick={() => void persist('draft')}>保存草稿</PrimaryButton></View>
          <PrimaryButton loading={submitting} disabled={saving || uploading} onClick={() => void persist('submit')}>提交签证</PrimaryButton>
        </>}
      </PageBody>
    </Screen>
  )
}
