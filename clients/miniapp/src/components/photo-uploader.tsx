import { Image, Text, View } from 'virtual:taro/components'
import { DeleteOutlined, PhotoOutlined } from '@taroify/icons'
import { SecondaryButton } from './ui'
import type { UploadedFile } from '../lib/types'

export function PhotoUploader({ files, uploading, removingId = '', attachedIds = [], text, onUpload, onRemove }: {
  files: UploadedFile[]
  uploading: boolean
  removingId?: string
  attachedIds?: string[]
  text: string
  onUpload: () => void
  onRemove: (index: number) => void
}) {
  return (
    <View>
      <View className="mb-4 flex items-center justify-between"><View className="flex items-center gap-2"><PhotoOutlined size={22} color="#3a967b" /><Text className="text-lg font-bold">现场照片</Text></View><Text className="text-xs text-[#969d99]">{files.length}/6</Text></View>
      <Text className="mb-4 block text-xs leading-5 text-[#969d99]">{text}</Text>
      {files.length ? <View className="mb-4 grid grid-cols-3 gap-2">{files.map((file, index) => {
        const fileId = String(file.id)
        const attached = attachedIds.includes(fileId)
        const removing = removingId === fileId
        return <View key={fileId} className="relative overflow-hidden rounded-2xl bg-[#f3f5f2]"><Image className="h-24 w-full" mode="aspectFill" src={String(file.previewUrl || file.url || '')} /><Text className="absolute bottom-1.5 left-1.5 rounded-full bg-black/55 px-2 py-0.5 text-xs text-white">{attached ? '已关联' : '待关联'}</Text><View className="absolute right-1 top-1 flex h-10 w-10 items-center justify-center rounded-full bg-black/55 text-white" onClick={() => { if (!uploading && !removingId) onRemove(index) }}>{removing ? <Text className="text-sm">…</Text> : <DeleteOutlined size={18} />}</View></View>
      })}</View> : <View className="mb-4 flex h-28 flex-col items-center justify-center rounded-2xl border border-dashed border-[#cdded6] bg-[#f7faf8]"><PhotoOutlined size={28} color="#79ae9b" /><Text className="mt-2 text-xs text-[#8fa198]">拍摄或从相册选择现场照片</Text></View>}
      <SecondaryButton loading={uploading} disabled={files.length >= 6 || Boolean(removingId)} onClick={onUpload}>{removingId ? '正在移除…' : uploading ? '正在上传…' : `添加照片（${files.length}/6）`}</SecondaryButton>
    </View>
  )
}
