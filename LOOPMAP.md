# LaborFlow Production Optimization LOOPMAP

本文档依据 `LOOPRULE.md` 编写，用于把 LaborFlow 从当前 MVP/试点状态推进到可生产落地状态。

当前目标不是一次性大改，而是把“安全、权限、审计、数据口径、部署、测试、业务闭环、体验优化”拆成可审查、可验证、可回滚的小闭环。

## 1. 执行约定

每个 loop 必须遵循 `LOOPRULE.md`：

1. 开始前同步远程：`git fetch origin`、`git status --short --branch`。
2. 一个 loop 只完成一个明确目标，不夹带无关重构。
3. 每个 loop 完成后运行本地验证、审查 diff、提交代码。
4. 提交后再次同步远程，必要时合并远端并重跑测试。
5. 推送成功后，loop 才能标记为 `done`。

状态说明：

| 状态 | 含义 |
| --- | --- |
| todo | 尚未开始 |
| doing | 正在执行 |
| partial | 已完成一部分，但未满足验收 |
| done | 已实现、验证、审查、提交并推送远程 |

## 2. Release Gate

| Gate | 目标 | 必须通过的条件 |
| --- | --- | --- |
| G0 | 开发基线可用 | 后端可编译，前端可构建，本地数据库迁移可执行 |
| G1 | 内部试点可用 | 权限、安全、审计、关键业务测试、部署脚本齐备 |
| G2 | 生产预发可用 | 有备份恢复、监控告警、灰度/回滚、HTTPS、真实域名和环境隔离 |
| G3 | 正式上线 | 完成安全验收、数据验收、业务验收、性能验收和上线演练 |

## 3. Loop Map

### P0：生产安全与权限基线

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L0-01 | backend | done | 建立生产环境配置校验 | `server/src/main/resources/application.yml`、后端配置类、`.env.example` | 生产 profile 下缺少强 JWT 密钥、数据库连接、上传目录等关键配置时应用启动失败；默认弱密钥不能用于生产 |
| L0-02 | backend | done | 收紧 OpenAPI 与静态上传访问 | `SecurityConfig`、`OpenApiConfig`、`WebMvcConfig` | 生产环境 Swagger/OpenAPI 默认关闭或仅管理员可见；上传文件不能无鉴权公开读取 |
| L0-03 | backend/web | done | 补齐接口级 RBAC 权限 | `server/src/main/java/com/company/labor/module/**/controller`、`web/src/components/layout/AppLayout.tsx` | 工资、系统、项目利润、审批、发放等敏感接口有角色限制；无权限菜单不展示；无权限请求返回 403 |
| L0-04 | backend | done | 增加数据范围权限 | 员工、工资、项目、日报、签证、统计相关 service/controller | 普通员工只能看本人数据；项目人员只能看授权项目；财务/老板/管理员按角色看到对应范围 |
| L0-05 | backend/web | done | 移除演示账号和弱默认行为 | `README.md`、`PROJECT_BRIEF.md`、`LoginPage.tsx`、seed migration | 登录页不再默认填充 `admin/admin123456`；文档标注初始化后必须改密；生产 seed 不保留弱口令 |

### P1：认证、微信绑定与账号治理

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L1-01 | backend | done | 完善 JWT 生命周期 | `JwtTokenProvider`、`JwtAuthenticationFilter`、auth service | 支持 token 过期策略、刷新策略或重新登录策略；无效 token 有明确错误；停用用户 token 不可继续访问 |
| L1-02 | backend | done | 增加登录安全控制 | auth controller/service、用户表迁移 | 登录失败计数、临时锁定、最后登录时间/IP、密码修改时间可记录；锁定账号不能登录 |
| L1-03 | backend/miniapp | done | 实现微信 code 换 session | 小程序登录接口、微信配置、用户绑定表迁移、`miniapp/services/auth.js` | 小程序可用 `wx.login` code 换取 openid/session；失败有明确提示；不把微信密钥提交到仓库 |
| L1-04 | backend/miniapp/web | done | 实现账号与员工、openid 绑定闭环 | `sys_user`、员工模块、小程序 profile/login 页面、Web 用户管理 | 小程序登录后能绑定平台账号和员工；员工解绑、换绑、停用流程可审计 |
| L1-05 | backend/web | done | 补全用户和角色管理 | `SystemController`、system service、Web 系统页面 | 管理员可创建/停用用户、分配角色、重置密码；非管理员不可访问系统管理 |

