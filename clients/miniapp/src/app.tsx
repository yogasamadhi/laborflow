import { useEffect } from 'react'
import type { PropsWithChildren } from 'react'
import Taro from 'virtual:taro/api'
import '@taroify/icons/van/index.css'
import { iconFontSource } from './lib/icon-font'
import './app.css'

function App({ children }: PropsWithChildren) {
  useEffect(() => {
    void Taro.loadFontFace({ family: 'laborflow-icon', source: iconFontSource, global: true }).catch(() => undefined)
  }, [])

  return children
}

export default App
