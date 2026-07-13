# LaborFlow Formal Launch Readiness LOOPMAP2

本文档依据 `docs/LOOPRULE.md` 编写，用于把 LaborFlow 从“业务功能基本完整”
推进到“正式上线前可验收、可回归、可交付”的状态。

本图只覆盖先前讨论的非部署事项：业务验收、端到端回归、权限与脱敏、测试门禁、
数据迁移验证、Web/小程序体验、界面美化、交互体验升级、产品文档和最终上线 gate。
它是正式上线前改进的执行地图，不替代每个 loop 的实际执行记录。

暂不覆盖：

- 后端 Docker 镜像、前端静态部署、Nginx、HTTPS、域名、CORS 生产环境配置。
- 备份恢复、监控告警、线上日志采集、灰度/回滚基础设施。
- 真实生产环境账号、密钥、证书和对象存储开通。

这些部署和运维事项继续留在 `docs/LOOPMAP.md` 的 P8/P10 或后续 infra loop 中处理。

## 1. 目标与范围

总体目标：

- 把当前“业务功能基本完整”的 LaborFlow 推进到“非部署事项已具备正式上线可信度”。
- 让每一项上线前改进都能按 `docs/LOOPRULE.md` 拆成单个可审查、可验证、可回滚的 loop。
- 让业务验收、权限回归、迁移验证、Web/小程序体验和最终 gate 都有可执行入口和证据记录。

参考源：

- `docs/LOOPRULE.md`
- 当前代码与功能评估结果
- `docs/LOOPMAP.md` 中已完成的生产化、安全、权限、工资、项目经营闭环
- 本文档后续新增的 UAT、视觉 QA、smoke、回归记录和上线审查文档

目标端：

- 后端：`server`
- Web 管理端：`web`
- 微信小程序现场端：`miniapp`
- 产品、验收和上线准备文档：`docs` 与根目录项目说明

明确不覆盖：

- 部署、运维、HTTPS、域名、反向代理、监控、备份恢复等 infra 工作。
- 真实生产环境账号、密钥、证书和对象存储开通。
- 与正式上线无关的技术栈替换、目录大搬迁或泛化重构。

## 2. 兼容与安全约束

执行本图任何 loop 时，不得回退以下基线：

- 认证与授权：JWT 生命周期、停用用户 token 失效、RBAC、项目数据范围、员工工资本人可见规则不得弱化。
- 审计与证据链：工资、项目、绩效、系统用户、微信绑定的关键操作日志不得减少。
- 文件安全：私有文件下载授权、附件归属校验、上传安全校验不得绕过。
- 数据可信：工资来源快照、项目结算快照、签证确认金额、数据库约束和状态机校验不得放松。
- 敏感信息：身份证、银行卡、手机号、工资等敏感字段不得在无权限角色中完整暴露。
- 历史兼容：已有工资、项目、附件、用户、微信绑定等历史数据迁移和展示不得破坏。
- 凭据安全：不得提交真实 `.env`、token、cookie、私钥、微信 app secret 或生产连接串。

涉及上述约束的 loop 必须在 `验收` 或 `验证` 中写明正向、越权、失败或回归检查。

## 3. 执行约定

每个 loop 必须遵循 `docs/LOOPRULE.md`：

1. 每次只执行一个 loop；前一个 loop 未闭环前，不开始下一个 loop。
2. 开始前记录目标分支并检查工作区：`git status --short --branch`、`git branch --show-current`。
3. 开始前同步目标分支：`git fetch origin`、`git merge --ff-only origin/<target-branch>`。
4. 从已同步目标分支创建专用分支：`git switch -c loop/<loop-id>-<short-topic>`。
5. 只改本 loop 范围内文件，不夹带无关重构、缓存、构建产物、密钥或临时文件。
6. 在 loop 分支上运行验证、审查 `git diff --check` 和 `git diff`，并提交一个可解释的功能 commit。
7. 切回目标分支，再次同步远端后用 `git merge --no-ff <loop-branch>` 合入。
8. 合并后重新运行受影响验证，检查 `git diff --check` 和 `git diff origin/<target-branch>...HEAD`。
9. 推送目标分支：`git push origin <target-branch>`。
10. 推送成功并确认对齐后，清理 loop 分支：`git branch -d <loop-branch>`；如保留分支，必须记录原因、位置和清理责任。
11. 无法运行的验证必须记录原因、影响范围和替代检查。

本地图中的所有计划 loop 默认只能是 `todo`。只有满足 `docs/LOOPRULE.md` 第 7 节完成定义，
并在本文完成记录区追加记录后，才能标记为 `done`。

状态说明：