### P2：审计、审批与证据链

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L2-01 | backend | done | 全局填充审计字段 | MyBatis `MetaObjectHandler`、安全上下文工具、基础实体 | `createdBy/updatedBy/createdAt/updatedAt` 自动填充；批量操作也能记录操作者 |
| L2-02 | backend | done | 标准化操作日志 | `operation_log`、日志服务、工资/项目/绩效 service | 关键操作记录操作者、模块、业务 ID、动作、摘要、时间；日志不可由普通用户篡改 |
| L2-03 | backend/web/miniapp | done | 完善工资审批证据链 | 工资月表、明细、审核、驳回、发放、凭证上传 | 工资生成、修改、提交、审核、驳回、单人发放、批量发放、导出都有日志和操作者 |
| L2-04 | backend/web/miniapp | done | 完善工程量与签证审批链 | 日报、工程量确认、签证确认、附件表、移动端待办 | 提交、项目经理确认、商务确认、驳回都有状态流、意见、操作者和附件证据 |
| L2-05 | backend/web | done | 关键业务快照固化 | 工资来源、结算详情、签证确认、项目看板 | 工资、结算、奖金池生成后保留来源快照；后续基础资料变化不破坏历史追溯 |

### P3：数据模型、DTO 与校验加固

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L3-01 | backend | done | 拆分请求 DTO 与响应 VO | 员工、工资、项目、绩效、系统 controller/domain | Controller 不再直接接收核心 DO 作为写入请求；内部字段不能被前端覆盖；已通过 `rg @RequestBody.*DO`、`git diff --check`、`mvn test` |
| L3-02 | backend | done | 补齐参数校验 | DTO、Controller、GlobalExceptionHandler | 必填、金额、日期、月份、手机号、身份证、状态枚举有明确校验；错误信息可读；已通过 `rg @RequestBody`、`git diff --check`、`mvn test` |
| L3-03 | database/backend | done | 增加数据库约束 | Flyway migration、mapper/service | 外键、唯一索引、状态 check、月份格式 check、金额范围约束覆盖核心表；已通过 `git diff --check`、`mvn test`，本机无 `psql/pg_ctl` 未执行真实 Flyway migrate |
| L3-04 | backend | done | 统一状态机校验 | 工资、日报、签证、班组结算、奖金池、绩效评分 | 工资、日报、签证、班组结算、奖金池、绩效评分状态变更统一经 `BusinessStateMachine` 校验；非法跳转被拒绝；状态动作保留操作日志/审核记录；已通过 `rg requireAnyStatus/requireStatus`、`git diff --check`、`mvn test` |
| L3-05 | backend | done | 统一软删除与恢复规则 | 员工、工资、项目、基础资料 service/mapper | 员工、工资月表/工资项、项目/施工段/工序/班组、部门/岗位删除与恢复统一走 service；恢复前校验 active 唯一约束；被业务引用的数据拒绝删除；已通过 `rg removeById/deleteById`、`git diff --check`、`mvn test` |

### P4：文件、凭证与资料归档安全

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L4-01 | backend | todo | 上传文件安全校验 | `FileRecordServiceImpl`、文件配置、异常处理 | 文件大小、扩展名、MIME、空文件、危险文件名有校验；非法文件不能入库落盘 |
| L4-02 | backend | todo | 私有文件访问与下载授权 | 文件 controller/service、WebMvc 静态资源配置 | 文件下载必须鉴权；只能访问本人或授权业务关联文件；不再直接公开 `/uploads/**` |
| L4-03 | backend | todo | 凭证和附件归属校验 | 工资凭证、日报照片、签证附件、资料归档 | 附件绑定业务前校验文件存在、上传者或权限合法；同一附件重复绑定有规则 |
| L4-04 | infra/backend | todo | 支持对象存储适配 | 文件存储接口、本地存储实现、对象存储实现配置 | 本地和对象存储可切换；生产配置不依赖应用容器本地磁盘 |

