# LaborFlow Desktop / 小程序全功能闭环回归记录

## 1. 基线与范围

- 日期：2026-07-28
- 代码基线：当前功能闭环工作区；单一提交完成后补记提交编号
- 数据库：Testcontainers PostgreSQL 18，Flyway V1→V24 与 V12→V24
- 外部环境：未使用真实账号、真实工资、openid、生产附件或生产数据库
- 人工结论：五角色 UAT、真机、弱网和业务签字仍为 `BLOCKED`

## 2. 缺陷—实现—证据追踪

| 领域 | 服务端/迁移 | Desktop/小程序 | 自动化证据 |
| --- | --- | --- | --- |
| 项目成员 | `employeeId` 唯一身份、专用 VO、重复校验、V24 清理旧身份 | 成员查询、异步员工选择、职责/启停/删除、项目切换清空员工 | `AccessControlIntegrationTest`、`FlywayLegacyUpgradeIntegrationTest` |
| 绩效计薪 | 方案/成绩状态机、指标和证据快照、自动评分、基数/金额公式、工资唯一成绩 Gate | 绩效方案和评分工作台，金额只读 | `PerformanceScoringRulesTest`、`SalarySourcesLifecycleIntegrationTest` |
| 日报定价 | 定价预览、单清单自动选择、多清单强制选择、无价草稿、提交快照 | Desktop/小程序合同清单、定价警告和提交阻断 | `ProjectSettlementLifecycleIntegrationTest` |
| 待办与审批 | 角色化 `/api/todos`、专用详情、签证确认 DTO、状态重验 | 小程序统一待办、完整详情、加载锁与成功刷新 | `AccessControlIntegrationTest`、签证/结算生命周期回归 |
| 综合仪表盘 | 单接口能力区域和敏感区隔离 | Desktop 角色化综合工作台 | `AccessControlIntegrationTest` |
| Reference/分页 | 最小 Reference VO、服务端筛选和统一页结果 | 异步搜索选择、统一分页；移除固定 100/200/500/1000 请求 | Desktop typecheck、Lint、Vitest、build 与静态搜索 |
| 附件 | `FileMetadataVO`、授权预览/下载、草稿/驳回删除 | 通用附件预览组件、日报/签证/回款/资料接入 | `FileAccessBoundaryIntegrationTest` |
| 操作日志 | ADMIN 安全 VO、组合筛选、时间校验、倒序 | 日志查询、分页、失败重试 | `AuditLogLifecycleIntegrationTest` |
| 小程序现场 | 详情名称、分页、筛选、草稿编辑、工资条反馈 | 第五页签“现场”，删除旧日报/签证空壳列表 | `npm --prefix legacy/miniapp run check` |

## 3. 自动化结果

以下结果均在 2026-07-28 的同一工作区、最终提交前重新执行：

| Gate | 命令 | 结果 |
| --- | --- | --- |
| 服务端 | `mvn -B -ntp -f server/pom.xml clean test` | pass：130 tests，0 failures，0 errors，0 skipped；Docker/Testcontainers PostgreSQL 18 可用 |
| JaCoCo | 检查 `server/target/site/jacoco/index.html` | pass：报告已生成，85 个服务端类完成分析 |
| Desktop 类型 | `pnpm --dir clients/desktop typecheck` | pass |
| Desktop Lint | `pnpm --dir clients/desktop lint` | pass：0 warnings |
| Desktop 测试 | `pnpm --dir clients/desktop test` | pass：7 files / 21 tests |
| Desktop 渲染层构建 | `pnpm --dir clients/desktop build:renderer` | pass：Vite production build |
| 小程序 | `npm --prefix legacy/miniapp run check` | pass：语法、ESLint、19 tests |
| 文档/迁移 | `node scripts/ci/check-docs.mjs` | pass：44 Markdown、24 migrations、`.env.example` 同步 |

自动化 `pass` 只证明工程 Gate，不替代
[`UAT_CHECKLIST.md`](UAT_CHECKLIST.md) 的业务执行或
[`UAT_SIGNOFF.md`](UAT_SIGNOFF.md) 的签字。

### 2026-08-16 Bun 工具链迁移复测

2026-07-28 表格保留当时实际执行的 pnpm、npm 和 Node 命令。客户端工具链迁移后另行复测：

| Gate | Bun 迁移结果 |
| --- | --- |
| Desktop | 冻结安装通过，0 个未信任安装脚本；lint、typecheck、8 files / 24 tests、renderer build、Electron package 和 Windows make 通过 |
| Miniapp | 冻结安装通过，0 个未信任安装脚本；typecheck、微信生产构建和 H5 生产构建通过 |
| Electron runtime | Electron 43 使用其内置 Node 24.18.1，无需单独安装系统 Node.js |

本次复测只验证工具链迁移，没有补齐五角色 UAT、真机、弱网或业务签字。

## 4. 人工与外部 Gate

| 项目 | 状态 | 解除条件 |
| --- | --- | --- |
| ADMIN/BOSS/MANAGER/FINANCE/EMPLOYEE 五角色 UAT | BLOCKED | 用非生产夹具执行 UAT-01 至 UAT-05 并留存证据 |
| 微信真实账号与真机 | BLOCKED | 验证登录、分页、上传、预览、审批和工资条反馈 |
| 弱网与重复点击 | BLOCKED | 开发者工具和真机录屏通过 |
| 目标数据库 V24 审计 | BLOCKED | 运行只读审计并由财务/项目负责人复核 |
| 业务签字与上线批准 | BLOCKED | 所有 P0/P1 关闭且 Gate 负责人正式批准 |
