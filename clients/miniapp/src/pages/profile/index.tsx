import { useCallback, useState } from 'react'
import Taro, { useDidShow } from 'virtual:taro/api'
import { Text, View } from 'virtual:taro/components'
import { ContactOutlined, SettingOutlined, UserCircleOutlined } from '@taroify/icons'
import { BottomTabBar, Card, GradientHeader, KeyValueRows, PageBody, PrimaryButton, RoundIcon, Screen, SectionTitle, SecondaryButton, StateView, StatusTag } from '../../components/ui'
import { clearLogin, getUser, refreshUser } from '../../lib/auth'
import type { SessionUser } from '../../lib/types'
import * as authService from '../../services/auth'

export default function ProfilePage() {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)

  const refresh = useCallback(async () => {
    if (refreshing) return
    const currentUser = getUser()
    if (!currentUser) {
      await Taro.reLaunch({ url: '/pages/login/index' })
      return
    }
    setUser(currentUser)
    setRefreshing(true)
    try {
      const data = await authService.me()
      setUser(refreshUser(data))
    } catch (error) {
      Taro.showToast({ title: error instanceof Error ? error.message : '用户信息刷新失败', icon: 'none' })
    } finally {
      setRefreshing(false)
    }
  }, [refreshing])

  useDidShow(() => {
    void refresh()
  })

  const logout = async () => {
    if (loggingOut) return
    setLoggingOut(true)
    try {
      await authService.logout()
    } catch {
      Taro.showToast({ title: '已清理本地登录信息', icon: 'none' })
    } finally {
      clearLogin()
      await Taro.reLaunch({ url: '/pages/login/index' })
      setLoggingOut(false)
    }
  }

  if (!user) return <Screen><StateView text="正在加载用户信息" /><BottomTabBar active="profile" /></Screen>
  const displayName = user.nickname || user.username
  return (
    <Screen>
      <GradientHeader title="个人中心" eyebrow="MY LABORFLOW" subtitle="账号、身份与权限">
        <View className="mt-8 flex items-center gap-4"><View className="flex h-20 w-20 items-center justify-center rounded-full border-4 border-white/30 bg-white text-[30px] font-bold text-[#389477] shadow-lg">{displayName.slice(0, 1).toUpperCase()}</View><View><Text className="block text-2xl font-bold text-white">{displayName}</Text><Text className="mt-1 block text-sm text-white/70">@{user.username}</Text><View className="mt-2"><StatusTag text={user.employeeId ? '员工已绑定' : '员工未绑定'} tone={user.employeeId ? 'primary' : 'warning'} /></View></View></View>
      </GradientHeader>
      <PageBody lifted>
        <SectionTitle title="账户信息" subtitle="来自服务端的最新身份资料" action={<SettingOutlined size={22} color="#55947f" />} />
        <Card><View className="mb-4 flex items-center gap-3"><RoundIcon className="bg-[#eaf7f0] text-[#39987d]"><UserCircleOutlined size={24} /></RoundIcon><View><Text className="block font-bold">基本身份</Text><Text className="mt-1 block text-xs text-[#969d99]">用户编号 {user.userId}</Text></View></View><KeyValueRows rows={[{ label: '登录账号', value: user.username }, { label: '员工绑定', value: user.employeeId ? '已绑定' : '未绑定' }, { label: '角色', value: user.roles.length ? user.roles.join('、') : '-' }]} /></Card>
        <Card className="mint-soft shadow-none"><View className="mb-4 flex items-center gap-3"><RoundIcon className="bg-white text-[#39987d]"><ContactOutlined size={24} /></RoundIcon><View><Text className="block font-bold">我的权限</Text><Text className="mt-1 block text-xs text-[#7d8b84]">入口仅用于展示，服务端会再次校验</Text></View></View><View className="flex flex-wrap gap-2">{user.roles.length ? user.roles.map((role) => <StatusTag key={role} text={role} />) : <Text className="text-sm text-[#929995]">暂无角色</Text>}</View></Card>
        <View className="mt-6 flex gap-3"><View className="flex-1"><SecondaryButton disabled={refreshing || loggingOut} onClick={() => void refresh()}>刷新资料</SecondaryButton></View><View className="flex-1"><PrimaryButton loading={loggingOut} onClick={() => void logout()}>退出登录</PrimaryButton></View></View>
      </PageBody>
      <BottomTabBar active="profile" />
    </Screen>
  )
}
