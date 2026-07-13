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
6. 在 loop 分支上按内部 checklist 运行直接受影响验证，审查累计 `git diff --check <target-branch>...HEAD` 和 `git diff <target-branch>...HEAD`，并提交一个或一组紧密相关的功能 commit。
7. 切回目标分支，再次同步远端后用 `git merge --no-ff <loop-branch>` 合入。
8. 合并后重新运行受影响验证，检查 `git diff --check` 和 `git diff origin/<target-branch>...HEAD`。
9. 推送目标分支：`git push origin <target-branch>`。
10. 推送成功并确认对齐后，清理 loop 分支：`git branch -d <loop-branch>`；如保留分支，必须记录原因、位置和清理责任。
11. 无法运行的验证必须记录原因、影响范围和替代检查。

### 3.1 粒度策略

本图以“端到端能力块”而不是目录、技术层或机械实施步骤划分 loop。一个能力块可以
跨后端、Web、小程序、测试和文档，但必须有一条完整验收链路和一个整体回滚边界；
这些必要组成部分放在同一 loop 的内部 checklist 中，不分别创建分支和重复完整回归。

- 默认合并：共享同一用户结果、状态机或质量 Gate，并可由一次聚焦 review、一次完整
  验收和一次回滚判断的工作，保留在同一 loop。
- 必须拆分：安全/数据持久化/公共协议的独立决策、可独立上线或回滚的结果、不同环境或
  owner 的阻塞条件，或已大到无法在一次 review 中理解的 diff。
- P24/P25 以管理端工作台、现场端可靠性、视觉与交互能力块组织；P21/P22 以业务链路和
  安全边界组织。测试、CI 接线、smoke 手册和证据模板仅在自身构成独立 Gate 时单列。
- 高风险 loop 在表格中标明风险/回滚边界；完成记录必须说明实际运行的验证和最终 diff。

本次按最新版规则改造时，所有已标记为 `done` 的 Loop 行和既有完成记录保持原样；
只调整尚未闭环的计划边界、依赖、范围和验证方式。已经开始实现但尚未形成完成记录的
能力，也不得借地图改造扩大原定目标。

本地图中的所有计划 loop 默认只能是 `todo`。只有满足 `docs/LOOPRULE.md` 第 7 节完成定义，
并在本文完成记录区追加记录后，才能标记为 `done`。

状态说明：

| 状态 | 含义 |
| --- | --- |
| todo | 尚未开始 |
| doing | 当前正在执行，且已有明确目标分支和 loop 分支 |
| partial | 已做一部分，但未满足验收、验证、提交、推送或分支清理要求 |
| done | 已实现、验证、审查、提交、合并回目标分支、推送远端，并清理 loop 分支；如需保留 loop 分支，已记录原因、位置和清理责任 |

### 3.2 待执行 Loop 元数据

下表只补充尚未闭环 Loop 的推荐字段，不改变第 7 节已完成条目。开始每个 Loop 前，
执行者必须再次核对依赖，并把“不在范围”写入本轮目标说明；依赖未满足时不得创建
实现分支。`P0/P1/P2` 只表达执行优先级，不替代真实状态。

| Loop | 优先级 | 依赖 | 不在范围 |
| --- | --- | --- | --- |
| `L2-22-01` | P0 | L2-20-02、L2-23-01、L2-23-04、L2-23-05 | 审计日志、附件归属和业务页面整体重做 |
| `L2-22-03` | P0 | L2-22-01 | 日志平台部署、长期归档和监控告警 |
| `L2-22-04` | P0 | L2-21-03、L2-22-01 | 对象存储开通、CDN 和生产存储迁移 |
| `L2-24-01` | P0 | L2-22-01 | 工资/项目业务规则、小程序主题和部署层性能配置 |
| `L2-24-02` | P1 | L2-24-01 | 工资计算规则、审批权限和导出协议变更 |
| `L2-24-03` | P1 | L2-24-01 | 项目计价规则、结算状态机和权限变更 |
| `L2-25-01` | P0 | L2-20-02、L2-22-01、L2-23-05 | 新增业务 API、破坏性协议升级和业务页面视觉重做 |
| `L2-25-02` | P1 | L2-21-01、L2-21-02、L2-21-03、L2-25-01 | 后端业务状态机和工资计算规则变更 |
| `L2-27-01` | P1 | P22、P24、P25 必需项闭环 | 部署运维文档、具体缺陷修复和未批准的功能承诺 |
| `L2-27-02` | P0 | P22-P26 必需项、L2-27-01 | 部署执行；Gate 不通过时不得以文档形式宣告上线 |

粒度复核后，以下从未执行的计划项不再保留独立分支生命周期：`L2-22-02` 并入
`L2-22-01`；`L2-24-04` 至 `L2-24-07` 并入 `L2-24-01` 至 `L2-24-03`；
`L2-25-03` 至 `L2-25-05` 并入 `L2-25-02`；`L2-27-03` 并入 `L2-27-01`，
`L2-27-04/L2-27-05` 并入 `L2-27-02`。测试、视觉证据和文档跟随对应能力一起收口。

