# LaborFlow UAT 签字与执行记录

## 1. 当前结论

**状态：BLOCKED，未批准 UAT。**

本记录是 2026-07-13 对正式上线范围所做的证据盘点和 Gate 执行记录，不是批准书。自动化
回归通过，但五类角色的完整非生产 UAT、真实小程序账号流程、弱网证据和授权签字均未完成。
依据 [`LAUNCH_SCOPE.md`](LAUNCH_SCOPE.md) 和
[`LAUNCH_DEFECT_POLICY.md`](LAUNCH_DEFECT_POLICY.md)，当前不能进入 G3 通过结论。

| 字段 | 记录 |
| --- | --- |
| 评审日期 | 2026-07-13 |
| 代码基线 | `67a8cef`，后续 Gate 记录文档在 `loop/L2-27-02-release-gate` 整理 |
| 环境 | 本地、Testcontainers PostgreSQL 18；无生产数据 |
| 数据与凭据 | 未记录测试账号、密码、token、openid、真实个人或工资数据 |
| 关联范围 | [`LAUNCH_SCOPE.md`](LAUNCH_SCOPE.md) |
| 关联问题 | [`LAUNCH_ISSUES.md`](LAUNCH_ISSUES.md) |
| 当前决策 | 不批准；重新审查条件见第 6 节 |

## 2. 自动化 Gate 结果

| 编号 | 检查 | 实际结果 | 状态 | 证据 |
| --- | --- | --- | --- | --- |
| AUTO-01 | `JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test` | 104 tests，0 failures，0 errors，0 skipped；Testcontainers PostgreSQL 18 应用 21 个 Flyway migration | pass | 本次本地 Gate 输出 |
| AUTO-02 | `pnpm --dir web typecheck && pnpm --dir web test && pnpm --dir web build` | typecheck 通过；Vitest 4 files / 8 tests 通过；生产构建通过 | pass | 本次本地 Gate 输出；构建见 OBS-001 |
| AUTO-03 | `npm --prefix miniapp run check` | first-party JS 语法检查通过；Node tests 8/8 通过 | pass | 本次本地 Gate 输出 |
| AUTO-04 | `node scripts/ci/check-docs.mjs` | 38 Markdown 文件和 21 个 Flyway migration 通过 | pass | 本次本地 Gate 输出 |
| AUTO-05 | Gitee Go PR workflow 与 required-status 分支保护 | workflow 文件存在；未取得远端导入、首次运行或保护规则已启用的证据 | blocked | GATE-003 |

`pass` 仅表示该自动化命令的结果，不能替代真实角色业务流程、真实账号登录、设备权限、
弱网、截图/录屏或业务签字。

## 3. 业务 UAT 结果

| UAT | 覆盖角色 | 本次状态 | 原因 | 需要的下一步 |
| --- | --- | --- | --- | --- |
| UAT-01 月度工资生成、审核、发放与员工确认 | FINANCE、BOSS、EMPLOYEE、ADMIN | blocked | 未执行带脱敏夹具的跨角色流程；员工工资条确认依赖真实小程序账号流程 | 创建五类非生产账号与工资夹具，完成全部六步并保留脱敏记录 |
| UAT-02 工程量日报到班组结算 | EMPLOYEE、MANAGER、FINANCE、ADMIN | blocked | 未执行项目成员/非成员越权、附件、确认、结算与审计抽查 | 准备项目夹具，完成五步并记录正向、越权和失败证据 |
| UAT-03 签证变更收入闭环 | EMPLOYEE、MANAGER、BOSS/FINANCE、ADMIN | blocked | 未执行真实提交、附件、金额确认、收入查看与日志抽查 | 准备项目与附件夹具，完成五步并记录正向、越权和失败证据 |

未发现自动化回归中的已确认 P0/P1 缺陷；但上述人工流程未执行，不得据此宣称“无 P0/P1
未关闭缺陷”这一上线条件已被证明。

## 4. 角色签字状态

| 角色 / 签字责任 | 所需确认 | 状态 | 原因 |
| --- | --- | --- | --- |
| ADMIN | 用户、角色、绑定和审计可追溯，越权被拒绝 | blocked | 未执行非生产角色 UAT 与日志抽查 |
| BOSS / 业务负责人 | 工资审核、经营查看及签证收入结果 | blocked | 未执行实际 UAT，未提供业务签字 |
| FINANCE | 工资生成/发放、薪酬来源和项目财务限制 | blocked | 未执行实际 UAT，未提供财务签字 |
| MANAGER | 项目资料、日报/签证确认和结算 | blocked | 未执行实际 UAT，未提供项目签字 |
| EMPLOYEE / 现场人员 | 本人工资、日报、签证、附件和待办体验 | blocked | 小程序真实账号登录与现场流程按用户指示暂不处理 |
| Release QA / Gate 负责人 | 证据完整性与最终 Gate 决策 | signed-not-approved | 已完成证据盘点；阻断项仍开放 |

`signed-not-approved` 只表示本记录的 Gate 结论已被写明，不代表任何业务角色已签字通过。

## 5. 小程序人工证据状态

下列内容来自 [`MINIAPP_VISUAL_QA_CHECKLIST.md`](MINIAPP_VISUAL_QA_CHECKLIST.md)，当前均为
`blocked`，不作为失败的产品结论，也不作为已通过结论：

| 证据项 | 状态 | 原因 | 关联问题 |
| --- | --- | --- | --- |
| 真实账号登录与绑定 | blocked | 按用户指示不处理小程序登录问题 | GATE-002 |
| 日报、照片上传、草稿恢复和提交录屏 | blocked | 未执行带账号的真实流程和弱网场景 | GATE-002 |
| 签证、照片上传、草稿恢复和提交录屏 | blocked | 未执行带账号的真实流程和弱网场景 | GATE-002 |
| 待办同意/驳回录屏 | blocked | 未执行管理角色真实流程 | GATE-002 |
| 工资条确认、异常和失败状态证据 | blocked | 未执行绑定员工的真实流程 | GATE-002 |
| 弱网、重复点击和多视口证据 | blocked | 未执行开发者工具或真机弱网验证 | GATE-002 |

## 6. 重新审查条件

1. 关闭或以正式范围变更批准 GATE-001 至 GATE-003；用户聊天中的“先不要管”不等同于
   缺陷策略要求的书面延期批准。
2. 以非生产夹具执行 `UAT_CHECKLIST.md` 的 UAT-01 至 UAT-03，逐步记录角色、结果、证据
   位置、问题编号和复测结果。
3. 完成小程序真实账号、上传、审批、工资确认、弱网、截图和录屏，或取得符合
   `LAUNCH_DEFECT_POLICY.md` 的书面范围延期批准。
4. 导入并运行 Gitee Go 工作流，配置 required-status 分支保护，并记录可审计的远端结果。
5. 重新运行本记录第 2 节的自动化 Gate，并由业务、财务、项目、现场和 Gate 负责人完成
   实际签字；只有届时全部条件满足才可将结论更新为批准。