| 状态 | 含义 |
| --- | --- |
| todo | 尚未开始 |
| doing | 当前正在执行，且已有明确目标分支和 loop 分支 |
| partial | 已做一部分，但未满足验收、验证、提交、推送或分支清理要求 |
| done | 已实现、验证、审查、提交、合并回目标分支、推送远端，并清理 loop 分支；如需保留 loop 分支，已记录原因、位置和清理责任 |

## 4. 当前基线

截至本图编写时，已观察到的基线：

- 后端非沙箱环境执行 `mvn test` 通过：83 tests, 0 failures, 0 errors。
- 沙箱内 `mvn test` 可能因 Mockito/ByteBuddy 无法 self-attach 失败；这不是业务断言失败。
- Web 执行 `pnpm build` 通过，但主 JS chunk 约 1.6 MB，存在代码分割警告。
- 小程序 JS 执行 `node --check` 通过。
- 未发现 `.github` CI。
- Web 缺少独立 `typecheck/test` 脚本和关键页面测试。
- Web 已有完整功能页面，但视觉层级、表格密度、表单引导、操作反馈和错误状态仍偏工程化。
- 小程序已有现场端页面，但还需要更清晰的现场填报路径、弱网反馈、触控态和移动端视觉一致性。
- 后端已有较多单元测试，但缺少数据库/Flyway/API 级集成测试基线。
- 小程序已有现场端页面，但缺少可重复 smoke checklist、弱网/上传失败/重复提交验证。
- 产品文档口径仍需与当前功能重新对齐。

仍不可信或尚未闭环的检查：

- 空库 Flyway 迁移和旧库升级尚未形成自动化或可重复演练。
- Web 关键页面缺少可重复视觉/交互回归记录。
- 小程序真机弱网、上传失败、重复提交、待办审批尚未形成可重复验收记录。
- CI 尚未把后端、Web、小程序和文档检查统一纳入 PR 门禁。

## 5. Loop 表字段与写法

本文各 Loop 表至少包含 `docs/LOOPRULE.md` 要求的字段：

| 字段 | 含义 |
| --- | --- |
| Loop | 稳定编号 |
| Owner | 负责人、角色或执行 agent |
| 状态 | `todo`、`doing`、`partial`、`done` |
| 目标 | 本 loop 要完成的单一目标 |
| 主要范围 | 涉及目录、模块、接口、页面或文档 |
| 验收 | 可验证的完成标准 |
| 验证 | 本 loop 必须运行或人工执行的检查 |

写法约束：

- `验收` 写可观察结果，不写笼统动作。
- `验证` 写真实命令、页面路径、接口路径、截图对象或手工 smoke 方法。
- 视觉和交互 loop 必须说明截图、录屏、多视口或状态覆盖方式。
- 涉及认证、权限、数据写入、迁移、工资发放、项目结算、文件访问的 loop，应扩大验证范围。
- 如果执行时发现某个 loop 同时包含多个目标，必须先拆分，不得硬做成一个大变更。

## 6. Launch Gate

| Gate | 目标 | 必须通过的条件 |
| --- | --- | --- |
| G2A | 上线范围冻结 | 正式上线范围、角色矩阵、验收清单、已知延期项全部成文 |
| G2B | 回归体系可用 | 后端集成测试、Web smoke、小程序 smoke、权限回归、迁移验证都有可执行入口 |
| G2C | 业务 UAT 通过 | 财务、项目经理、老板/管理层、员工/现场人员、管理员验收场景通过并留痕 |
| G2D | 体验验收通过 | Web 与小程序关键流程完成视觉、交互、响应式、错误反馈、可访问性验收 |
| G3 | 非部署正式上线就绪 | 本图必需 loop 全部 `done`，或有明确延期说明；无 P0/P1 未关闭缺陷 |

## 7. Loop Map

### P20：上线范围、角色与业务验收口径

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-20-01 | product/docs | done | 冻结正式上线功能范围 | `docs/PROJECT_BRIEF.md`、`docs/ROADMAP.md`、新增上线范围文档 | 列出本次上线功能、延期功能、明确不做事项；与 Web/后端/小程序真实能力一致 | `git diff --check` |
| L2-20-02 | product/backend/web | done | 建立角色与权限矩阵 | 新增 `docs/ROLE_PERMISSION_MATRIX.md`、后端 controller、Web 菜单 | ADMIN/BOSS/MANAGER/FINANCE/普通员工/项目人员的菜单、接口、数据范围清晰成表 | `rg -n "PreAuthorize|roles" server/src/main/java web/src`; `git diff --check` |
| L2-20-03 | product/docs | done | 编写业务验收清单 | 新增 `docs/UAT_CHECKLIST.md` | 财务、项目经理、老板/管理层、员工/现场人员、管理员均有端到端验收用例 | `git diff --check` |
| L2-20-04 | product/docs | done | 编写初始化验收清单 | `README.md`、新增 `docs/INITIALIZATION_CHECKLIST.md` | 管理员创建、角色初始化、工资项、工序库、测试账号、微信绑定准备步骤清晰 | `git diff --check` |