## 4. 当前基线

当前已观察到的基线：

- 后端非沙箱完整 `mvn test` 已通过 101 tests，包含 Testcontainers PostgreSQL 18、空库
  Flyway、历史库升级、API 合约、工资、项目结算、签证收入和薪酬来源生命周期回归。
- Gitee Go 的后端、Web、小程序和文档 PR workflow 已入库；远端导入、首次运行和分支
  保护仍是仓库管理员设置。
- Web 已有 `typecheck`、Vitest 和生产构建门禁；构建通过，但主 JS chunk 约 1.60 MB，
  代码分割是 L2-24-06 的独立性能边界。
- 小程序已有全量 first-party JavaScript 语法检查和基础 smoke 证据模板；工资条本人读取与
  确认已纳入工资 E2E 回归。
- Web 与小程序已有主要业务页面，但视觉层级、表格与表单密度、异常状态、弱网、触控态
  和多视口体验尚未完成正式版验收。
- 产品文档、角色操作手册、缺陷策略、UAT 签字和最终非部署 Gate 尚未收口。

仍不可信或尚未闭环的检查：

- 权限、脱敏、审计日志和文件访问等高风险边界仍缺少完整 E2E 或越权回归。
- Web 关键页面缺少可重复视觉/交互回归记录和路由级代码分割验证。
- 小程序真机弱网、上传失败、重复提交、待办审批以及视觉录屏尚未形成可重复验收记录。
- Gitee Go 的远端实际运行与 required-status 分支保护尚待管理员完成并留存证据。

## 5. Loop 表字段与写法

本文各 Loop 表至少包含 `docs/LOOPRULE.md` 要求的字段：

| 字段 | 含义 |
| --- | --- |
| Loop | 稳定编号 |
| Owner | 负责人、角色或执行 agent |
| 状态 | `todo`、`doing`、`partial`、`done` |
| 目标 | 本 loop 要完成的单一目标 |
| 主要范围 | 涉及目录、模块、接口、页面或文档 |
| 内部 checklist | 为同一验收结果必须一起完成的紧密相关实现、测试、接线和文档步骤 |
| 风险 / 回滚 | 高风险边界和失败时应整体撤销的范围；纯文档或独立 Gate 可注明低风险 |
| 验收 | 可验证的完成标准 |
| 验证 | 本 loop 必须运行或人工执行的检查 |

写法约束：

- `验收` 写可观察结果，不写笼统动作。
- `内部 checklist` 不是独立 loop；它用于表达同一能力块的必要步骤。
- `风险 / 回滚` 说明支付、权限、数据写入、迁移、公共协议或用户可见流程的隔离边界。
- `验证` 写真实命令、页面路径、接口路径、截图对象或手工 smoke 方法。
- 视觉和交互 loop 必须说明截图、录屏、多视口或状态覆盖方式。
- 涉及认证、权限、数据写入、迁移、工资发放、项目结算、文件访问的 loop，应扩大验证范围。
- 如果执行时发现某个 loop 出现两个可独立交付/回滚的结果，必须先拆分；不要仅因跨
  文件、跨端或同时需要测试和文档而拆分。

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

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-20-01 | product/docs | done | 冻结正式上线功能范围 | `docs/PROJECT_BRIEF.md`、`docs/ROADMAP.md`、上线范围文档 | 核对真实能力；列出延期/不做；写 Gate 入口 | 低风险；回滚为整份范围文档 | 范围、延期和不做事项与三端能力一致 | `git diff --check` |
| L2-20-02 | product/backend/web | done | 建立角色与权限矩阵 | 权限表达式、Web 菜单、`ROLE_PERMISSION_MATRIX.md` | 汇总角色；映射接口/菜单/数据范围；校验来源代码 | 认证与数据范围；回滚为整次矩阵/接线变更 | ADMIN/BOSS/MANAGER/FINANCE/普通员工/项目人员能力清晰成表 | `rg -n "PreAuthorize\|roles" server/src/main/java web/src`; `git diff --check` |
| L2-20-03 | product/docs | done | 编写业务验收清单 | `docs/UAT_CHECKLIST.md` | 准备角色夹具；关键正向流；越权和异常证据 | 低风险；回滚为清单文档 | 各业务角色均有端到端验收用例 | `git diff --check` |
| L2-20-04 | product/docs | done | 编写初始化验收清单 | `README.md`、`INITIALIZATION_CHECKLIST.md` | 管理员；角色/工资项/工序；测试账号/微信准备 | 初始化与凭据；回滚为文档和 bootstrap 接线整体 | 初始化、测试账号和微信准备步骤清晰 | `git diff --check` |

