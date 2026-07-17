# 技术债：ProjectManagementServiceImpl 上帝类拆分

## 状态
**推迟执行（已评估，方案明确，待专项排期）**

本文档记录 L2 改进计划 Phase B 中 #6 项的完整结构分析与拆分方案。
当前实现可正常工作且被集成测试覆盖（`CoreApiContractIntegrationTest`、
`ProjectSettlementLifecycleIntegrationTest` 等），无功能驱动因素，故不在
本轮改动中做高风险的机械拆分。后续启动时按本文档执行即可。

## 现状
- 文件：`server/src/main/java/com/company/labor/module/project/service/impl/ProjectManagementServiceImpl.java`
- 规模：1728 行，~60 个 public 方法，21 个注入的 mapper/service 字段
- 唯一调用方：`ProjectManagementController`（1:1 透传全部方法）+ `CompensationServiceImpl`（仅用 `dashboard(...)`）
- 接口 `ProjectManagementService` 是公共契约，**必须原样保留**

## 聚合划分（15 个）
| 聚合 | 方法数 | 关键耦合 |
|---|---|---|
| project | 7 | softDeleteGuard（19 张引用表）|
| section | 5 | requireProject（跨）|
| work_process | 5 | softDeleteGuard |
| team | 5 | softDeleteGuard |
| team_unit_price | 3 | requireProject/Team/WorkProcess |
| project_process_price | 3 | requireProject/WorkProcess |
| contract_item | 4 | normalizeContractItem 跨 project/section/work_process |
| daily_work_report | 11 | 定价读 contract_item + process_price（跨）|
| settlement | 5 | generateTeamSettlement 读 6 个聚合（最重）|
| variation_order | 10 | requireProject + fileBindingGuard |
| cost_record | 3 | requireProject |
| payment | 3 | requireProject + fileBindingGuard |
| machinery | 6 | normalizeMachineryWorkRecord 跨 project/section/equipment |
| material | 3 | normalizeMaterialRecord 跨 project/section |
| document_archive | 3 | requireProject/Section?/WorkProcess? + fileBindingGuard |
| dashboard | 1 | 读 9 个 mapper（纯读模型，CompensationServiceImpl 也依赖）|

## 拆分方案（facade 模式，零 API 变更）

1. **新建 `ProjectDomainSupport`（共享组件）**：承载跨聚合的 `require*` 查找族
   （`requireProject`/`requireSection`/`requireWorkProcess`/`requireTeam`/
   `requireMachineryEquipment`）+ 原语（`nvl`/`defaultText`/`requireText`/
   `deleted`/`percent`/`financeBreakdown`/`remark`）+ 两个 `fillOperator` +
   `auditUserProvider`。所有子服务注入它，避免循环依赖。

2. **按聚合新建 `@Service`**（建议顺序，从简到繁）：
   - `ProjectService`、`SectionService`、`WorkProcessService`、`TeamService`
     （各自拥有自己的 `require*` + `ensure*NameAvailable`）
   - `TeamUnitPriceService`、`ProjectProcessPriceService`、`ContractItemService`
     （依赖上面 4 个的 require*）
   - `DailyWorkReportService`（依赖 ContractItemService/ProjectProcessPriceService 的 `latest*` 读）
   - `SettlementService`（依赖 DailyWorkReportService、TeamUnitPriceService、
     WorkProcessService、ProjectService、TeamService —— 最重，最后做）
   - `VariationOrderService`、`CostRecordService`、`PaymentService`、
     `MachineryService`、`MaterialService`、`DocumentArchiveService`
     （各依赖 ProjectService / Section 等）
   - `DashboardService`（读模型聚合器，CompensationServiceImpl 改注入它）

3. **`ProjectManagementServiceImpl` 降为 facade**：保留为 `@Service`，
   注入所有子服务，每个 `@Override` 方法委托给对应子服务。可逐步标
   `@Deprecated` 鼓励 controller 直接注入子服务，但**不强制**（接口契约不变）。

4. **共享依赖注入策略**：`operationLogService`、`softDeleteGuard`、
   `fileBindingGuard` 已是外部 bean，需要的子服务直接注入。
   `BusinessStateMachine` 是静态调用，无需注入。
   `AccessControlSupport` **不在 impl 内**（仅 controller 层），保持不变。

## 执行风险与验证
- **风险**：跨聚合方法（settlement/daily_report/dashboard）的依赖链最易引入循环注入。
  先做 `ProjectDomainSupport` 可消解大部分。
- **验证门槛**（必须全绿才合并）：
  - `mvn test`（含 Testcontainers 集成测试，尤其 `ProjectSettlementLifecycleTest`、
    `CoreApiContractIntegrationTest`、`VariationIncomeLifecycleIntegrationTest`）
  - `RequestDtoBoundaryTest`（DTO 契约守卫）
  - `BusinessStateMachineTest`（状态机不变）
- 建议在独立分支、单独 PR 完成，不与其它改动混在一起，便于 review 与回滚。

## 补测试缺口
中心 CRUD（Section/WorkProcess/Team）目前缺单测，拆分时应顺带补齐。