### P21：核心业务端到端回归

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-21-01 | qa/backend/web | todo | 建立工资主流程 E2E 回归 | 员工、工资档案、月度工资、审批、发放、工资条 | 员工入职 -> 工资档案 -> 生成工资 -> 提交 -> 审核 -> 发放 -> 工资条确认流程可重复验证 | `mvn test`; `pnpm build`; 记录 smoke 结果 |
| L2-21-02 | qa/backend/web/miniapp | todo | 建立项目日报到班组结算 E2E 回归 | 项目、合同清单、日报、确认、班组结算、支付 | 项目创建 -> 合同清单 -> 日报 -> 工程量确认 -> 班组结算 -> 支付流程可重复验证 | `mvn test`; `pnpm build`; `node --check miniapp/**/*.js` 或等价检查 |
| L2-21-03 | qa/backend/web/miniapp | todo | 建立签证变更收入 E2E 回归 | 签证、附件、确认金额、项目看板 | 小程序/管理端提交签证 -> 管理端确认金额 -> 项目看板收入变化可验证 | `mvn test`; `pnpm build` |
| L2-21-04 | qa/backend/web | todo | 建立薪酬来源进入工资 E2E 回归 | 考勤、扣款、绩效、奖金、工资生成来源 | 考勤扣款、法定扣款、绩效、项目奖金能进入工资明细和来源记录 | `mvn test`; `pnpm build` |
| L2-21-05 | qa/docs | todo | 建立回归结果记录模板 | 新增 `docs/REGRESSION_RECORD_TEMPLATE.md` | 每次回归能记录环境、账号、数据、步骤、期望、实际、截图/导出文件、缺陷链接 | `git diff --check` |

### P22：权限、安全与敏感信息回归

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-22-01 | backend/qa | todo | 补齐后端权限集成测试 | `server/src/test`、认证、工资、项目、系统接口 | 普通员工、项目人员、财务、项目经理、管理员的正向/越权访问均有测试 | `mvn test` |
| L2-22-02 | web/qa | todo | 验证 Web 菜单与 403 体验 | `web/src/components/layout`、axios 拦截器、关键页面 | 无权限菜单不展示；直接访问无权限路由/接口时有清晰提示，不白屏 | `pnpm build`; Web smoke |
| L2-22-03 | backend/web | todo | 敏感信息脱敏与查看权限 | 员工、工资、系统用户、导出页面/API | 身份证、银行卡、手机号、工资金额按角色脱敏；有权限角色可查看完整信息 | `mvn test`; `pnpm build`; 权限 smoke |
| L2-22-04 | backend/qa | todo | 审计日志覆盖回归 | 工资、项目、绩效、系统用户、微信绑定 | 关键创建、修改、审批、驳回、发放、导出、绑定/解绑操作都有可查询日志 | `mvn test`; UAT 日志抽查 |
| L2-22-05 | backend/qa | todo | 文件访问与附件归属端到端验证 | 文件上传、工资凭证、日报照片、签证附件、资料归档 | 未授权用户不能下载私有文件；附件不能跨业务重复滥用 | `mvn test`; 文件 smoke |

### P23：后端集成测试与数据迁移可信度

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-23-01 | backend | done | 建立后端集成测试基线 | Testcontainers 或测试数据库 profile、Flyway、认证测试工具 | 集成测试能启动真实数据库或隔离测试库，自动执行迁移 | `mvn test` 或 `mvn verify` |
| L2-23-02 | backend/database | done | 空库 Flyway 迁移验证 | `server/src/main/resources/db/migration`、测试或脚本 | 从空库可完整迁移到最新版本；默认角色、工资项、工序库符合初始化口径 | 迁移测试；`mvn test` |
| L2-23-03 | backend/database | done | 旧库升级迁移演练 | 匿名 fixture、迁移脚本、升级文档 | 含历史工资、项目、附件、用户、微信绑定的旧数据可升级且核心数据不丢 | 升级演练记录；迁移测试 |
| L2-23-04 | backend | done | API 请求校验与错误响应集成测试 | Controller、DTO、GlobalExceptionHandler | 必填、状态、金额、月份、权限错误均返回统一可读错误 | `mvn test` |
| L2-23-05 | backend | done | 关键 API 合约快照 | OpenAPI 或 contract fixture、核心接口响应结构 | 前后端依赖的核心字段有合约记录，破坏性变更能被发现 | `mvn test`; contract check |