### P21：核心业务端到端回归

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-21-01 | qa/backend/web | done | 建立工资主流程 E2E 回归 | 员工、工资档案、月度工资、审批、发放、工资条 | HTTP 夹具；角色分工；本人读取/确认；Web/小程序 smoke 手册 | 工资状态与金额；回滚为工资自助接口、确认 UI 和测试整体 | 入职到工资条确认可重复验证 | `mvn test`; `pnpm build`; 回归记录 |
| L2-21-02 | qa/backend/web/miniapp | done | 建立项目日报到班组结算 E2E 回归 | 项目、合同清单、日报、确认、班组结算、支付 | 计价夹具；日报确认；结算支付；看板金额断言；smoke 手册 | 项目金额和结算状态；回滚为整条结算回归能力 | 项目创建到班组支付可重复验证 | `mvn test`; `pnpm build`; `npm --prefix miniapp run check` |
| L2-21-03 | qa/backend/web/miniapp | done | 建立签证变更收入 E2E 回归 | 签证、附件、确认金额、项目看板 | 创建/绑定附件；提交；确认金额；看板收入断言；三端 smoke | 收入与附件归属；回滚为签证确认/收入接线整体 | 小程序或管理端提交签证后，确认金额和看板收入可验证 | `mvn test`; `pnpm build`; `npm --prefix miniapp run check` |
| L2-21-04 | qa/backend/web | done | 建立薪酬来源进入工资 E2E 回归 | 考勤、法定扣款、绩效、项目奖金、工资来源 | 构造四类来源；生成工资；明细/来源断言；管理端 smoke | 工资金额和来源快照；回滚为薪酬汇总链路整体 | 各来源可进入工资明细并可追溯 | `mvn test`; `pnpm build` |
| L2-21-05 | qa/docs | done | 复用统一回归结果证据模板 | `PULL_REQUEST_TEMPLATE.md`、回归证据规则和模板 | 统一 PR 分类；详细记录模板；豁免规则；数据脱敏 | 低风险；由 L2-26-05 已闭环产物整体回滚 | 每次回归可记录环境、步骤、结果和证据，不再重复建设模板 | PR 模板检查；`node scripts/ci/check-docs.mjs` |

### P22：权限、安全与敏感信息回归

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-22-01 | backend/web/qa | done | 建立访问控制、敏感数据与无权限体验回归 | 认证、工资、项目、系统、导出 API、AppLayout、axios 和受限路由 | 角色 200/401/403；字段/导出脱敏；菜单与路由直达；401/403/重试反馈；集成与 Web 回归 | 授权、敏感数据和用户可见访问反馈；回滚为访问判定、序列化、导航和错误状态整体 | 各角色授权/越权和脱敏行为可重复验证，无权限入口不展示且直达受限页面不白屏 | `mvn test`; `pnpm typecheck`; `pnpm test`; `pnpm build`; 权限/Web smoke |
| L2-22-03 | backend/qa | done | 建立关键审计日志回归 | 工资、项目、绩效、用户、微信绑定 | 关键动作矩阵；自动化断言；日志查询/抽查路径 | 审计可追溯；回滚为新增日志测试/接线整体 | 创建、修改、审批、驳回、发放、导出和绑定/解绑均可查询 | `mvn test`; UAT 日志抽查 |
| L2-22-04 | backend/qa | done | 验证文件访问与附件归属边界 | 上传、工资凭证、日报照片、签证附件、资料归档 | 授权下载；跨业务重用拒绝；私有附件 smoke；回归断言 | 私有文件与业务归属；回滚为文件授权/绑定变更整体 | 未授权无法下载，附件不能跨业务重复滥用 | `mvn test`; 文件 smoke |

### P23：后端集成测试与数据迁移可信度

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-23-01 | backend | done | 建立后端集成测试基线 | Testcontainers、Flyway、认证测试工具 | PostgreSQL 容器；空库迁移；HTTP 登录/授权；CI 前提 | 测试基础设施；回滚为测试依赖和配置整体 | 集成测试可启动隔离数据库并执行迁移 | `mvn test` |
| L2-23-02 | backend/database | done | 空库 Flyway 迁移验证 | migration、初始化数据、测试 | 最新版本；角色；工资项；工序库断言 | 数据库结构；回滚仅撤销验证测试，迁移不倒退 | 空库可迁移到最新版本且默认数据正确 | `mvn test` |
| L2-23-03 | backend/database | done | 旧库升级迁移演练 | 匿名 fixture、迁移、升级文档 | V12 fixture；升级；核心历史数据断言；操作指引 | 历史数据兼容；回滚为测试/文档，不回退已发布迁移 | 旧数据可升级且工资、项目、附件、用户和微信绑定不丢 | `mvn test`; 升级演练记录 |
| L2-23-04 | backend | done | API 请求校验与错误响应集成测试 | Controller、DTO、异常处理 | 必填；状态/金额/月；权限；统一错误 envelope | 公共 API 错误协议；回滚为异常映射/测试整体 | 错误返回统一且可读 | `mvn test` |
| L2-23-05 | backend | done | 关键 API 合约快照 | contract fixture、核心接口 | 登录/身份；项目；月度工资；字段快照 | 公共协议；回滚为合约 fixture/测试整体 | 破坏性核心字段变化可被发现 | `mvn test`; contract check |

