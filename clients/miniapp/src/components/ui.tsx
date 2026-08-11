import { useEffect } from 'react'
import type { CSSProperties, PropsWithChildren, ReactNode } from 'react'
import Taro from 'virtual:taro/api'
import { Button as TaroifyButton } from '@taroify/core'
import { Button, Input, Picker, Text, Textarea, View } from 'virtual:taro/components'
import { CashOutlined, HomeOutlined, NotesOutlined, TodoListOutlined, UserCircleOutlined } from '@taroify/icons'

function menuBottom(): number {
  try {
    return Taro.getMenuButtonBoundingClientRect().bottom + 14
  } catch {
    return 62
  }
}

export function Screen({ children, className = '' }: PropsWithChildren<{ className?: string }>) {
  return <View className={`min-h-screen bg-[#f7f8f4] pb-8 text-[#252a27] ${className}`}>{children}</View>
}

export function PageBody({ children, lifted = false, className = '' }: PropsWithChildren<{ lifted?: boolean; className?: string }>) {
  return <View className={`relative px-5 pb-7 ${lifted ? '-mt-7 rounded-t-[30px] bg-white pt-7' : 'pt-5'} ${className}`}>{children}</View>
}

export function GradientHeader({ title, subtitle, eyebrow, children, compact = false }: PropsWithChildren<{ title: string; subtitle?: string; eyebrow?: string; compact?: boolean }>) {
  const style: CSSProperties = { paddingTop: `${menuBottom()}px` }
  return (
    <View className={`mint-hero px-5 ${compact ? 'pb-12' : 'pb-16'}`} style={style}>
      {eyebrow ? <Text className="mb-2 block text-xs font-semibold tracking-[0.24em] text-white/75">{eyebrow}</Text> : null}
      <Text className="block text-[30px] font-bold leading-tight tracking-tight text-white">{title}</Text>
      {subtitle ? <Text className="mt-2 block text-sm leading-6 text-white/80">{subtitle}</Text> : null}
      {children}
    </View>
  )
}

export function Card({ children, className = '', onClick }: PropsWithChildren<{ className?: string; onClick?: () => void }>) {
  return <View className={`mb-4 rounded-[24px] bg-white p-5 shadow-[0_10px_30px_rgba(35,83,68,0.07)] ${className}`} onClick={onClick}>{children}</View>
}