### P24：Web 正式版体验与质量门禁

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-24-01 | web | todo | 增加 Web 类型与测试脚本 | `web/package.json`、测试框架配置 | 提供 `typecheck`、`test` 或等价脚本；CI 可直接调用 | `pnpm typecheck`; `pnpm test`; `pnpm build` |
| L2-24-02 | web/qa | todo | 增加 Web 登录与导航 smoke | 登录页、AppLayout、权限菜单 | 登录成功、登录失败、token 失效、菜单显示可自动或半自动验证 | `pnpm test`; `pnpm build` |
| L2-24-03 | web | todo | 统一错误、加载、空状态 | axios、通用组件、员工/工资/项目关键页 | 请求失败、无数据、加载中、无权限均有一致 UI；不会静默失败或白屏 | `pnpm build`; Web smoke |
| L2-24-04 | web | todo | 补强工资关键操作体验 | 月度工资、明细、发放、导出、异常页 | 生成、重算、提交、审核、批量发放、导出有确认、结果反馈和失败提示 | `pnpm build`; 工资 smoke |
| L2-24-05 | web | todo | 补强项目关键操作体验 | 日报、工程量确认、签证、结算、看板 | 项目核心表格有筛选/分页；审批/驳回/支付有确认和结果反馈 | `pnpm build`; 项目 smoke |
| L2-24-06 | web | todo | 路由级懒加载与代码分割 | `web/src/app/router.tsx`、页面 import、Vite 配置 | 生产构建主 chunk 明显下降；关键路由按需加载；无首屏功能回归 | `pnpm build`；记录 chunk 对比 |
| L2-24-07 | web | todo | 大列表分页、筛选和批量操作完善 | 员工、工资明细、日报、结算、绩效列表 | 大数据量下可分页筛选；批量操作有确认、进度和结果反馈 | `pnpm build`; Web smoke |
| L2-24-08 | design/web | todo | 建立 Web 视觉设计基线 | `web/src/styles.css`、通用组件、布局组件 | 颜色、字号、间距、阴影、边框、状态色、图标使用形成统一 token/约定；页面不再呈现零散临时样式 | `pnpm build`; 视觉抽查 |
| L2-24-09 | design/web | todo | 优化整体布局与信息层级 | AppLayout、Dashboard、工资、项目、系统页面 | 顶栏、侧栏、页面标题、操作区、筛选区、表格区层级清晰；首屏能直接进入主要工作流 | `pnpm build`; 桌面截图验收 |
| L2-24-10 | design/web | todo | 表格与表单视觉升级 | 通用 Field、Modal、表格样式、员工/工资/项目表单 | 表格可扫描、列宽稳定、状态标签一致；表单分组、必填、错误、帮助信息清晰 | `pnpm build`; 表单 smoke |
| L2-24-11 | design/web | todo | 关键操作微交互与反馈 | 按钮、Modal、批量操作、上传、导出、审批动作 | hover/focus/disabled/loading/success/error 状态完整；长操作有进度或明确等待反馈 | `pnpm build`; 交互 smoke |
| L2-24-12 | design/web | todo | 空状态、异常状态与引导文案美化 | 通用状态组件、员工/工资/项目/系统页面 | 空数据、无权限、加载失败、操作失败都有业务化文案和下一步操作；不出现生硬技术错误 | `pnpm build`; 状态截图验收 |
| L2-24-13 | design/web | todo | 响应式与窄屏体验 | 主要页面布局、表格滚动、弹窗、筛选区 | 1366px、1440px、移动窄屏/平板宽度下文字不重叠，按钮不溢出，弹窗可滚动操作 | `pnpm build`; 多视口截图验收 |
| L2-24-14 | design/web/qa | todo | 建立 Web 视觉回归截图清单 | 新增 `docs/WEB_VISUAL_QA_CHECKLIST.md`，可选 Playwright 截图 | 登录、仪表盘、员工、工资、项目、绩效、系统关键页面有截图验收清单；后续 UI 改动可比对 | `git diff --check`; 截图 smoke |

