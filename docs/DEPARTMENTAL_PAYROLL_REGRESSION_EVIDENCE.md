# 分部门工资核算回归证据

## 运行信息

| 字段 | 记录 |
| --- | --- |
| Pull request / commit | 本文件所在的 `main` 提交，可通过 Git 历史追溯 |
| 测试执行者 | Codex 自动化回归 |
| 日期和时间 | 2026-10-27，Asia/Shanghai |
| 环境 | 本地测试环境；PostgreSQL 18 Testcontainers |
| 构建或版本 | Flyway V25；本文件所在提交 |
| 数据夹具 | `DepartmentalPayrollIntegrationTest` 合成部门工资夹具 |
| 测试角色 | 合成 ADMIN、BOSS、FINANCE、EMPLOYEE 角色 |
| 关联需求 | 工资档案与月度工资按直属部门核算 |

## 变更边界

| 类别 | 受影响行为或数据 | 选择 / 不适用 | 不适用原因 |
| --- | --- | --- | --- |
| 工资或薪酬 | 工资档案启用、部门工资表生成、审批、发放、统计、工资条与导出范围 | 选择 | — |
| 项目或结算 | 未修改项目日报、结算或付款状态机 | 不适用 | 只调整绩效读取上月工资明细的跨部门定位，不改变项目结算数据和权限 |
| 权限或敏感数据 | FINANCE 只读部门 Reference、员工本人明细范围 | 选择 | — |
| 迁移或历史数据 | V25 部门字段、复合唯一约束、存量回填和历史全公司兼容 | 选择 | — |

## 验证结果

| 场景 | 前置条件和步骤 | 预期结果 | 实际结果 | 通过 / 失败 | 证据引用 |
| --- | --- | --- | --- | --- | --- |
| 自动化专项检查 | 执行 `DepartmentalPayrollIntegrationTest` | 部门工资完整业务边界全部通过 | 两个直属部门同月独立建表、生成、提交、审批、发放及统计断言通过 | 通过 | `server/src/test/java/com/company/labor/integration/DepartmentalPayrollIntegrationTest.java` |
| 主要工资流程 | 两个启用部门分别配置在职员工工资档案并生成同月工资表 | 每张表只包含本部门员工，状态和金额独立流转 | 部门范围、明细部门快照及独立审批发放均符合预期 | 通过 | `DepartmentalPayrollIntegrationTest` |
| 允许角色和数据范围 | FINANCE 读取部门 Reference，BOSS 审批部门工资表，EMPLOYEE 查询本人明细 | 各角色仅获得所需范围 | FINANCE 可读取部门层级和状态，审批及本人明细范围通过现有权限集成测试覆盖 | 通过 | `AccessControlIntegrationTest`、`SalaryLifecycleIntegrationTest` |
| 拒绝角色和越权访问 | FINANCE 尝试新增部门，EMPLOYEE 尝试管理月度工资 | 返回统一禁止访问响应 | 部门管理仍拒绝 FINANCE，工资管理仍拒绝 EMPLOYEE | 通过 | `AccessControlIntegrationTest`、`ApiRequestValidationIntegrationTest` |
| 错误和边界行为 | 启用无部门员工档案；同部门同月重复建表；员工调岗后尝试在新部门重复生成 | 明确拒绝且不产生重复工资明细 | 三类场景均被服务端拒绝；原部门草稿重生成移除员工后可在新部门生成 | 通过 | `DepartmentalPayrollIntegrationTest` |
| 直属部门边界 | 一级部门和直属子部门分别有员工并生成工资 | 不向一级部门归并子部门员工 | 两张表仅包含各自直属部门员工 | 通过 | `DepartmentalPayrollIntegrationTest` |
| 桌面端部门展示 | 执行部门分组辅助逻辑测试、类型检查和渲染构建 | 分组、月份标题、部门筛选参数正确，构建成功 | 工资档案部门分组、月度月份分组及默认标题断言通过；类型检查和渲染构建通过 | 通过 | `departmentalPayroll.test.ts`；Desktop `typecheck`、`build:renderer` |
| 小程序待办 | 执行微信与 H5 构建 | 工资待办可携带并展示部门名称 | `check` 和 `build:h5` 均通过 | 通过 | Miniapp 本地质量门输出 |

## 迁移详情

| 检查 | 记录 |
| --- | --- |
| 来源数据库或夹具版本 | 空库 V0；旧版升级夹具 V12 |
| 迁移命令或集成测试 | `mvn -B -ntp -f server/pom.xml test`；`PostgresIntegrationTest`；`FlywayLegacyUpgradeIntegrationTest` |
| 迁移结果 | 通过；全量后端 131 个测试全部通过，V0 和 V12 夹具均升级至 V25 |
| 核心数据不变量 | 正常数据按 `(salary_month, department_id)` 唯一；单一部门明细可回填；无法唯一归属的历史记录保留空部门并显示“历史全公司”；工资明细部门快照不随员工调岗改变 |
| 回滚或恢复考虑 | V25 包含约束和数据回填，不执行自动降级；生产应用前应备份数据库并先在副本验证，失败时恢复备份 |

## 结果与评审

| 字段 | 记录 |
| --- | --- |
| 总体结果 | 本次代码合入所需自动化门禁全部通过；不代表人工 UAT 或上线批准 |
| 缺陷、偏差或后续 | pnpm/Node 运行环境下 Desktop 全量 28 项测试通过，原 Electron `text/plain` MIME 断言失败未复现；权限回归的登录请求显式指定 `Accept-Language: zh-CN`，消除停用账号提示对宿主机语言的依赖，完整后端复测通过；微信开发者工具、真机及人工业务验收仍按发布质量门执行，本记录不代表上线批准 |
| 复测提交和结果 | 本文件所在提交：Backend 131/131（无跳过），部门工资集成场景 1/1、V0/V12 至 V25 升级和 JaCoCo 报告通过；Desktop install/ignored-builds/lint/typecheck/test/build:renderer 全部通过（28/28），Miniapp install/ignored-builds/check/build:h5 全部通过，Legacy Miniapp check 全部通过（19/19），文档及 diff 检查通过；`run.py` 语法和帮助入口检查通过 |
| 评审人 | 待评审 |

本记录只引用合成测试数据和自动化结果，不包含账号凭据、个人信息、生产数据或未脱敏工资金额。
