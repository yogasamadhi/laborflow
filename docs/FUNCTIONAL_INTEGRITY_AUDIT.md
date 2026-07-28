# LaborFlow 功能完整性存量数据审计

## 1. 用途

`V23__functional_integrity_guards.sql` 会修复可安全重算的草稿工资金额，并阻止新的工资公式
错误和跨项目施工段关联。它不会改写已提交、已审核、已发放工资或已确认项目结算。

上线复审前必须在生产备份或非生产克隆上执行
[`scripts/ops/audit-functional-integrity.sql`](../scripts/ops/audit-functional-integrity.sql)，
核对修复前可能产生的存量异常。脚本只包含 `SELECT`，输出技术 ID、状态、日期及金额，不输出
姓名、手机号、账号、openid、附件内容或访问凭据。

## 2. 执行

```bash
psql "$LABORFLOW_AUDIT_DATABASE_URL" \
  --set ON_ERROR_STOP=1 \
  --file scripts/ops/audit-functional-integrity.sql
```

不得把连接串、密码、token 或未脱敏输出提交到仓库。执行结果应保存到受限的 Gate 证据位置，
并记录数据库备份标识、代码版本、执行时间和复核人。

## 3. 处理原则

- 草稿工资可以由财务在 Web 中修正或重新生成；修正后重新执行脚本。
- “旧工资可能使用未来档案”查询对 V23 之前没有档案 ID 快照的记录只输出候选项，不等同
  于已确认异常；财务需对照固定工资来源和档案明细复核。
- `pending_approval`、`approved`、`paid` 工资异常必须停止后续发放，由财务和 Gate 负责人
  核对后通过单独、经审批的纠正脚本处理。
- 已确认奖金池、日报产值、班组结算或跨项目资料异常必须由项目、财务和 Gate 负责人共同
  确认归属及正确金额；不得根据脚本结果自动覆盖。
- 每项异常必须关联 `LAUNCH_ISSUES.md` 中的缺陷编号、修复或纠正证据以及复测结果。
- 只有所有 P0/P1 存量异常关闭，且真实角色 UAT 完成后，才能重新评审上线 Gate。