### P25：小程序现场端正式版体验

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-25-01 | miniapp/qa | todo | 建立小程序 smoke checklist | 新增 `docs/MINIAPP_SMOKE_CHECKLIST.md`、`miniapp` 页面 | 登录、绑定、工资查看、日报、签证、待办、上传、退出均有检查步骤 | `git diff --check`; 微信开发者工具 smoke |
| L2-25-02 | miniapp | todo | 上传进度、失败重试与错误提示 | `miniapp/services/file.js`、日报/签证页面 | 照片上传有进度；失败可重试；错误提示可读；不会误判提交成功 | `node --check`; 小程序 smoke |
| L2-25-03 | miniapp | todo | 草稿与弱网恢复 | 日报、签证创建页、本地缓存 | 断网/退出后草稿可恢复；提交成功后清理草稿；不会覆盖用户新输入 | `node --check`; 弱网 smoke |
| L2-25-04 | miniapp | todo | 防重复提交 | 日报、签证、待办审批按钮 | 提交中按钮锁定；重复点击不会生成重复业务记录或重复审批 | `node --check`; 小程序 smoke |
| L2-25-05 | miniapp/backend | todo | 小程序接口契约回归 | `miniapp/services`、后端相关接口 | 小程序字段与后端 DTO/状态流一致；后端字段变化能被 smoke 捕获 | `mvn test`; `node --check` |
| L2-25-06 | design/miniapp | todo | 小程序视觉基线统一 | `miniapp/app.wxss`、页面 wxss、Vant 组件主题 | 首页、工资、日报、签证、待办、我的页面颜色、间距、卡片、状态标签一致；触控区域不拥挤 | `node --check`; 真机/开发者工具截图验收 |
| L2-25-07 | design/miniapp | todo | 现场填报路径交互升级 | 日报创建、签证创建、项目/施工段/工序选择 | 现场人员能按“选择项目 -> 填工程量/签证 -> 上传照片 -> 保存/提交”的自然顺序完成；必填和错误提示清楚 | `node --check`; 小程序流程 smoke |
| L2-25-08 | design/miniapp | todo | 待办审批体验升级 | 待办列表、审批/驳回交互、意见输入 | 待办按类型和紧急度可扫描；审批、驳回、意见输入、结果反馈清晰；误触风险降低 | `node --check`; 小程序待办 smoke |
| L2-25-09 | design/miniapp | todo | 工资查看与工资条确认体验升级 | 工资列表、工资详情、明细项、确认/反馈 | 员工能清楚看到月份、应发、扣减、实发、发放状态和异常反馈入口；金额展示稳定易读 | `node --check`; 工资查看 smoke |
| L2-25-10 | design/miniapp/qa | todo | 建立小程序视觉回归清单 | 新增 `docs/MINIAPP_VISUAL_QA_CHECKLIST.md` | 主要页面、弹窗、空状态、弱网、上传中、提交中均有截图/录屏验收要求 | `git diff --check`; 微信开发者工具 smoke |

### P26：自动化质量门禁

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-26-01 | ci/backend | done | 建立后端测试门禁 | `.github/workflows` 或等价 CI、`server` | PR 自动运行后端测试；失败阻断合入 | CI dry run 或本地等价命令 |
| L2-26-02 | ci/web | done | 建立 Web 构建与测试门禁 | CI、`web` | PR 自动运行 Web typecheck/test/build；失败阻断合入 | CI dry run 或本地等价命令 |
| L2-26-03 | ci/miniapp | done | 建立小程序语法与 smoke 门禁 | CI、`miniapp`、smoke 文档 | PR 自动运行小程序 JS 语法检查；人工 smoke checklist 有提交模板 | CI dry run；`node --check` |
| L2-26-04 | ci/docs | done | 建立文档与 diff 检查门禁 | CI、Markdown、迁移文件 | PR 至少运行 `git diff --check`；可选检查文档链接和 migration 命名 | CI dry run |
| L2-26-05 | ci/qa | done | 建立回归证据归档规则 | PR 模板、回归记录模板 | 影响工资/项目/权限/迁移的 PR 必须附验证记录或说明不适用 | PR 模板检查 |

### P27：产品文档、操作手册与最终上线 Gate

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- |
| L2-27-01 | docs/product | todo | 更新 README 与产品文档口径 | `README.md`、`docs/PROJECT_BRIEF.md`、`docs/ROADMAP.md` | 文档不再停留在 v0.1；准确描述当前正式版功能边界 | `git diff --check` |
| L2-27-02 | docs/product | todo | 编写角色操作手册 | 新增 `docs/USER_GUIDE_*.md` | 财务、项目经理、老板/管理层、员工/现场人员、管理员均有操作路径 | `git diff --check` |
| L2-27-03 | docs/qa | todo | 编写缺陷分级与上线阻断规则 | 新增 `docs/LAUNCH_DEFECT_POLICY.md` | P0/P1/P2 缺陷定义、阻断条件、延期批准规则清晰 | `git diff --check` |
| L2-27-04 | release/qa | todo | 建立 UAT 问题跟踪与签字表 | 新增 `docs/UAT_SIGNOFF.md` | 各角色验收结果、问题、处理状态、签字/确认人可记录 | `git diff --check` |
| L2-27-05 | release | todo | 正式上线非部署 Gate 审查 | `docs/LAUNCH_READINESS_REVIEW.md` | 本图所有必需 loop 已 done 或明确延期；测试、UAT、权限、迁移、文档证据齐全 | 完整回归命令；人工 gate 审查 |

## 8. 建议执行顺序

1. 先做 P20：冻结范围、角色矩阵和验收清单，避免后续 loop 目标漂移。
2. 再做 P23 与 P26：把集成测试、迁移验证和 CI 门禁立起来，让后续变更可回归。
3. 接着做 P21 与 P22：跑通核心业务和权限安全的端到端回归。
4. 然后做 P24 与 P25：补 Web 和小程序正式版体验、界面美化、交互升级。
5. 最后做 P27：文档、UAT、缺陷策略和最终 gate 收口。

