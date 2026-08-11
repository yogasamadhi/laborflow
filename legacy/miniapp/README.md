# LaborFlow Miniapp

微信原生小程序端，定位为建筑劳务项目的现场作业端，不复制 Web 管理后台。

## 功能

- 微信授权后使用平台账号登录并绑定当前小程序会话。
- 首页围绕今日现场工作：今日日报状态、现场快捷入口、待办数量、账号绑定状态。
- 现场人员填报工程量日报，支持保存草稿、上传现场照片、提交和驳回后再提交。
- 现场人员上报签证变更，支持保存草稿、上传现场照片、提交和驳回后再提交。
- 管理角色处理工资审核、工程量确认、签证确认、班组结算审核。
- 员工查看本人工资历史、发放状态和工资条明细。
- 账号、角色、JWT 与现有 Spring Boot API 复用。

## 本地运行

1. 启动后端服务，默认地址为 `http://localhost:8089`。
2. 用微信开发者工具导入 `legacy/miniapp/`。
3. 开发者工具中关闭“校验合法域名”或把本地 API 域名加入配置。
4. 如需切换 API 地址，修改 `config/env.js`。

## Vant Weapp

页面已接入 Vant Weapp 组件。首次导入微信开发者工具后，请执行“工具 -> 构建 npm”，生成 `miniprogram_npm` 后即可预览。

## CI And Smoke Evidence

Run the JavaScript syntax gate locally with:

```bash
npm run check
```

`npm run check` also runs the mocked authentication and service-contract
regression tests. The identity/session contract and the required DevTools
smoke steps are in
[`docs/MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md`](../../docs/MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md).
The field-workspace visual, interaction, upload, and weak-network evidence is
defined in
[`docs/MINIAPP_VISUAL_QA_CHECKLIST.md`](../../docs/MINIAPP_VISUAL_QA_CHECKLIST.md).

The Gitee PR workflow and activation requirements are documented in
[`docs/CI_MINIAPP_GATE.md`](../../docs/CI_MINIAPP_GATE.md). Record manual
user-flow verification with
[`docs/MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](../../docs/MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md).