### P24：Web 正式版体验与质量门禁

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-24-01 | design/web/qa | done | 建立 Web 正式版公共体验、视觉与性能基线 | 登录、AppLayout、通用状态、styles/components、响应式、router、`WEB_VISUAL_QA_CHECKLIST.md` | 登录/token 状态；设计 token 与组件层级；loading/empty/error；多视口交互；路由懒加载/chunk；截图基线 | Web 共享基础和加载策略；回滚为壳层、通用组件、样式、路由加载和视觉基线整体 | 登录与访问反馈一致，页面层级可扫描，多视口不溢出，主 chunk 明显下降且关键页面有截图基线 | `pnpm --dir web typecheck`; `pnpm --dir web test`; `pnpm --dir web build`; chunk 对比；1366/1440/窄屏 smoke/截图 |
| L2-24-02 | web/qa | done | 完成工资工作台关键操作与视觉体验 | 月度工资、明细、发放、导出、异常及对应视觉状态 | 生成/重算；提交/审核；批量发放；导出；确认/结果/失败；正常/空/异常截图 | 工资状态和金额操作；回滚为工资工作台及对应视觉基线整体 | 关键工资动作均有确认、结果和失败提示，金额与状态清晰可扫描且可重复视觉验收 | `pnpm --dir web typecheck`; `pnpm --dir web test`; `pnpm --dir web build`; 工资 smoke/截图 |
| L2-24-03 | web/qa | todo | 完成项目工作台、大列表与视觉体验 | 日报、确认、签证、结算、看板、项目列表及对应视觉状态 | 筛选/分页；批量操作；审批/驳回/支付确认；结果反馈；大表格响应式；正常/空/异常截图 | 项目状态和大列表交互；回滚为项目工作台及对应视觉基线整体 | 项目核心表格可扫描，关键操作可安全完成，多视口和异常状态可重复视觉验收 | `pnpm --dir web typecheck`; `pnpm --dir web test`; `pnpm --dir web build`; 项目 smoke/截图 |

### P25：小程序现场端正式版体验

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-25-01 | miniapp/backend/qa | todo | 建立小程序身份、会话与接口契约可靠性基线 | 登录、微信绑定、token、退出、缓存、`miniapp/services`、后端 DTO/状态流 | 登录/绑定正反向；token 失效；缓存清理；字段/状态清单；contract/HTTP 回归；弱网 smoke | 身份、会话和移动端公共协议；回滚为认证、契约 fixture 和服务接线整体 | 身份流程可安全恢复，字段或状态破坏性变化会被自动化或 smoke 捕获 | `mvn test`; `npm --prefix miniapp run check`; 微信开发者工具登录/绑定/弱网 smoke |
| L2-25-02 | design/miniapp/qa | todo | 完成小程序现场工作台的可靠交互与视觉回归 | app theme、日报、签证、上传/草稿、待办审批、工资条、`MINIAPP_VISUAL_QA_CHECKLIST.md` | 填报选择/校验；上传重试/草稿恢复；提交锁定；待办防误触；工资金额/确认；空/弱网/提交中截图录屏 | 现场写入、审批和工资确认；回滚为现场工作台页面、服务接线和视觉基线整体 | 现场人员可在弱网下完成或恢复填报、审批和工资确认，重复触发不产生重复写入，关键状态可重复视觉验收 | `mvn test`; `npm --prefix miniapp run check`; 微信开发者工具全流程 smoke；真机或开发者工具截图/录屏 |

### P26：自动化质量门禁

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-26-01 | ci/backend | done | 建立后端测试门禁 | Gitee Go、`server` | workflow；Java/Docker；Testcontainers；保护说明 | 质量 Gate；回滚为 workflow 文档整体 | PR 可运行后端完整测试 | CI dry run；本地 Maven 等价命令 |
| L2-26-02 | ci/web | done | 建立 Web 构建与测试门禁 | Gitee Go、`web` | frozen install；typecheck；Vitest；build；保护说明 | 质量 Gate；回滚为 workflow/脚本整体 | PR 可运行 Web typecheck/test/build | CI dry run；本地 pnpm 等价命令 |
| L2-26-03 | ci/miniapp | done | 建立小程序语法与 smoke 门禁 | Gitee Go、`miniapp`、smoke 模板 | first-party JS；workflow；人工证据模板 | 质量 Gate；回滚为检查/模板整体 | PR 可运行小程序语法检查并提交 smoke 证据 | CI dry run；`npm --prefix miniapp run check` |
| L2-26-04 | ci/docs | done | 建立文档与 diff 检查门禁 | Gitee Go、Markdown、迁移文件 | whitespace；链接；migration 命名/版本；workflow | 质量 Gate；回滚为检查脚本/workflow整体 | PR 可发现 diff、链接和迁移完整性问题 | CI dry run；`node scripts/ci/check-docs.mjs` |
| L2-26-05 | ci/qa | done | 建立回归证据归档规则 | PR 模板、回归记录模板、评审规则 | 风险分类；记录模板；豁免；数据脱敏；审核 Gate | 证据治理；回滚为模板/规则整体 | 工资/项目/权限/迁移 PR 必须附证据或可接受豁免 | PR 模板检查 |