### P5：工资、薪酬与财务业务闭环

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L5-01 | backend/web | todo | 工资生成规则可配置化 | 工资档案、工资项、考勤、绩效、奖金来源 | 固定工资、考勤扣款、绩效、项目奖金可配置启用；生成结果可解释 |
| L5-02 | backend/web | todo | 工资锁账与重算机制 | 月度工资表、明细、来源记录、前端操作 | 草稿可重算；提交后限制修改；审核后锁定；必要调整走冲正或重新生成流程 |
| L5-03 | backend/web | todo | 补充社保、公积金、个税与借支 | 薪酬规则、工资明细、统计、导出 | 法定扣款、借支扣款、其他扣款可维护并进入工资明细和导出 |
| L5-04 | backend/web | todo | 银行代发与工资条确认 | 工资导出、支付记录、员工端工资条 | 可导出银行代发格式；员工端可查看工资条并确认或反馈异常 |
| L5-05 | backend/web | todo | 工资异常处理闭环 | 异常统计、处理记录、工资详情 | 异常不只展示，还能标记已处理、忽略、转入调整；处理过程可审计 |

### P6：项目经营与成本闭环

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L6-01 | backend/web | todo | 项目合同清单与计量口径 | 项目、工序、施工段、合同清单、项目单价 | 合同清单能定义工程量、单位、单价；日报产值计算有明确口径 |
| L6-02 | backend/web/miniapp | todo | 工程量确认与班组结算闭环 | 日报、确认、班组单价、结算、移动端待办 | 已确认工程量才能结算；同一日报不能重复结算；结算可审核、驳回、支付 |
| L6-03 | backend/web/miniapp | todo | 签证变更收入闭环 | 签证、附件、确认金额、项目看板 | 签证从提交到确认金额有状态流；确认金额进入项目经营统计 |
| L6-04 | backend/web | todo | 项目回款、发票、质保金 | 回款记录、发票记录、质保金、项目看板 | 项目能展示合同额、已开票、已回款、应收、质保金和回款进度 |
| L6-05 | backend/web | todo | 成本归集与利润分析 | 成本、材料、机械、班组结算、工资分摊 | 项目毛利、成本构成、现金流、风险预警口径稳定，并可追溯来源 |

### P7：测试、质量门禁与回归体系

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L7-01 | backend | todo | 建立后端单元测试基线 | `server/src/test`、工资、项目、权限核心 service | `mvn test` 有真实用例；工资生成、状态机、权限至少覆盖核心路径 |
| L7-02 | backend | todo | 建立后端集成测试基线 | Testcontainers 或测试数据库、Flyway、Controller 测试 | 数据库迁移可在测试中执行；核心 API 有集成测试；认证和权限可验证 |
| L7-03 | web | todo | 建立前端类型与组件测试 | `web/package.json`、测试框架、关键页面 | 有 `typecheck/test` 脚本；登录、工资、项目关键页面有基础测试 |
| L7-04 | miniapp | todo | 建立小程序核心流程验证 | 小程序服务封装、登录、日报、签证、工资历史 | 至少有接口封装测试或可执行 smoke checklist；小程序接口与后端契约一致 |
| L7-05 | ci | todo | 建立 CI 门禁 | GitHub Actions 或等价 CI、后端/前端/小程序检查 | PR 自动运行后端测试、前端构建、diff check；失败不能合入 |

### P8：部署、运维与生产环境

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L8-01 | infra/backend | todo | 后端 Docker 镜像 | `server/Dockerfile`、构建配置、运行环境变量 | 可用镜像启动后端；健康检查可用；不把密钥打进镜像 |
| L8-02 | infra/web | todo | 前端静态部署与 Nginx 反代 | `web` 构建、Nginx 配置、API 代理 | 前端静态资源可部署；API 反代、history fallback、缓存策略可用 |
| L8-03 | infra | todo | 完整 compose 或部署脚本 | `docker-compose.yml`、环境模板、启动文档 | 一条命令可启动 PostgreSQL、后端、前端；环境变量清晰；数据卷持久化 |
| L8-04 | infra/database | todo | 数据库备份恢复方案 | PostgreSQL 备份脚本、恢复文档、演练记录 | 可执行备份；可在空库恢复；恢复步骤写入文档 |
| L8-05 | infra/backend | todo | 监控、日志与健康检查 | Actuator 或健康接口、日志配置、部署文档 | 有 `/health` 或等价健康检查；关键错误可检索；日志不输出敏感信息 |
| L8-06 | infra | todo | HTTPS、域名、CORS 与环境隔离 | 反向代理、安全头、CORS、profile | 生产使用 HTTPS；CORS 白名单明确；dev/test/prod 配置隔离 |

