import { useCallback, useEffect, useState } from 'react'
import Taro from 'virtual:taro/api'
import { Text, View } from 'virtual:taro/components'
import { Lock } from '@taroify/icons'
import { GradientHeader, PageBody, PrimaryButton, RoundIcon, Screen, TextField } from '../../components/ui'
import { setLogin } from '../../lib/auth'
import { wechatSessionEnabled } from '../../lib/env'
import { textOf, type ApiRecord } from '../../lib/types'
import * as authService from '../../services/auth'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [wechatOpenid, setWechatOpenid] = useState('')
  const [wechatBound, setWechatBound] = useState(false)
  const [loading, setLoading] = useState(false)

  const refreshWechatSession = useCallback(async (showError: boolean) => {
    try {
      const result = await Taro.login()
      if (!result.code) throw new Error('微信登录凭证获取失败')
      const session = await authService.wechatSession(result.code)
      setWechatOpenid(textOf(session, 'openid'))
      setWechatBound(Boolean(session.bound))
      return session
    } catch (error) {
      const message = error instanceof Error ? error.message : '微信登录凭证获取失败'
      if (showError) Taro.showToast({ title: message, icon: 'none' })
      throw error
    }
  }, [])

  useEffect(() => {
    if (!wechatSessionEnabled) return
    void refreshWechatSession(false).catch(() => undefined)
  }, [refreshWechatSession])

  const submit = async () => {
    const normalizedUsername = username.trim()
    const normalizedPassword = password.trim()
    if (!normalizedUsername || !normalizedPassword) {
      Taro.showToast({ title: '请输入账号密码', icon: 'none' })
      return
    }
    setLoading(true)
    try {
      let currentWechatOpenid = wechatOpenid
      if (wechatSessionEnabled) {
        try {
          const session = await refreshWechatSession(false)
          currentWechatOpenid = textOf(session, 'openid') || currentWechatOpenid
        } catch (error) {
          const message = error instanceof Error ? error.message : '微信登录凭证获取失败'
          Taro.showToast({ title: message, icon: 'none' })
        }
      }
      const data: ApiRecord = await authService.login({
        username: normalizedUsername,
        password: normalizedPassword,
        wechatOpenid: currentWechatOpenid,
        wechatNickname: ''
      })
      setLogin(data)
      await Taro.switchTab({ url: '/pages/home/index' })
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '登录失败，请重试', icon: 'none' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Screen>
      <GradientHeader title="LaborFlow" eyebrow="建筑劳务协作平台" subtitle="现场工作台" compact />
      <PageBody lifted className="pb-5 pt-5">
        <View className="mb-5 flex items-center gap-3"><RoundIcon className="h-11 w-11 rounded-2xl bg-[#eaf7f0] text-[#39987d]"><Lock size={22} /></RoundIcon><View><Text className="block text-[26px] font-bold leading-tight">欢迎回来</Text><Text className="mt-1 block text-[30px] text-[#87918c]">登录后进入现场工作台</Text></View></View>
        <TextField label="账号" value={username} placeholder="请输入账号" required large onChange={setUsername} />
        <TextField label="密码" value={password} placeholder="请输入密码" required password large onChange={setPassword} />
        <View className="mb-4 rounded-2xl bg-[#eaf7f0] px-4 py-3"><Text className="block text-[28px] leading-5 text-[#2e856b]">{!wechatSessionEnabled ? '当前为账号密码测试模式。' : wechatOpenid ? (wechatBound ? '当前微信已绑定平台账号。' : '登录后将自动绑定当前微信。') : '正在获取微信凭证；失败时仍可账号密码登录。'}</Text></View>
        <PrimaryButton loading={loading} onClick={() => void submit()}>登录并进入</PrimaryButton>
        <Text className="mt-4 block text-center text-[26px] leading-5 text-[#a1a7a3]">仅限已授权的企业账号使用</Text>
      </PageBody>
    </Screen>
  )
}