### P27：产品文档、操作手册与最终上线 Gate

| Loop | Owner | 状态 | 目标 | 主要范围 | 内部 checklist | 风险 / 回滚 | 验收 | 验证 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| L2-27-01 | docs/product/qa | todo | 完成正式版产品文档、角色手册与缺陷治理包 | README、brief、roadmap、`USER_GUIDE_*.md`、`LAUNCH_DEFECT_POLICY.md` | 功能/延期口径；质量入口；五类角色路径；P0/P1/P2；阻断/延期批准；术语和链接 | 对外口径与发布治理；回滚为文档治理包整体 | 正式版边界准确，所有角色有可执行路径，缺陷分级、阻断和批准条件明确 | `git diff --check`; `node scripts/ci/check-docs.mjs`; 角色 walkthrough；Gate 审查 |
| L2-27-02 | release/qa | todo | 执行 UAT 签字与正式上线非部署 Gate | `UAT_SIGNOFF.md`、问题跟踪、`LAUNCH_READINESS_REVIEW.md`、全部证据 | 角色结果；问题/复测；签字；Loop/测试/权限/迁移证据；缺陷/延期批准；最终决策 | 发布 Gate；不通过时不批准上线，不回滚已验收业务数据 | 各角色结果和未决问题可追溯，必需 Loop 完成或正式延期，且无 P0/P1 未关闭缺陷 | 完整回归；UAT 演练与签字；人工 Gate 审查 |

## 8. 建议执行顺序

1. 已完成的 P20、P21、P23、P26 保持冻结，不因后续地图改造重写其范围或完成记录。
2. 先顺序闭环 P22：`L2-22-01` → `L2-22-03` → `L2-22-04`。
3. Web 按公共基础到业务能力推进：`L2-24-01` → `L2-24-02` → `L2-24-03`。
4. 小程序按身份与协议基础到现场工作台推进：`L2-25-01` → `L2-25-02`。
5. 最后顺序闭环 P27：`L2-27-01` 文档治理包 → `L2-27-02` UAT 与非部署 Gate。

依赖与阻塞条件：

- P21/P22 的业务和权限回归，应在 P20 的上线范围与角色矩阵明确后执行。
- P23 的迁移和集成测试基线，应优先于大量业务改动完成，否则回归可信度不足。
- P24/P25 的体验升级应引用 P20/P21 的关键流程；视觉 QA 清单和证据作为对应能力
  Loop 的内部 checklist 同步收口，不再后置为独立文档 Loop。
- P27 的最终 gate 必须等待 P20-P26 中必需 loop 全部 `done` 或有明确延期批准。

## 9. 每类 Loop 的最小验证建议

| 变更类型 | 最小验证 |
| --- | --- |
| 文档-only | `git diff --check`；人工检查路径、命令、角色名称是否真实存在；审查 `git diff` |
| 后端 Java 代码 | `JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test` |
| 后端集成测试/迁移 | 同上，并记录 Testcontainers PostgreSQL、迁移起点和最终版本 |
| Web 代码 | `pnpm --dir web typecheck`、`pnpm --dir web test`、`pnpm --dir web build` |
| Web 性能 | `pnpm --dir web build`，记录主 chunk 和 gzip 体积变化 |
| Web 视觉/交互 | `pnpm --dir web build`，关键页面桌面/窄屏截图，记录 hover/focus/loading/error 状态 |
| 小程序 JS | `npm --prefix miniapp run check` |
| 小程序体验 | 微信开发者工具 smoke，记录机型、网络、账号、步骤和截图 |
| 小程序视觉/交互 | 真机或开发者工具截图/录屏，覆盖触控态、上传中、提交中、弱网、空状态 |
| 权限/安全 | 至少验证匿名 401、越权 403、授权 200、敏感字段脱敏 |
| 端到端业务 | 按 `docs/UAT_CHECKLIST.md` 记录步骤、数据、结果和异常 |

合并回目标分支后，必须重新运行本 loop 受影响验证，并执行：