### P9：前端体验、性能与可用性

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L9-01 | web | todo | 路由级懒加载与代码分割 | `web/src/app/router.tsx`、页面 import、Vite 配置 | 生产构建主 chunk 明显下降；路由页面按需加载 |
| L9-02 | web | todo | 统一错误、加载、空状态 | 通用组件、axios 拦截器、关键页面 | 请求失败、空数据、加载中、无权限都有清晰 UI；不会静默失败 |
| L9-03 | web | todo | 表格筛选、分页和批量操作体验 | 员工、工资、项目、绩效列表页 | 大列表可分页筛选；批量操作有确认和结果反馈 |
| L9-04 | web | todo | 敏感信息脱敏展示 | 员工、工资、系统用户、导出页面 | 身份证、银行卡、手机号按权限脱敏；有权限角色可查看完整信息 |
| L9-05 | miniapp | todo | 现场端离线/弱网体验 | 小程序日报、签证、文件上传 | 弱网下有保存草稿、上传进度、失败重试；重复提交可防护 |

### P10：文档、验收与上线演练

| Loop | Owner | 状态 | 目标 | 主要范围 | 验收 |
| --- | --- | --- | --- | --- | --- |
| L10-01 | docs | todo | 更新产品与技术文档口径 | `README.md`、`PROJECT_BRIEF.md`、`ROADMAP.md` | 文档与当前功能一致；不再停留在 v0.1 MVP 口径 |
| L10-02 | docs/backend/web | todo | 编写业务验收清单 | 验收文档、工资/项目/小程序流程 | 财务、项目经理、老板、员工四类角色都有验收用例 |
| L10-03 | docs/infra | todo | 编写运维手册 | 部署、备份、恢复、日志、账号初始化 | 新环境可按手册部署；故障恢复步骤明确 |
| L10-04 | release | todo | 预发上线演练 | 预发环境、迁移脚本、回滚脚本、验收记录 | 完成一次从空库部署到业务验收的演练；记录问题和修复 |
| L10-05 | release | todo | 正式上线 gate | 安全、数据、业务、性能、运维验收 | 所有 P0-P9 必需 loop 已 `done` 或有明确延期说明；最终回归通过 |

## 4. 建议执行顺序

1. 先完成 P0、P1、P2、P3，建立生产安全与数据可信底座。
2. 再完成 P7、P8，确保每次改动都可验证、可部署、可回滚。
3. 然后推进 P5、P6，补齐工资和项目经营业务闭环。
4. 最后完成 P9、P10，提升体验并完成上线验收。

## 5. 每个 Loop 的最小验证建议

| 变更类型 | 最小验证 |
| --- | --- |
| 文档-only | `git diff --check` |
| 后端 Java 代码 | `JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test` |
| 前端 Web 代码 | `pnpm build` |
| 小程序代码 | 微信开发者工具预览 smoke，加接口封装测试或人工 checklist |
| 数据库迁移 | 空库执行 Flyway migration，旧库升级 migration |
| 部署配置 | 本地或预发环境完整启动 smoke |
| 权限/安全 | 正向角色通过，越权角色返回 403，匿名请求返回 401 |

## 6. 当前已知基线

截至本文档编写时，已观察到的基线状态：

- 后端可执行 `mvn test` 并成功，但当前未发现实际测试用例。
- 前端可执行 `pnpm build` 并成功，但主 bundle 偏大，需代码分割。
- `docker-compose.yml` 当前只包含 PostgreSQL，尚未包含后端、前端、反向代理等生产部署组件。
- 小程序端已有登录、日报、签证、待办、工资入口，但微信 openid 绑定后端闭环尚未实现。
- 多数接口只有登录鉴权，细粒度角色权限和数据范围权限仍需补齐。

