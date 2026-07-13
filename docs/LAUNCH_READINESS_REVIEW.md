# LaborFlow 非部署正式上线就绪审查

## 1. 决策

**2026-07-13 结论：NOT APPROVED（不批准进入部署阶段）。**

本审查只覆盖正式上线前的产品、质量、UAT 和治理条件，不覆盖部署、域名、HTTPS、监控、
备份、生产密钥或基础设施。自动化回归已通过，但 `G2C`、`G2D` 和远端质量门禁证据未满足；
因此不能把完成的工程 loop 或本地测试写成正式上线已批准。

| 字段 | 记录 |
| --- | --- |
| 审查日期 | 2026-07-13 |
| 审查基线 | `67a8cef`；Gate 记录在 `loop/L2-27-02-release-gate` 整理 |
| 范围 | [`LAUNCH_SCOPE.md`](LAUNCH_SCOPE.md) 中的非部署条件 |
| 证据策略 | [`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md) |
| UAT 记录 | [`UAT_SIGNOFF.md`](UAT_SIGNOFF.md) |
| 问题台账 | [`LAUNCH_ISSUES.md`](LAUNCH_ISSUES.md) |
| 缺陷与延期规则 | [`LAUNCH_DEFECT_POLICY.md`](LAUNCH_DEFECT_POLICY.md) |

## 2. Gate 结果

| Gate | 条件 | 状态 | 实际证据与结论 |
| --- | --- | --- | --- |
| G2A 上线范围冻结 | 范围、角色矩阵、验收清单和已知延期项成文 | pass | `LAUNCH_SCOPE.md`、角色矩阵、UAT 清单、角色手册和缺陷策略已在库；L2-27-01 已完成 |
| G2B 回归体系可用 | 后端、Web、小程序、文档、权限与迁移有可执行入口 | pass-local / blocked-remote | 本地后端 104、Web 8、小程序 8、文档/21 migrations 通过；Gitee Go 远端导入、首跑和 required-status 保护未证实，见 GATE-003 |
| G2C 业务 UAT | 五类角色关键场景通过并留痕 | blocked | UAT-01 至 UAT-03 及五类角色签字均未执行；没有非生产夹具、业务证据或签字，见 GATE-001 |
| G2D 体验验收 | Web 与小程序关键流程的状态、响应式、错误反馈和可访问性验收 | blocked | 小程序真实登录、上传、审批、工资确认、弱网、截图和录屏按用户指示暂不处理；已登录 Web 与小程序视觉证据也未完整形成，见 GATE-001、GATE-002 |
| G3 非部署正式上线就绪 | 必需条件满足、无 P0/P1 未关闭缺陷、延期有效 | not-approved | G2C/G2D/GATE-003 未通过；不能证明 P0/P1 为零，也没有有效延期批准 |

## 3. 自动化回归证据

| 检查 | 结果 | 覆盖说明 |
| --- | --- | --- |
| 后端 Maven | pass，104 tests | Testcontainers PostgreSQL 18、21 Flyway migration、工资/项目/权限/文件/审计等自动化回归 |
| Web | pass，typecheck；Vitest 4 files / 8 tests；production build | 生产构建通过；`SalaryStatisticsPage` 惰性 chunk 警告记录为 OBS-001 |
| 小程序 | pass，8 Node tests | JS 语法、会话、服务契约、上传部分失败、登录初始化和本机草稿；不覆盖真实账号流程 |
| 文档和迁移 | pass，38 Markdown / 21 migrations | 本地链接、Flyway 命名和版本检查 |

所有命令的详细状态和不能替代的人工范围见 [`UAT_SIGNOFF.md`](UAT_SIGNOFF.md)。

## 4. 阻断项与解除条件

1. **GATE-001：完整跨角色 UAT 和签字。** 建立脱敏非生产夹具，执行 UAT-01 至 UAT-03，
   覆盖正向、越权、失败与审计路径，逐项记录并完成业务、财务、项目、现场和 QA 签字。
2. **GATE-002：小程序真实账号和体验证据。** 完成真实登录/绑定、日报与签证上传、待办、
   工资确认、弱网、重复点击、多视口和截图/录屏；或者按缺陷策略取得书面范围延期批准。
3. **GATE-003：远端质量门禁。** 导入 Gitee Go workflow，运行 PR Gate，并启用 required-status
   分支保护；保留可审计的远端结果。
4. **OBS-001：性能观察项。** 在重新审查时依据目标网络和使用场景决定是否需要进一步优化，
   不得忽略构建警告或将其伪装为已验收性能指标。

## 5. 重新审查与批准规则

Gate 负责人只能在以下所有条件满足后，把结论改为 `APPROVED`：

- GATE-001 至 GATE-003 已关闭，或每一项都有符合
  `LAUNCH_DEFECT_POLICY.md` 的有效范围变更/延期批准。
- UAT 签字记录逐项为 `pass`、`fail` 或有批准的 `not-applicable`，没有以 `blocked` 代替
  通过；五类责任角色已完成真实签字。
- 未关闭 P0/P1 为零，并有足够的人工和自动化证据支撑该判断。
- 在最终基线重新运行后端、Web、小程序和文档 Gate，保存结果和差异审查。

在这些条件满足前，本文件的唯一有效结论是“不批准”。
