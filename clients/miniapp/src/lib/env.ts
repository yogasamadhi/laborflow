const defaultApiBaseUrl = 'http://localhost:8089/api'

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl).replace(/\/$/, '')
export const uploadUrl = import.meta.env.VITE_UPLOAD_URL || `${apiBaseUrl}/files/upload`
const wechatMiniappAppId = import.meta.env.VITE_PLUGIN_TARO_WECHAT_APP_ID || ''

export const wechatSessionEnabled = Boolean(wechatMiniappAppId && wechatMiniappAppId !== 'touristappid')
