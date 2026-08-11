import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vitePluginTaro, { type VitePluginTaroTarget } from 'vite-plugin-taro'

const targetEnvName = 'VITE_PLUGIN_TARO_TARGET'
const projectRoot = fileURLToPath(new URL('.', import.meta.url))

function getTarget(env: Record<string, string>): VitePluginTaroTarget {
  const target = env[targetEnvName]
  if (target === 'wx' || target === 'h5') return target
  throw new Error(`${targetEnvName} must be "wx" or "h5".`)
}

function fromRoot(...segments: string[]) {
  return path.resolve(projectRoot, ...segments)
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_PLUGIN_TARO_')
  const target = getTarget(env)
  const wechatAppId = env.VITE_PLUGIN_TARO_WECHAT_APP_ID || 'touristappid'

  return {
    build: {
      outDir: target === 'wx' ? fromRoot('dist') : fromRoot('dist', 'h5')
    },
    plugins: [
      vitePluginTaro({
        target,
        app: 'src/app.tsx',
        pages: [
          {
            path: 'pages/login/index',
            config: {
              navigationStyle: 'custom'
            }
          },
          {
            path: 'pages/home/index',
            config: { navigationStyle: 'custom' }
          },
          {
            path: 'pages/salary/index',
            config: { navigationStyle: 'custom' }
          },
          {
            path: 'pages/site/index',
            config: { navigationStyle: 'custom' }
          },
          {
            path: 'pages/todo/index',
            config: { navigationStyle: 'custom' }
          },
          {
            path: 'pages/profile/index',
            config: { navigationStyle: 'custom' }
          },
          {
            path: 'pages/salary/detail',
            config: { navigationBarTitleText: '工资条' }
          },
          {
            path: 'pages/report/create',
            config: { navigationBarTitleText: '现场填报' }
          },
          {
            path: 'pages/variation/create',
            config: { navigationBarTitleText: '现场上报' }
          }
        ],
        appJson: {
          lazyCodeLoading: 'requiredComponents',
          window: {
            backgroundColor: '#f6f8f7',
            navigationBarBackgroundColor: '#ffffff',
            navigationBarTextStyle: 'black',
            navigationBarTitleText: 'LaborFlow'
          },
          tabBar: {
            color: '#9aa19d',
            selectedColor: '#329477',
            backgroundColor: '#ffffff',
            borderStyle: 'white',
            list: [
              { pagePath: 'pages/home/index', text: '首页' },
              { pagePath: 'pages/salary/index', text: '工资' },
              { pagePath: 'pages/site/index', text: '现场' },
              { pagePath: 'pages/todo/index', text: '待办' },
              { pagePath: 'pages/profile/index', text: '我的' }
            ]
          }
        },
        projectConfigJson: {
          appid: wechatAppId,
          projectname: 'laborflow-miniapp',
          description: 'LaborFlow 微信小程序',
          compileType: 'miniprogram',
          setting: {
            urlCheck: false,
            es6: true,
            postcss: false,
            minified: false,
            compileHotReLoad: true,
            enhance: true
          }
        },
        projectPrivateConfigJson: {
          setting: {
            urlCheck: false
          }
        },
        sitemapJson: {
          rules: [{ action: 'allow', page: '*' }]
        }
      })
    ]
  }
})