依赖与阻塞条件：

- P21/P22 的业务和权限回归，应在 P20 的上线范围与角色矩阵明确后执行。
- P23 的迁移和集成测试基线，应优先于大量业务改动完成，否则回归可信度不足。
- P24/P25 的体验升级应引用 P20/P21 的关键流程和 P24/P25 的视觉 QA 清单。
- P27 的最终 gate 必须等待 P20-P26 中必需 loop 全部 `done` 或有明确延期批准。

## 9. 每类 Loop 的最小验证建议

| 变更类型 | 最小验证 |
| --- | --- |
| 文档-only | `git diff --check`；人工检查路径、命令、角色名称是否真实存在；审查 `git diff` |
| 后端 Java 代码 | `JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test` |
| 后端集成测试/迁移 | `mvn test` 或 `mvn verify`，并记录测试数据库或 Testcontainers 方式 |
| Web 代码 | `pnpm typecheck`、`pnpm test`、`pnpm build`，按项目实际脚本调整 |
| Web 性能 | `pnpm build`，记录主 chunk 和 gzip 体积变化 |
| Web 视觉/交互 | `pnpm build`，关键页面桌面/窄屏截图，记录 hover/focus/loading/error 状态 |
| 小程序 JS | `find miniapp -name '*.js' -exec node --check {} \;` 或等价检查 |
| 小程序体验 | 微信开发者工具 smoke，记录机型、网络、账号、步骤和截图 |
| 小程序视觉/交互 | 真机或开发者工具截图/录屏，覆盖触控态、上传中、提交中、弱网、空状态 |
| 权限/安全 | 至少验证匿名 401、越权 403、授权 200、敏感字段脱敏 |
| 端到端业务 | 按 `docs/UAT_CHECKLIST.md` 记录步骤、数据、结果和异常 |

合并回目标分支后，必须重新运行本 loop 受影响验证，并执行：

```bash
git status --short --branch
git diff --check
git diff origin/<target-branch>...HEAD
```

## 10. Done 条件补充

本图中的 loop 除满足 `docs/LOOPRULE.md` 的完成定义外，还必须满足：

- 若涉及用户可见流程，必须更新或引用对应 smoke/UAT 步骤。
- 若涉及界面美化或交互升级，必须附关键页面截图/录屏或视觉 QA 记录，不能只凭口头评价标记完成。
- 若涉及响应式布局，必须至少覆盖桌面主宽度和一个窄屏视口，确认文字、按钮、弹窗和表格不重叠、不溢出。
- 若涉及操作反馈，必须覆盖正常、加载中、成功、失败、禁用或无权限状态中的相关状态。
- 若涉及权限、数据范围、敏感信息、文件访问、工资发放、项目结算或迁移，必须有自动化测试或明确人工验证记录。
- 若引入延期项，必须写入正式上线范围或缺陷策略文档，不能只在聊天或提交说明中口头记录。
- 若本地沙箱无法运行某验证，必须用非沙箱、CI 或人工替代验证，并记录原因。
- 未推送目标分支、未合并回目标分支、未清理 loop 分支且未记录保留原因时，不得标记为 `done`。
- 如果工作区或分支状态有未解释残留，不得标记为 `done`。

## 11. 完成记录

每完成一个 loop，在对应表格中把状态改为 `done`，并在本节追加一条记录。不要覆盖历史记录。

```text
Loop:
Target branch:
Loop branch:
Commit:
Merge commit:
Verification:
Diff review:
Remote sync:
Push:
Loop branch cleanup:
Current branch/worktree:
Notes:
```

示例：

```text
Loop: L2-20-03
Target branch: main
Loop branch: loop/L2-20-03-uat-checklist
Commit: <commit-sha>
Merge commit: <merge-sha>
Verification: git diff --check; reviewed docs/UAT_CHECKLIST.md manually
Diff review: passed; reviewed final diff against origin/main
Remote sync: fetched and fast-forwarded main before branch creation and before merge
Push: pushed main to origin
Loop branch cleanup: deleted local loop/L2-20-03-uat-checklist
Current branch/worktree: main, clean
Notes: Added UAT checklist covering finance, project manager, boss, employee/site worker, and admin roles.
```

```text
Loop: L2-20-01
Target branch: master
Loop branch: loop/L2-20-01-launch-scope
Commit: a3c7921 loop: freeze formal launch scope
Merge commit: a94fc48 merge: complete L2-20-01 launch scope
Verification: git diff --check passed; reviewed current Web routes, miniapp page registry, and backend controller surface against the frozen scope
Diff review: passed; reviewed git diff --stat and git diff --name-status origin/master...HEAD after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master was pushed to origin at a765434 after explicit user authorization; this status correction is pushed immediately after its record commit
Loop branch cleanup: deleted local loop/L2-20-01-launch-scope after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added docs/LAUNCH_SCOPE.md as the current formal-release scope; PROJECT_BRIEF.md and ROADMAP.md now label the old staged plan as historical design baseline. An initial remote-push restriction was lifted by explicit user authorization on 2026-07-13; the implementation, merge, push, and cleanup are now complete.
```