## 7. 完成记录模板

每完成一个 loop，在对应表格中把状态改为 `done`，并在此处追加记录：

```text
Loop:
Commit:
Branch:
Pushed:
验证命令:
结果:
备注:
```

```text
Loop: L0-01
Commit: 59edf86
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added production-only startup validation for JWT secret, database connection/password, and upload directory.
```

```text
Loop: L0-02
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Disabled springdoc by default in production profiles, restricted OpenAPI routes to ADMIN when enabled, and removed anonymous upload resource access.
```

```text
Loop: L0-03
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; pnpm build
结果: passed
备注: Added coarse-grained backend RBAC for sensitive modules and filtered Web sidebar entries by stored user roles.
```

```text
Loop: L0-04
Commit: 6824df1
Branch: codex/production-loopmap-execution
Pushed: yes after partial loop push
验证命令: JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Partially implemented data scope. Non-salary roles can only read their own salary detail items, and non-project-management roles are scoped to their own daily reports and variation orders. Full project authorization remains blocked until a project-member/project-scope model exists.
```

```text
Loop: L0-04 completion
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added project_member authorization model and scoped non-management project list/detail/section access to authorized projects.
```

```text
Loop: L0-05
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: rg -n "admin123456|admin /" README.md PROJECT_BRIEF.md ROADMAP.md server/src/main/resources/db/migration web/src miniapp; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; pnpm build
结果: passed
备注: Removed Web login defaults, removed documented weak default credentials, and added a migration that disables the unchanged weak default admin account.
```

```text
Loop: L1-01
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added explicit JWT expiry/invalid-token errors, returned token expiry metadata on login, and rejected tokens for missing, deleted, or inactive users.
```

```text
Loop: L1-02
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added login security fields, temporary lockout after repeated failures, last login timestamp/IP recording, and locked/disabled account responses.
```

```text
Loop: L1-03
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; node --check miniapp/services/auth.js; node --check miniapp/pages/login/index.js
结果: passed
备注: Added WeChat miniapp code2Session configuration/client, pending openid binding persistence, a public auth session endpoint, and miniapp wx.login exchange handling without committing app secrets.
```

```text
Loop: L1-04
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; pnpm build (in web/)
结果: passed
备注: Bound miniapp openid sessions to platform users/employees after login, added admin bind/unbind/disable APIs with operation logs, and added a Web system page for WeChat binding management.
```

```text
Loop: L1-05
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; pnpm build (in web/)
结果: passed
备注: Added admin user create/update/disable, role assignment, password reset APIs, audit logging, and a Web system user management page. System routes remain protected by ADMIN method security.
```

```text
Loop: L2-01
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added an audit user provider backed by the Spring Security context, filled createdBy/updatedBy in MyBatis MetaObjectHandler, and annotated BaseDO plus non-Base audit entities for automatic fill.
```

```text
Loop: L2-02
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added OperationLogService for standardized module/business/action/operator/content logging, replaced salary/system/wechat handwritten log writes, and added project/compensation status-operation logs.
```

```text
Loop: L2-03
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test; pnpm build (in web/)
结果: passed
备注: Added salary_audit_record evidence records, captured operators and status/proof context for salary creation, generation, edits, approval, rejection and payments, exposed audit records to Web/miniapp clients, and kept salary export operator logging through operation_log.
```

```text
Loop: L2-04
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; node --check miniapp/services/project.js; pnpm build (in web/); JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Added project approval evidence records for daily reports and variation orders, captured server-side operators and before/after statuses, exposed confirm/file evidence endpoints, added Web evidence-chain viewing, and added miniapp service accessors.
```

```text
Loop: L2-05
Commit: this loop commit
Branch: codex/production-loopmap-execution
Pushed: yes after loop push
验证命令: git diff --check; pnpm build (in web/); JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn test
结果: passed
备注: Preserved generated business snapshots for team settlements and bonus pools, kept salary source snapshots through salary_source_record, displayed snapshot names/summaries in Web tables, and added a compensation snapshot unit test.
```