export function PageTitle({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return <View className="mb-6 flex items-start justify-between gap-3">
    <View>
      <View className="mb-3 h-1 w-9 rounded-full bg-[#f0d66d]" />
      <Text className="block text-[27px] font-bold leading-tight tracking-tight text-[#252a27]">{title}</Text>
      {subtitle ? <Text className="mt-2 block text-sm leading-5 text-[#8a918d]">{subtitle}</Text> : null}
    </View>
    {action}
  </View>
}

export function SectionTitle({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return <View className="mb-4 flex items-end justify-between gap-3">
    <View><Text className="block text-xl font-bold text-[#252a27]">{title}</Text>{subtitle ? <Text className="mt-1 block text-xs text-[#a0a6a2]">{subtitle}</Text> : null}</View>
    {action}
  </View>
}

export function Notice({ children, tone = 'info' }: PropsWithChildren<{ tone?: 'info' | 'warning' | 'danger' }>) {
  const toneClass = tone === 'danger'
    ? 'bg-[#fff0ee] text-[#c65a4c]'
    : tone === 'warning'
      ? 'bg-[#fff8dc] text-[#9b7a22]'
      : 'bg-[#eaf7f0] text-[#267b63]'
  return <View className={`mb-4 rounded-2xl px-4 py-3 text-sm leading-5 ${toneClass}`}>{children}</View>
}

export function StateView({ text, actionText, onAction }: { text: string; actionText?: string; onAction?: () => void }) {
  return <View className="flex min-h-52 flex-col items-center justify-center gap-4 px-7 text-center">
    <View className="flex h-16 w-16 items-center justify-center rounded-full bg-[#eaf7f0] text-2xl text-[#3aa487]">○</View>
    <Text className="text-sm leading-6 text-[#969d99]">{text}</Text>
    {actionText && onAction ? <Button className="m-0 rounded-full border-0 bg-[#3aa487] px-6 text-sm text-white" onClick={onAction}>{actionText}</Button> : null}
  </View>
}

export function StatusTag({ text, tone = 'primary' }: { text: string; tone?: 'primary' | 'warning' | 'danger' }) {
  const color = tone === 'danger'
    ? 'bg-[#fff0ee] text-[#e06b5d]'
    : tone === 'warning'
      ? 'bg-[#fff7d8] text-[#aa8524]'
      : 'bg-[#e8f6ef] text-[#318b70]'
  return <Text className={`inline-block rounded-md px-2.5 py-1 text-xs font-medium ${color}`}>{text}</Text>
}

export function PrimaryButton({ children, loading = false, disabled = false, onClick }: PropsWithChildren<{ loading?: boolean; disabled?: boolean; onClick?: () => void }>) {
  return <TaroifyButton block size="large" shape="round" color="#39a487" loading={loading} disabled={disabled} onClick={onClick}>{children}</TaroifyButton>
}

export function SecondaryButton({ children, disabled = false, onClick }: PropsWithChildren<{ disabled?: boolean; onClick?: () => void }>) {
  return <Button disabled={disabled} className="m-0 rounded-full border border-[#dce8e2] bg-white px-4 text-sm font-medium text-[#397966]" onClick={onClick}>{children}</Button>
}

function FieldLabel({ label, required, large = false }: { label: string; required: boolean; large?: boolean }) {
  return <Text className={`mb-2 block font-semibold text-[#66706b] ${large ? 'text-[30px]' : 'text-[28px]'}`}>{label}{required ? <Text className="ml-1 text-[#e06b5d]">*</Text> : null}</Text>
}

export function TextField({ label, value, placeholder, required = false, password = false, type = 'text', large = false, onChange }: {
  label: string
  value: string
  placeholder?: string
  required?: boolean
  password?: boolean
  type?: 'text' | 'number' | 'digit'
  large?: boolean
  onChange: (value: string) => void
}) {
  return <View className="mb-4"><FieldLabel label={label} required={required} large={large} /><Input className={`rounded-2xl bg-[#f5f7f4] px-4 text-[#252a27] ${large ? 'h-14 text-[34px]' : 'h-14 text-[30px]'}`} value={value} placeholder={placeholder} password={password} type={type} onInput={(event) => onChange(String(event.detail.value || ''))} /></View>
}

export function TextAreaField({ label, value, placeholder, required = false, onChange }: {
  label: string
  value: string
  placeholder?: string
  required?: boolean
  onChange: (value: string) => void
}) {
  return <View className="mb-4"><FieldLabel label={label} required={required} /><Textarea className="min-h-28 w-full rounded-2xl bg-[#f5f7f4] px-4 py-3 text-[30px] leading-6 text-[#252a27]" value={value} placeholder={placeholder} maxlength={500} onInput={(event) => onChange(String(event.detail.value || ''))} /></View>
}

export function SelectField({ label, value, range, placeholder = '请选择', required = false, disabled = false, onChange }: {
  label: string
  value: string
  range: Array<{ label: string; value: string }>
  placeholder?: string
  required?: boolean
  disabled?: boolean
  onChange: (value: string) => void
}) {
  const index = range.findIndex((item) => item.value === value)
  return <View className="mb-4"><FieldLabel label={label} required={required} /><Picker mode="selector" range={range.map((item) => item.label)} value={index >= 0 ? index : undefined} disabled={disabled || range.length === 0} onChange={(event) => { const item = range[Number(event.detail.value)]; if (item) onChange(item.value) }}><View className={`flex h-14 items-center justify-between rounded-2xl bg-[#f5f7f4] px-4 text-[30px] ${disabled ? 'opacity-50' : ''}`}><Text className={value ? 'text-[#252a27]' : 'text-[#a9afab]'}>{range[index]?.label || placeholder}</Text><Text className="text-xl text-[#8b948f]">⌄</Text></View></Picker></View>
}

export function DateField({ label, value, required = false, onChange }: { label: string; value: string; required?: boolean; onChange: (value: string) => void }) {
  return <View className="mb-4"><FieldLabel label={label} required={required} /><Picker mode="date" value={value} onChange={(event) => onChange(String(event.detail.value))}><View className="flex h-14 items-center justify-between rounded-2xl bg-[#f5f7f4] px-4 text-[30px]"><Text>{value || '请选择日期'}</Text><Text className="text-xl text-[#8b948f]">⌄</Text></View></Picker></View>
}

export function KeyValueRows({ rows }: { rows: Array<{ label: string; value: string }> }) {
  return <View>{rows.map((row) => <View key={row.label} className="flex justify-between gap-5 border-b border-[#edf0ed] py-3.5 last:border-0"><Text className="text-sm text-[#929995]">{row.label}</Text><Text className="max-w-[64%] text-right text-sm font-medium leading-5 text-[#363c38]">{row.value}</Text></View>)}</View>
}

export function Metric({ value, label, className = '' }: { value: ReactNode; label: string; className?: string }) {
  return <View className={`min-w-0 ${className}`}><Text className="block truncate text-[26px] font-bold tracking-tight text-[#252a27]">{value}</Text><Text className="mt-1 block text-xs text-[#929995]">{label}</Text></View>
}

export function RoundIcon({ children, className = '' }: PropsWithChildren<{ className?: string }>) {
  return <View className={`flex h-12 w-12 items-center justify-center rounded-2xl text-2xl ${className}`}>{children}</View>
}

export function FloatingAction({ children, onClick }: PropsWithChildren<{ onClick: () => void }>) {
  return <View className="fixed bottom-40 right-5 z-10 flex h-14 w-14 items-center justify-center rounded-full bg-[#40ad90] text-2xl text-white shadow-[0_10px_24px_rgba(45,145,117,0.32)]" onClick={onClick}>{children}</View>
}

type TabKey = 'home' | 'salary' | 'site' | 'todo' | 'profile'

const tabs: Array<{ key: TabKey; label: string; url: string; icon: ReactNode }> = [
  { key: 'home', label: '首页', url: '/pages/home/index', icon: <HomeOutlined size={24} /> },
  { key: 'salary', label: '工资', url: '/pages/salary/index', icon: <CashOutlined size={24} /> },
  { key: 'site', label: '现场', url: '/pages/site/index', icon: <NotesOutlined size={24} /> },
  { key: 'todo', label: '待办', url: '/pages/todo/index', icon: <TodoListOutlined size={24} /> },
  { key: 'profile', label: '我的', url: '/pages/profile/index', icon: <UserCircleOutlined size={24} /> }
]

export function BottomTabBar({ active }: { active: TabKey }) {
  useEffect(() => {
    void Taro.hideTabBar({ animation: false }).catch(() => undefined)
  }, [])

  return <><View className="h-24" /><View className="app-tabbar fixed bottom-0 left-0 right-0 z-10 flex items-center justify-around"><View className="flex w-full justify-around">{tabs.map((item) => <View key={item.key} className={`flex min-w-14 flex-col items-center gap-1 ${item.key === active ? 'text-[#339477]' : 'text-[#747d78]'}`} onClick={() => { if (item.key !== active) void Taro.switchTab({ url: item.url }) }}>{item.icon}<Text className="text-[28px] font-medium">{item.label}</Text></View>)}</View></View></>
}
