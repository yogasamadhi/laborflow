# LaborFlow 微信小程序

基于 React 19 的 Taro 微信小程序工程，使用以下技术栈：

- React 19.2
- Taro 4 API（通过 `vite-plugin-taro` 的 React 19 兼容运行时）
- Tailwind CSS 4
- weapp-tailwindcss 5
- Taroify 1
- TypeScript 7、Vite 8、pnpm 11

## 环境要求

- Node.js 22.18.x 或 24.11+
- pnpm 11.2+
- 微信开发者工具稳定版

## 本地开发

```bash
pnpm install --frozen-lockfile
cp .env.example .env.local
pnpm dev
```

如有正式微信 App ID，将它写入不会提交的 `.env.local`：

```text
VITE_PLUGIN_TARO_WECHAT_APP_ID=你的微信AppID
VITE_API_BASE_URL=https://你的服务域名/api
VITE_UPLOAD_URL=https://你的服务域名/api/files/upload
```

开发环境默认连接 `http://localhost:8089/api`。真机或正式环境必须替换为已配置在微信小程序后台的 HTTPS 服务域名；附件地址默认由 `VITE_API_BASE_URL` 推导，无需单独设置。未配置 App ID 时使用微信开发者工具的 `touristappid`，登录页会自动使用账号密码测试模式，不发起微信会话交换。开发构建输出到 `dist/`，在微信开发者工具中导入该目录即可。

如需启用微信绑定，除小程序 `.env.local` 的 `VITE_PLUGIN_TARO_WECHAT_APP_ID` 外，还需在仓库根目录运行环境中配置同一 AppID 的 `WECHAT_MINIAPP_APP_ID` 与 `WECHAT_MINIAPP_APP_SECRET`，然后重启服务与小程序构建。

仓库根目录的 `python3 run.py` 会同时启动该 watch 构建；使用 `python3 run.py --no-miniapp` 可跳过。

## 常用命令

```bash
pnpm dev             # 微信小程序 watch 构建
pnpm dev:h5          # H5 开发服务
pnpm typecheck       # TypeScript 检查
pnpm build           # 微信小程序生产构建
pnpm build:h5        # H5 生产构建
pnpm check           # 类型检查 + 微信小程序构建
```

## 已迁移功能

- 微信登录凭证交换、账号密码登录、微信绑定、JWT 存储与失效回登录。
- 首页项目、工资、日报、签证及有权限用户的待办摘要。
- 工资历史、工资条明细、工资条确认与异议反馈。
- 日报/签证的列表、筛选、详情和草稿/驳回后编辑。
- 日报的项目、施工段、合同清单、工序、班组、实时定价预览、草稿与提交。
- 签证的项目、施工段、数量/金额信息、草稿与提交。
- 日报和签证照片选择、逐张上传、已上传附件绑定/移除，以及按用户隔离的本机草稿恢复。
- 工资、日报两级确认、签证确认金额和班组结算待办的详情、同意与驳回。
- 个人资料刷新与退出登录。

服务端继续是唯一的权限与数据范围判定点；小程序只依据登录用户展示入口，任何写入和审批仍由 API 复核。

## 样式与组件约定

Tailwind v4 的全局入口是 `src/app.css`，由 weapp-tailwindcss 转换为微信小程序可用的 WXSS。新增源码目录仍放在 `src/` 下，入口中的 `@source "./"` 会统一扫描。

Taroify 组件按需引入组件和样式，避免把整个组件库打入主包：

```tsx
import Button from '@taroify/core/button'
import '@taroify/core/button/style'
```

## React 19 说明

官方 Taro 4.2.1 目前仍声明 React 18 peer dependency，直接将官方 Taro CLI 工程升级到 React 19 会遇到 reconciler 与开发构建兼容问题。本工程使用基于 Taro 4 组件、API 和运行时的 `vite-plugin-taro` React 19 兼容实现，并通过 `virtual:taro/api` 与 `virtual:taro/components` 使用 Taro 能力。

Taroify 1.0.5 同样仍声明 React 18 peer dependency。本工程已验证当前示例组件可完成 React 19 微信端与 H5 构建；引入新的 Taroify 组件时仍需完成微信开发者工具预览和真机冒烟，尤其是依赖过渡动画或旧 ReactDOM API 的组件。