```text
Loop: L2-20-02
Target branch: master
Loop branch: loop/L2-20-02-role-permission-matrix
Commit: 74ed117 loop: align role permissions
Merge commit: a315d37 merge: complete L2-20-02 role matrix
Verification: server mvn test passed (84 tests); web pnpm build passed; miniapp JavaScript syntax check passed; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-20-02-role-permission-matrix is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added the role/data-scope matrix; separated project business management from finance access; required project access for creating or moving daily reports and variation orders; aligned Web menus and miniapp todo fetches with the same role rules.
```

```text
Loop: L2-20-03
Target branch: master
Loop branch: loop/L2-20-03-uat-checklist
Commit: c6925b7 loop: add business UAT checklist
Merge commit: 6f16161 merge: complete L2-20-03 UAT checklist
Verification: git diff --check passed; reviewed UAT roles, prerequisites, three cross-role flows, evidence requirements, and explicit negative authorization checks
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-20-03-uat-checklist is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added executable UAT coverage for ADMIN, BOSS, FINANCE, MANAGER, EMPLOYEE, salary-to-payslip, daily-report-to-settlement, and variation-income flows.
```

```text
Loop: L2-20-04
Target branch: master
Loop branch: loop/L2-20-04-initialization-checklist
Commit: f169801 loop: add initialization readiness
Merge commit: e5855a2 merge: complete L2-20-04 initialization
Verification: server mvn test passed (89 tests, including BootstrapAdminRunnerTest); git diff --check passed; reviewed initialization checklist against Flyway seed data, self-registration behavior, role matrix, and WeChat configuration keys
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-20-04-initialization-checklist is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added an environment-only, no-active-admin bootstrap with credential validation, password hashing, audit logging, and non-admin elevation protection; documented the complete initialization acceptance path.
```

```text
Loop: L2-23-01
Target branch: master
Loop branch: loop/L2-23-01-integration-baseline
Commit: 5186bd8 loop: add postgres integration baseline
Merge commit: b84f62e merge: complete L2-23-01 integration baseline
Verification: server mvn test passed (90 tests); Testcontainers started PostgreSQL 18, Flyway applied 21 migrations from an empty database, anonymous system access returned 401, bootstrap ADMIN login and authenticated system user access returned 200; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-23-01-integration-baseline is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added Testcontainers PostgreSQL and JUnit Jupiter dependencies, a real HTTP integration test, and local/CI Docker requirements in README.
```

```text
Loop: L2-23-02
Target branch: master
Loop branch: loop/L2-23-02-flyway-empty-db
Commit: b35f198 loop: verify empty database migrations
Merge commit: 2b6b9e9 merge: complete L2-23-02 empty migration
Verification: server mvn test passed (91 tests); Testcontainers PostgreSQL 18 applied Flyway V1-V21 from an empty database; assertions verified V21, five active default role codes, required active salary items, and 17 active work processes; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-23-02-flyway-empty-db is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: The empty-database baseline now fails when the latest migration version or initialization data drifts from the documented contract.
```

```text
Loop: L2-23-03
Target branch: master
Loop branch: loop/L2-23-03-legacy-upgrade
Commit: 9d54d0b loop: verify legacy database upgrades
Merge commit: a5e4d02 merge: complete L2-23-03 legacy upgrade
Verification: server mvn test passed (92 tests); Testcontainers PostgreSQL 18 migrated an anonymous V12 fixture to V21 and retained user, employee, salary, project, daily-report attachment, variation attachment, WeChat binding, and project-member data; V14/V16 defaults and V20 confirmed amount backfill were asserted; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-23-03-legacy-upgrade is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added a repeatable anonymous historical-data upgrade fixture and an upgrade guide; production backup, restore, large-volume performance, and real object-storage transfer remain explicitly out of scope for this non-deployment loop.
```

```text
Loop: L2-23-04
Target branch: master
Loop branch: loop/L2-23-04-api-validation-errors
Commit: 6811f00 loop: cover API validation errors
Merge commit: ba1927b merge: complete L2-23-04 API validation errors
Verification: server mvn test passed (96 tests); real HTTP Testcontainers PostgreSQL coverage asserted unified {code,message,data} errors and HTTP statuses for missing required login fields, unreadable JSON, invalid salary month, negative project amount, invalid project status, and EMPLOYEE salary-management denial; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-23-04-api-validation-errors is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Global errors now return 400 for request/business validation, 409 for integrity conflicts, 403 for forbidden access, and retain the existing Result envelope; malformed JSON returns a clear request-body message.
```

