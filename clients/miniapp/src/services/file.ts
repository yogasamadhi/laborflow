import Taro from 'virtual:taro/api'
import { getToken } from '../lib/auth'
import { uploadUrl } from '../lib/env'
import { maxPhotoCount } from '../lib/format'
import { errorMessage, recoverExpiredSession } from '../lib/request'
import { isRecord, type ApiRecord, type UploadedFile } from '../lib/types'

interface UploadOptions {
  onProgress?: (progress: number) => void
}

interface ChooseAndUploadOptions {
  maxCount?: number
  onProgress?: (progress: { current: number; total: number; progress: number }) => void
}

export class UploadError extends Error {
  uploadedFiles: UploadedFile[]

  constructor(message: string, uploadedFiles: UploadedFile[] = []) {
    super(message)
    this.uploadedFiles = uploadedFiles
  }
}

export function upload(filePath: string, options: UploadOptions = {}): Promise<UploadedFile> {
  const token = getToken()
  return new Promise((resolve, reject) => {
    const task = Taro.uploadFile({
      url: uploadUrl,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (response) => {
        let body: ApiRecord = {}
        try {
          const parsed: unknown = JSON.parse(response.data)
          body = isRecord(parsed) ? parsed : {}
        } catch {
          reject(new Error('上传结果解析失败'))
          return
        }
        if (response.statusCode === 401 && token) {
          recoverExpiredSession()
          reject(new Error('登录已失效'))
          return
        }
        if (response.statusCode === 403) {
          reject(new Error(errorMessage(body, '当前账号无权上传文件')))
          return
        }
        if (response.statusCode >= 200 && response.statusCode < 300 && body.code === 0 && isRecord(body.data) && body.data.id) {
          resolve(body.data as UploadedFile)
          return
        }
        reject(new Error(errorMessage(body, '上传失败')))
      },
      fail: (error) => {
        const message = String(error.errMsg || '')
        reject(new Error(/cancel/i.test(message) ? '已取消上传' : '上传失败，请检查网络后重试'))
      }
    })
    if (options.onProgress) task.onProgressUpdate((event) => options.onProgress?.(event.progress || 0))
  })
}

export function chooseAndUpload(options: ChooseAndUploadOptions = {}): Promise<UploadedFile[]> {
  const requested = Number(options.maxCount)
  const count = Number.isFinite(requested) && requested > 0
    ? Math.min(maxPhotoCount, Math.max(1, Math.floor(requested)))
    : maxPhotoCount
  return new Promise((resolve, reject) => {
    Taro.chooseMedia({
      count,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: async (response) => {
        const uploaded: UploadedFile[] = []
        try {
          for (const [index, file] of response.tempFiles.entries()) {
            const result = await upload(file.tempFilePath, {
              onProgress: (progress) => options.onProgress?.({ current: index + 1, total: response.tempFiles.length, progress })
            })
            uploaded.push(result)
            options.onProgress?.({ current: index + 1, total: response.tempFiles.length, progress: 100 })
          }
          resolve(uploaded)
        } catch (error) {
          reject(new UploadError(error instanceof Error ? error.message : '上传未完成，可稍后重试', uploaded))
        }
      },
      fail: (error) => {
        const message = String(error.errMsg || '')
        reject(new UploadError(/cancel/i.test(message) ? '已取消选择照片' : '选择照片失败，请稍后重试'))
      }
    })
  })
}
