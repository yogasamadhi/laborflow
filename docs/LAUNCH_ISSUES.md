# LaborFlow 上线 Gate 问题跟踪

## 1. 使用说明

本台账记录正式上线非部署 Gate 的缺陷、证据缺口和外部配置缺口。它不把“尚未测试”写成
产品缺陷，也不把自动化通过写成已完成 UAT。分级、阻断和延期规则见
[`LAUNCH_DEFECT_POLICY.md`](LAUNCH_DEFECT_POLICY.md)，UAT 状态见
[`UAT_SIGNOFF.md`](UAT_SIGNOFF.md)。

状态含义：`open` 尚未处理，`blocked` 等待外部条件或人工验收，`resolved` 已修复待复测，
`closed` 已复测或已获有效批准。任何 P0/P1 未关闭时，Gate 必须不批准。

## 2. 当前问题

| 编号 | 类型 / 等级 | 影响范围 | 状态 | 事实与证据 | 责任角色 | 关闭条件 |
| --- | --- | --- | --- | --- | --- | --- |
| GATE-001 | Gate blocker / 证据缺口，非已确认产品缺陷 | UAT-01、UAT-02、UAT-03 与 ADMIN、BOSS、FINANCE、MANAGER、EMPLOYEE 签字 | blocked | 本轮仅完成自动化回归；没有五类非生产角色、夹具、逐步执行记录、截图/导出或业务签字 | Release QA 与业务、财务、项目负责人 | 所有 UAT 步骤用脱敏夹具执行、记录并复测，或按范围变更正式移除对应能力 |
| GATE-002 | Gate blocker / 人工小程序证据缺口，非已确认产品缺陷 | 微信登录/绑定、日报、签证、上传、待办、工资确认、弱网和录屏 | blocked | 用户于 2026-07-13 指示先不处理小程序登录问题；现有结果仅覆盖未登录模拟器与自动化契约测试 | 移动端 QA 与业务验收负责人 | 完成真实非生产账号和弱网证据，或由业务负责人和 Gate 负责人按缺陷策略完成书面范围延期批准 |
| GATE-003 | Gate blocker / 外部质量配置缺口 | Gitee Go 后端、Web、小程序和文档 PR workflow；required-status 分支保护 | blocked | workflow 文件已入库，但远端导入、首次运行和保护规则已启用的结果未被本地 Gate 观察到 | 仓库管理员 | 提供远端工作流成功记录及受保护分支 required-status 配置证据 |
| OBS-001 | 观察项 / 待性能评估，不是本轮已确认缺陷 | Web `SalaryStatisticsPage` 惰性 chunk | open | 2026-07-13 本地生产构建通过，同时报告约 1,036.92 kB、343.96 kB gzip 的惰性 chunk 警告；初始主入口已保持代码分割 | Web 负责人 | 依据实际网络和用户场景评估；若影响正式范围体验，按 P2 或更高等级登记、修复或批准延期 |

## 3. Gate 结论关联

- 当前没有基于本轮自动化检查确认的 P0/P1 产品缺陷。
- 当前也没有足够的人工 UAT 证据证明 P0/P1 为零，故不能满足正式上线条件。
- GATE-001 至 GATE-003 均未获得符合策略的延期批准；它们共同导致
  [`LAUNCH_READINESS_REVIEW.md`](LAUNCH_READINESS_REVIEW.md) 为“不批准”。