```text
Loop: L2-23-05
Target branch: master
Loop branch: loop/L2-23-05-api-contract-snapshots
Commit: ee97b5a loop: snapshot core API contracts
Merge commit: 554f772 merge: complete L2-23-05 API contracts
Verification: server mvn test passed (97 tests); CoreApiContractIntegrationTest ran against Testcontainers PostgreSQL 18 and verified the checked-in JSON contract for Result envelope, login, current identity, project create/page, monthly salary create/page, and salary overview response fields; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-23-05-api-contract-snapshots is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: The versioned JSON fixture is the explicit review surface for intentional core response-shape changes; removing or renaming any captured field now fails the integration test until the contract is deliberately updated.
```

```text
Loop: L2-26-01
Target branch: master
Loop branch: loop/L2-26-01-backend-ci-gate
Commit: 9cac623 loop: add backend CI quality gate
Merge commit: 49c4550 merge: complete L2-26-01 backend CI gate
Verification: Ruby YAML parsing passed for .workflow/PRPipeline.yml; the exact CI-equivalent Maven command `mvn -B -ntp -f server/pom.xml test` passed locally with 97 tests and Testcontainers PostgreSQL 18; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-26-01-backend-ci-gate is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added the Gitee Go PR workflow and activation/protection instructions. Gitee Go pipeline creation/import, Docker-capable runner selection, first remote run, and required-status branch protection are repository administrator settings and are explicitly documented; they were not claimed as a local remote run.
```

```text
Loop: L2-26-02
Target branch: master
Loop branch: loop/L2-26-02-web-ci-gate
Commit: 9689311 loop: add web quality gate
Merge commit: 97e9ac0 merge: complete L2-26-02 web CI gate
Verification: Ruby YAML parsing passed for .workflow/PRWebPipeline.yml; pnpm typecheck passed; Vitest passed (2 tests); pnpm build passed; git diff --check passed. The build reports an existing 1.60 MB main chunk warning, which remains scheduled for L2-24-06 route-level code splitting.
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-26-02-web-ci-gate is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added pnpm typecheck/test scripts, a focused Vitest utility baseline, frozen-lockfile CI installation, and the Gitee Go PR workflow. Pipeline import, first remote run, and required-status branch protection remain explicit Gitee repository administrator settings.
```

```text
Loop: L2-26-03
Target branch: master
Loop branch: loop/L2-26-03-miniapp-ci-gate
Commit: 40f093d loop: add miniapp syntax gate
Merge commit: b20b047 merge: complete L2-26-03 miniapp CI gate
Verification: Ruby YAML parsing passed for .workflow/PRMiniappPipeline.yml; npm --prefix miniapp run check passed and ran node --check for every first-party miniapp JavaScript file; git diff --check passed
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-26-03-miniapp-ci-gate is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added a Gitee Go PR syntax workflow and a reusable smoke evidence template for login, binding, salary, daily report, variation, to-do, and session scenarios. Detailed steps, visual evidence, and weak-network validation remain in the dedicated miniapp quality loops; pipeline import, first remote run, and required-status branch protection remain Gitee administrator settings.
```

```text
Loop: L2-26-04
Target branch: master
Loop branch: loop/L2-26-04-docs-diff-gate
Commit: ef29d85 loop: add docs and diff CI gate
Merge commit: 11e269b merge: complete L2-26-04 docs diff gate
Verification: git diff --check origin/master...HEAD passed; node scripts/ci/check-docs.mjs passed for 17 repository-maintained Markdown files and 21 Flyway migrations; Ruby YAML parsing passed for .workflow/PRDocsPipeline.yml
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-26-04-docs-diff-gate is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added a dependency-free PR gate for whitespace errors, local Markdown links, and Flyway migration filename/version integrity. Pipeline import, first remote run, and required-status branch protection remain explicit Gitee administrator settings.
```

```text
Loop: L2-26-05
Target branch: master
Loop branch: loop/L2-26-05-regression-evidence
Commit: 5c0d23d loop: add regression evidence policy
Merge commit: 6b68442 merge: complete L2-26-05 regression evidence
Verification: node scripts/ci/check-docs.mjs passed for 20 repository-maintained Markdown files and 21 Flyway migrations; git diff --check and git diff --check origin/master...HEAD passed; manually reviewed the root PR template, required-category checks, exception fields, detailed record template, and data-safety rules
Diff review: passed; reviewed final name-status and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-26-05-regression-evidence is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added a repository-controlled root PULL_REQUEST_TEMPLATE.md, a detailed regression evidence record template, and a reviewer policy that blocks merge when payroll, project, permission, or migration evidence is missing or an exception is not concretely accepted. Completed evidence remains in the PR or approved QA artifact and must use redacted non-production data.
```
