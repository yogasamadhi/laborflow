# 小程序发布检查清单

提交小程序到微信审核前必须逐项确认。任何一项未完成都**不应**提交审核，
否则真机调用会因域名/协议不匹配而失败。

## 1. API 地址

- [ ] 复制 `miniapp/config/env.local.example.js` 为 `miniapp/config/env.local.js`（该文件被 gitignore，不会进库）。
- [ ] 将 `baseURL` 与 `uploadURL` 改为生产 **https** 地址，例如
      `https://api.laborflow.example.com/api`。
      - 生产环境**禁止**使用 `http://` 或 `localhost`。
      - `env.local.js` 必须同时提供 `baseURL` 和 `uploadURL`，否则会回退到
        开发默认值（见 `config/env.js` 的 resolve 逻辑）。
- [ ] 构建产物中确认 `env.js` 实际加载的是生产地址（可在开发者工具 Network
      面板看请求域名）。

## 2. 合法域名（微信公众平台）

- [ ] 登录微信公众平台 → 开发管理 → 开发设置 → 服务器域名。
- [ ] 在 **request 合法域名** 添加生产 baseURL 的域名（https）。
- [ ] 在 **uploadFile 合法域名** 添加生产 uploadURL 的域名（https）。
- [ ] 在 **downloadFile 合法域名** 添加任何文件下载涉及的域名（若有）。

## 3. urlCheck

- [ ] `miniapp/project.config.json` 的 `setting.urlCheck` 在**发布前必须为 `true`**。
      开发期间为 `false` 以便指向 localhost；发布构建必须改回 `true`，
      否则真机不会校验合法域名，可能与平台审核冲突。
- [ ] 改回 `true` 后，在开发者工具里用生产配置跑一遍核心流程（登录、
      日报、签证、工资），确认无“不在以下 request 合法域名列表中”报错。

## 4. AppID 与基础库

- [ ] `miniapp/project.config.json` 的 `appid` 为正式小程序的 AppID（当前为
      `wxcd370211e94d496c`，确认是否为发布目标）。
- [ ] `libVersion` 升级到当前稳定基础库版本（当前 `3.8.12` 偏旧；
      `project.private.config.json` 里漂移到 `3.15.2`，两者需统一）。建议在
      开发者工具里“调试基础库”选一个稳定版并同步到两个配置文件。
- [ ] 确认未使用已废弃 API（如 `wx.getUserProfile`，见 D3 修复说明）。

## 5. 隐私与权限

- [ ] 小程序后台填写《用户隐私保护指引》，声明用到的权限（如相机、相册）。
- [ ] `app.json` 中声明的 `requiredPrivateInfos` / `permission` 与实际使用一致。

## 6. 回归验证（真机）

- [ ] 登录、登出（验证 token 吊销走 `/auth/logout`）。
- [ ] 日报/签证创建、拍照上传、草稿恢复。
- [ ] 工资条查看与确认/异常上报。
- [ ] 首页在弱网/后端不可用时显示错误提示而非空白（见 D1）。

## 7. 回滚准备

- [ ] 记录本次提交的体验版版本号与对应 server 提交。
- [ ] 确认 `env.local.js` 与合法域名可在需要时快速切回旧地址。