```bash
git status --short --branch
git diff --check origin/<target-branch>...HEAD
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
Commit(s):
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
Commit(s): <commit-sha>
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

```text
Loop: L2-21-01
Target branch: master
Loop branch: loop/L2-21-01-payroll-e2e
Commit: 113d616 loop: add payroll lifecycle regression
Merge commit: 09412db merge: complete L2-21-01 payroll E2E
Verification: SalaryLifecycleIntegrationTest passed before and after merge against Testcontainers PostgreSQL 18, covering ADMIN employee/profile/monthly creation and generation, BOSS approval, FINANCE payment, and the bound EMPLOYEE's own history/detail/items/payslip confirmation; full server Maven suite passed with 98 tests; pnpm typecheck, pnpm test (2 tests), and pnpm build passed; npm --prefix miniapp run check passed; node scripts/ci/check-docs.mjs passed for 21 repository-maintained Markdown files and 21 Flyway migrations; git diff --check passed. The existing 1.60 MB Web main-chunk warning remains scheduled for L2-24-06.
Diff review: passed; reviewed final name-status, full source/test/runbook content, and diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-21-01-payroll-e2e is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added a self-only salary-detail read endpoint without relaxing the manager-only monthly list; the miniapp now uses that endpoint, displays payslip status, confirms through an explicit modal, and disables repeat confirmation while it is pending. The runbook records the automated and Web/miniapp smoke path using non-production evidence.
```

```text
Loop: L2-21-02
Target branch: master
Loop branch: loop/L2-21-02-report-settlement-e2e
Commit(s): 35db16a loop: add project settlement lifecycle regression
Merge commit: 52d36f7 merge: complete L2-21-02 project settlement E2E
Verification: ProjectSettlementLifecycleIntegrationTest passed before and after merge against Testcontainers PostgreSQL 18, covering project, section, work process, team, team unit price, active contract item, priced daily report, submit, project-manager confirmation, final confirmation, generated/confirmed/paid team settlement, and project-dashboard quantity/value/payable/paid totals; full server Maven suite passed with 99 tests; pnpm typecheck, pnpm test (2 tests), and pnpm build passed; npm --prefix miniapp run check passed; node scripts/ci/check-docs.mjs passed for 22 repository-maintained Markdown files and 21 Flyway migrations; git diff --check and git diff --check origin/master...HEAD passed. The existing 1.60 MB Web main-chunk warning remains scheduled for L2-24-06.
Diff review: passed; reviewed final name-status, complete source/test/runbook content, and accumulated diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-21-02-report-settlement-e2e is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record and separate concurrent-rule-update pushes
Notes: The project flow is one end-to-end capability block: contract pricing feeds the daily-report output snapshot, confirmed reports feed a one-time team settlement using team price, payment updates the settlement, and the dashboard reflects the resulting quantity and financial totals. Detailed field UX, permission negatives, and miniapp weak-network coverage remain in their dedicated loops.
```

```text
Loop: LOOPMAP2-granularity
Target branch: master
Loop branch: loop/loopmap2-granularity
Commit(s): 6a94ce3 loop: rebalance launch readiness map
Merge commit: fc7c1ae merge: complete LOOPMAP2 granularity update
Verification: node scripts/ci/check-docs.mjs passed for 22 repository-maintained Markdown files and 21 Flyway migrations; all Loop table rows passed the escaped-pipe-aware column-count check; git diff --check and git diff --check origin/master...HEAD passed
Diff review: passed; reviewed the complete staged map diff and final diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/loopmap2-granularity is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added the latest LOOPRULE grain policy, current test/CI baseline, internal checklist and risk/rollback fields to every Loop table, and replaced fragmented P24/P25 todo rows with end-to-end capability blocks. L2-21-05 is now explicitly satisfied by the existing L2-26-05 PR evidence policy/template rather than duplicating a second artifact; existing completed function records were preserved.
```

```text
Loop: L2-21-03
Target branch: master
Loop branch: loop/L2-21-03-variation-income-e2e
Commit: e1c35f0 loop: add variation income regression
Merge commit: e1f3894 merge: complete L2-21-03 variation income E2E
Verification: VariationIncomeLifecycleIntegrationTest passed before and after merge against Testcontainers PostgreSQL 18, covering project/section setup, real multipart file upload, private file metadata, attachment binding, submission, pending variation income, commercial confirmation, confirmation history, confirmed amount, revenue breakdown, and contract-derived receivable. FileRecordStorageSwitchTest passed after fixing file-record creation so its required private download path is persisted in the initial insert. The full server Maven suite passed with 100 tests before and after merge; pnpm typecheck and pnpm test (2 tests) passed before merge; pnpm build passed before and after merge; npm --prefix miniapp run check passed; node scripts/ci/check-docs.mjs passed for 23 repository-maintained Markdown files and 21 Flyway migrations. The existing 1.60 MB Web main-chunk warning remains scheduled for L2-24-06.
Diff review: passed; reviewed final name-status, complete source/test/runbook content, and accumulated diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-21-03-variation-income-e2e is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: The miniapp variation attachment adapter now uses the server-supported `photo` type rather than rejected `site_photo`. The E2E flow exposed that file_record.file_path is required at insert time; file IDs and private download paths are now prepared before saving, so file upload works for variation evidence and other file consumers. Detailed field-flow reliability and wider miniapp UX coverage remain in P25.
```

```text
Loop: L2-21-04
Target branch: master
Loop branch: loop/L2-21-04-salary-sources-e2e
Commit: ad9fbaf loop: add salary sources regression
Merge commit: a940b00 merge: complete L2-21-04 salary sources E2E
Verification: SalarySourcesLifecycleIntegrationTest passed before and after merge against Testcontainers PostgreSQL 18, covering an active employee/profile, automatic leave-based attendance deduction, active social-security deduction, confirmed performance score, released and confirmed project bonus allocation, variable-source-only salary generation, four salary items, four signed source snapshots with their source IDs, and compensation-dashboard totals. The full server Maven suite passed with 101 tests before and after merge; pnpm typecheck and pnpm test (2 tests) passed before merge; pnpm build passed before and after merge; node scripts/ci/check-docs.mjs passed for 24 repository-maintained Markdown files and 21 Flyway migrations. The existing 1.60 MB Web main-chunk warning remains scheduled for L2-24-06.
Diff review: passed; reviewed final name-status, complete regression/runbook content, and accumulated diff stat against origin/master after merge
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-21-04-salary-sources-e2e is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: This regression intentionally disables fixed salary so the expected payable, deduction, and actual amounts are attributable only to attendance, statutory deduction, performance, and project bonus sources. Existing aggregation behavior required no production code change; the new test and Web smoke record make the cross-module contract repeatable. Permission-negative coverage remains in P22.
```

```text
Loop: LOOPMAP2-pending-compression
Target branch: master
Loop branch: loop/loopmap2-pending-compression
Commit(s): 42615b8 loop: compress pending readiness loops
Merge commit: eb3b779 merge: compress pending readiness loops
Verification: node scripts/ci/check-docs.mjs passed for 24 repository-maintained Markdown files and 21 Flyway migrations; all Loop table rows passed the escaped-pipe-aware column-count check; the pending Loop count is 10; git diff --check and git diff --check origin/master...HEAD passed
Diff review: passed; reviewed the complete map diff, confirmed no existing `done` row changed, and reviewed the merged diff stat against origin/master
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/loopmap2-pending-compression is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Consolidated only never-executed plan items into ten independent capability loops. Access control, audit traceability, and file-ownership security remain separate risk boundaries; tests, visual evidence, and documents now close with their capability loop rather than a later micro-loop.
```

```text
Loop: L2-22-01
Target branch: master
Loop branch: loop/L2-22-01-access-sensitive-e2e
Commit(s): 8df8b0b loop: add access control regression
Merge commit: c53be3c merge: complete L2-22-01 access control
Verification: AccessControlIntegrationTest passed against Testcontainers PostgreSQL 18; the post-merge full server Maven suite passed with 102 tests; pnpm --dir web typecheck passed; pnpm --dir web test passed with 6 tests; pnpm --dir web build passed with a 1,605.36 kB main chunk (the existing code-splitting warning remains scheduled for L2-24-01); node scripts/ci/check-docs.mjs passed for 25 repository-maintained Markdown files and 21 Flyway migrations; local Vite service returned the Web application. The manual Web smoke steps are recorded in ACCESS_CONTROL_E2E_REGRESSION.md; browser-control tooling was unavailable in this environment, so no screenshot evidence is claimed.
Diff review: passed; reviewed the full functional diff before commit and the merged diff against origin/master; git diff --check and git diff --check origin/master...HEAD passed
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-22-01-access-sensitive-e2e is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added real HTTP authorization coverage for anonymous system access, employee own-salary boundaries, export and approval denial, project-membership scope, and user-field non-disclosure. Web now shares route and menu role groups, presents a protected-route denial state, preserves safe same-origin return paths on re-login, and surfaces 403 messages without replacing server-side authorization.
```

```text
Loop: L2-22-03
Target branch: master
Loop branch: loop/L2-22-03-audit-log-regression
Commit(s): 5d988f2 loop: add audit log regression
Merge commit: 61bbbdf merge: complete L2-22-03 audit log regression
Verification: AuditLogLifecycleIntegrationTest passed against Testcontainers PostgreSQL 18, covering user creation/update; salary monthly creation/update/generation/submission/rejection/resubmission/approval/payment/export; project variation submission/rejection/confirmation; compensation deduction save/update and performance-score confirmation; and administrator WeChat binding/unbinding. The post-merge full server Maven suite passed with 103 tests. node scripts/ci/check-docs.mjs passed for 26 repository-maintained Markdown files and 21 Flyway migrations. The administrator log-query smoke path is documented in AUDIT_LOG_E2E_REGRESSION.md; no manual UAT execution is claimed here.
Diff review: passed; reviewed the focused functional diff and merged diff against origin/master; git diff --check and git diff --check origin/master...HEAD passed
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-22-03-audit-log-regression is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: The regression queries the administrative log endpoint and asserts module, action, business ID, operator identity, and non-empty log content for the listed business actions. It uses a test-only JDBC fixture to create the pending WeChat binding required by the existing administrator bind/unbind API.
```

```text
Loop: L2-22-04
Target branch: master
Loop branch: loop/L2-22-04-file-access-boundaries
Commit(s): f2d58bd loop: secure file access boundaries
Merge commit: a731b92 merge: complete L2-22-04 file access boundaries
Verification: FileAccessBoundaryIntegrationTest passed against Testcontainers PostgreSQL 18 with real multipart storage, covering uploader, associated-project-member, unrelated-project-member, and administrator download outcomes; rejection of daily-report evidence reused as a variation attachment; batch payment of two salary details with one permitted proof; and rejection of that salary proof as a project archive. FileBindingGuardTest and ProjectAttachmentBindingValidationTest also passed. The post-merge full server Maven suite passed with 104 tests. node scripts/ci/check-docs.mjs passed for 27 repository-maintained Markdown files and 21 Flyway migrations. The manual file smoke path is documented in FILE_ACCESS_E2E_REGRESSION.md; no manual smoke execution is claimed here.
Diff review: passed; reviewed the complete staged functional diff and merged diff against origin/master; git diff --check and git diff --check origin/master...HEAD passed
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-22-04-file-access-boundaries is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: FileBindingGuard now locks the private file row within transactional attachment writes and rejects a file that is already bound in a different business domain. Reuse within the same domain remains supported for existing batch salary-proof behavior; the guard covers daily reports, variation orders, project-payment vouchers, project documents, and salary payment proofs.
```

```text
Loop: L2-24-01
Target branch: master
Loop branch: loop/L2-24-01-web-foundation
Commit(s): 0bb900a loop: improve web experience foundation
Merge commit: 33f321d merge: complete L2-24-01 web experience foundation
Verification: Post-merge pnpm --dir web typecheck passed; pnpm --dir web test passed with 3 files and 6 tests; pnpm --dir web build passed. The initial index chunk is 396.80 kB / 127.43 kB gzip, down from the prior 1,605.36 kB / 505.39 kB gzip baseline; ProjectPages is 60.25 kB / 9.83 kB gzip and CompensationPages is 19.26 kB / 3.71 kB gzip. The existing ECharts salary-statistics code is isolated as a 1,036.92 kB / 343.96 kB gzip lazy chunk. node scripts/ci/check-docs.mjs passed for 28 repository-maintained Markdown files and 21 Flyway migrations. The in-app browser rendered login at 1366 x 768 and 390 x 844, and the register mode exposed all expected fields without overlap; authenticated-shell, route-loading, and request-feedback UAT remain explicit non-production-account checkpoints in WEB_VISUAL_QA_CHECKLIST.md.
Diff review: passed; reviewed the complete functional diff before commit and merged diff against origin/master; git diff --check and git diff --check origin/master...HEAD passed
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-24-01-web-foundation is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: The shared login, application shell, feedback states, responsive navigation, and route loading now share a public visual baseline. Route-level lazy loading removes feature modules from the initial route; further ECharts reduction is deliberately separate from this public-shell loop. No payroll or project business rule was changed.
```

```text
Loop: L2-24-02
Target branch: master
Loop branch: loop/L2-24-02-payroll-workspace
Commit(s): bf03353 loop: improve payroll workspace experience
Merge commit: e3f611f merge: complete L2-24-02 payroll workspace experience
Verification: Post-merge pnpm --dir web typecheck passed; pnpm --dir web test passed with 4 files and 8 tests, including salaryWorkflow.test.ts for valid monthly state actions and safeguard copy; pnpm --dir web build passed with a 396.98 kB / 127.52 kB gzip initial index chunk. node scripts/ci/check-docs.mjs passed for 29 repository-maintained Markdown files and 21 Flyway migrations. The payroll and salary-anomaly feature directories contain no remaining window.confirm or window.prompt calls. PAYROLL_WORKSPACE_QA_CHECKLIST.md defines non-production authenticated smoke and screenshot checkpoints; no live approval, payment, export, or authenticated screenshot is claimed because a permitted test account was not available in this loop.
Diff review: passed; reviewed the complete functional diff before commit and merged diff against origin/master; git diff --check and git diff --check origin/master...HEAD passed
Remote sync: fetched origin and fast-forward check passed before branch creation and before merge
Push: master is pushed to origin as part of this loop completion
Loop branch cleanup: local loop/L2-24-02-payroll-workspace is deleted after target push
Current branch/worktree: master, clean and aligned with origin/master after the completion-record push
Notes: Added shared high-impact action confirmation, state-aware monthly action guards, required reject remarks, workflow progress, and loading/empty/error/result states across monthly list/detail, payment, export, payslip, and salary anomaly views. Existing salary calculation rules, API contracts, approval authorization, and export formats remain unchanged.
```
