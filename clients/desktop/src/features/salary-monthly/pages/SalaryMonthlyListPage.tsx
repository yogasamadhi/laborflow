import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Download, FileSpreadsheet, Plus, RefreshCw, RotateCcw, Send, Trash2, Wallet, XCircle } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { ActionConfirmationModal } from "../../../components/common/ActionConfirmationModal";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { getAllPages, postData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";
import { canRunMonthlyAction, getMonthlyActionCopy, type MonthlyAction } from "../salaryWorkflow";
import type { Department } from "../../department/types";
import { groupMonthliesByMonth } from "../departmentalPayroll";

type Monthly = {
  id: string;
  salaryMonth: string;
  departmentId?: string;
  departmentName?: string;
  title: string;
  totalEmployeeCount: number;
  totalPayableAmount: number;
  totalDeductionAmount: number;
  totalActualAmount: number;
  status: "draft" | "pending_approval" | "approved" | "paid" | "archived" | string;
  approvedAt?: string;
  paidAt?: string;
  deleted?: number;
  includeFixedSalary?: number;
  includeAttendanceDeduction?: number;
  includePerformanceSalary?: number;
  includeProjectBonus?: number;
};

type PendingAction = {
  monthly: Monthly;
  action: MonthlyAction;
  remark: string;
};

const statusClass: Record<string, string> = {
  draft: "badge-yellow",
  pending_approval: "badge-yellow",
  approved: "badge-green",
  paid: "badge-green",
  archived: "badge-red"
};

function ruleSummary(monthly: Monthly) {
  const rules = [
    monthly.includeFixedSalary !== 0 ? "固定" : "",
    monthly.includeAttendanceDeduction !== 0 ? "考勤" : "",
    monthly.includePerformanceSalary !== 0 ? "绩效" : "",
    monthly.includeProjectBonus !== 0 ? "奖金" : ""
  ].filter(Boolean);
  return rules.length > 0 ? rules.join(" / ") : "未启用";
}

export function SalaryMonthlyListPage() {
  const queryClient = useQueryClient();
  const [dataScope, setDataScope] = useState<"0" | "1" | "all">("0");
  const [recycleOpen, setRecycleOpen] = useState(false);
  const [monthFilter, setMonthFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [departmentFilter, setDepartmentFilter] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null);
  const [confirmationError, setConfirmationError] = useState("");
  const monthlyQuery = usePagedQuery<Monthly>(["salary-monthly", dataScope], "/salary/monthly", {
    deleted: dataScope === "all" ? -1 : Number(dataScope),
    salaryMonth: monthFilter || undefined,
    status: statusFilter || undefined,
    departmentId: departmentFilter || undefined
  });
  const { data: departmentsData } = useQuery({
    queryKey: ["departments", "salary-monthly-list"],
    queryFn: () => getAllPages<Department>("/reference/departments")
  });
  const departments = departmentsData ?? [];
  const recycleQuery = useQuery({
    queryKey: ["salary-monthly", "recycle-bin"],
    queryFn: () => getAllPages<Monthly>("/salary/monthly", { deleted: 1 }),
    enabled: recycleOpen
  });
  const rows = monthlyQuery.data?.records ?? [];
  const visibleRows = rows;
  const groupedRows = groupMonthliesByMonth(visibleRows);
  const totals = visibleRows.reduce(
    (sum, row) => ({
      employeeCount: sum.employeeCount + Number(row.totalEmployeeCount ?? 0),
      payable: sum.payable + Number(row.totalPayableAmount ?? 0),
      deduction: sum.deduction + Number(row.totalDeductionAmount ?? 0),
      actual: sum.actual + Number(row.totalActualAmount ?? 0)
    }),
    { employeeCount: 0, payable: 0, deduction: 0, actual: 0 }
  );

  const action = useMutation({
    mutationFn: ({ id, action: operation, remark }: { id: string; action: MonthlyAction; remark?: string }) => {
      const payload = operation === "approve" || operation === "reject" ? { remark: remark?.trim() || undefined } : undefined;
      return postData<Monthly | null>(`/salary/monthly/${id}/${operation}`, payload);
    },
    onSuccess: async (monthly, variables) => {
      const copy = getMonthlyActionCopy(variables.action);
      setError("");
      setConfirmationError("");
      setSuccess(variables.action === "generate" && monthly
        ? `生成完成：${monthly.totalEmployeeCount ?? 0} 人，实发合计 ${money(monthly.totalActualAmount)}。`
        : copy.success);
      setPendingAction(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => {
      setSuccess("");
      setConfirmationError(reason instanceof Error ? reason.message : "操作失败，请稍后重试。");
    }
  });

  function openAction(monthly: Monthly, operation: MonthlyAction) {
    setConfirmationError("");
    setPendingAction({ monthly, action: operation, remark: "" });
  }

  function closeAction() {
    if (!action.isPending) setPendingAction(null);
  }

  function confirmAction() {
    if (!pendingAction) return;
    const copy = getMonthlyActionCopy(pendingAction.action);
    if (copy.needsRemark && !pendingAction.remark.trim()) {
      setConfirmationError("请填写驳回原因，便于工资表回到草稿后继续处理。");
      return;
    }
    action.mutate({ id: pendingAction.monthly.id, action: pendingAction.action, remark: pendingAction.remark });
  }

  const confirmationCopy = pendingAction ? getMonthlyActionCopy(pendingAction.action) : null;

  return (
    <Page
      title="月度工资表"
      description="工资表按月份分区，各直属部门独立生成、审核和发放。"
      actions={
        <div className="table-actions">
          <Button type="button" variant="secondary" onClick={() => setRecycleOpen(true)}>
            <Trash2 size={16} />
            回收站
          </Button>
          <Link to="/salary/monthly/create">
            <Button>
              <Plus size={16} />
              创建部门工资表
            </Button>
          </Link>
        </div>
      }
    >
      {error ? <div className="form-error page-message" role="alert">{error}</div> : null}
      {success ? <div className="form-success page-message" role="status">{success}</div> : null}
      {monthlyQuery.isPending ? <RouteLoadingState /> : null}
      {monthlyQuery.error ? <DataErrorState message={monthlyQuery.error instanceof Error ? monthlyQuery.error.message : undefined} onRetry={() => void monthlyQuery.refetch()} /> : null}
      {!monthlyQuery.isPending && !monthlyQuery.error ? (
        <>
          <div className="grid grid-3">
            <div className="card"><div className="muted">工资表</div><h3>{visibleRows.length}</h3></div>
            <div className="card"><div className="muted">覆盖人数</div><h3>{totals.employeeCount}</h3></div>
            <div className="card"><div className="muted">实发合计</div><h3>{money(totals.actual)}</h3></div>
          </div>

          <div className="panel toolbar monthly-toolbar">
            <Field label="月份">
              <TextInput type="month" value={monthFilter} onChange={(event) => setMonthFilter(event.target.value)} />
            </Field>
            <Field label="状态">
              <SelectInput value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="">全部</option>
                <option value="draft">草稿</option>
                <option value="pending_approval">待审核</option>
                <option value="approved">已审核</option>
                <option value="paid">已发放</option>
              </SelectInput>
            </Field>
            <Field label="数据范围">
              <SelectInput value={dataScope} onChange={(event) => setDataScope(event.target.value as "0" | "1" | "all")}>
                <option value="0">正常</option>
                <option value="1">回收站</option>
                <option value="all">全部</option>
              </SelectInput>
            </Field>
            <Field label="部门">
              <SelectInput value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)}>
                <option value="">全部部门</option>
                {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
              </SelectInput>
            </Field>
            <Button type="button" variant="secondary" onClick={() => { setMonthFilter(""); setStatusFilter(""); setDepartmentFilter(""); setDataScope("0"); }}>
              <RefreshCw size={16} />
              重置
            </Button>
          </div>

          <div className="panel table-wrap">
            {visibleRows.length === 0 ? (
              <div className="empty-state">
                {rows.length === 0 ? "还没有工资月份。创建工资月份后，可在此生成、审核和发放。" : "没有符合当前筛选条件的工资表。"}
              </div>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>月份</th>
                    <th>标题</th>
                    <th>部门</th>
                    <th>规则</th>
                    <th>人数</th>
                    <th>应发</th>
                    <th>扣减</th>
                    <th>实发</th>
                    <th>状态</th>
                    <th>数据状态</th>
                    <th>审核时间</th>
                    <th>发放时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {groupedRows.flatMap((group) => {
                    return [
                    <tr key={`month-${group.salaryMonth}`}>
                      <td colSpan={13}><div className="section-heading"><strong>{group.salaryMonth}</strong><span className="muted">{group.records.length} 个部门 · {group.employeeCount} 人 · 实发 {money(group.actualAmount)}</span></div></td>
                    </tr>,
                    ...group.records.map((monthly) => (
                    <tr key={monthly.id}>
                      <td>{monthly.salaryMonth}</td>
                      <td><Link to={`/salary/monthly/${monthly.id}`}>{monthly.title}</Link></td>
                      <td>{monthly.departmentName ?? "历史全公司"}</td>
                      <td>{ruleSummary(monthly)}</td>
                      <td>{monthly.totalEmployeeCount ?? 0}</td>
                      <td>{money(monthly.totalPayableAmount)}</td>
                      <td>{money(monthly.totalDeductionAmount)}</td>
                      <td>{money(monthly.totalActualAmount)}</td>
                      <td><span className={`badge ${statusClass[monthly.status] ?? "badge-yellow"}`}>{statusText(monthly.status)}</span></td>
                      <td><span className={monthly.deleted === 1 ? "badge badge-red" : "badge badge-green"}>{monthly.deleted === 1 ? "已删除" : "正常"}</span></td>
                      <td>{monthly.approvedAt ? monthly.approvedAt.slice(0, 10) : "-"}</td>
                      <td>{monthly.paidAt ? monthly.paidAt.slice(0, 10) : "-"}</td>
                      <td>
                        <div className="table-actions">
                          <Link to={`/salary/monthly/${monthly.id}`}><Button variant="secondary"><FileSpreadsheet size={16} />明细</Button></Link>
                          {monthly.deleted === 1 ? (
                            <Button variant="secondary" onClick={() => openAction(monthly, "restore")} disabled={action.isPending}><RotateCcw size={16} />恢复</Button>
                          ) : (
                            <>
                              {canRunMonthlyAction(monthly, "generate") ? <Button variant="secondary" onClick={() => openAction(monthly, "generate")} disabled={action.isPending}><RefreshCw size={16} />生成</Button> : null}
                              {canRunMonthlyAction(monthly, "submit") ? <Button variant="secondary" onClick={() => openAction(monthly, "submit")} disabled={action.isPending}><Send size={16} />提交</Button> : null}
                              {canRunMonthlyAction(monthly, "approve") ? <Button onClick={() => openAction(monthly, "approve")} disabled={action.isPending}><CheckCircle size={16} />通过</Button> : null}
                              {canRunMonthlyAction(monthly, "reject") ? <Button variant="danger" onClick={() => openAction(monthly, "reject")} disabled={action.isPending}><XCircle size={16} />驳回</Button> : null}
                              {(monthly.status === "approved" || monthly.status === "paid") ? <Link to={`/salary/monthly/${monthly.id}/payment`}><Button><Wallet size={16} />发放</Button></Link> : null}
                              <Link to={`/salary/monthly/${monthly.id}/export`}><Button variant="secondary"><Download size={16} />导出</Button></Link>
                              <Link to={`/salary/monthly/${monthly.id}/payslip`}><Button variant="secondary">工资条</Button></Link>
                              {canRunMonthlyAction(monthly, "delete") ? <Button variant="danger" onClick={() => openAction(monthly, "delete")} disabled={action.isPending}><Trash2 size={16} />删除</Button> : null}
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                    ))];
                  })}
                </tbody>
              </table>
            )}
          </div>
          {monthlyQuery.pagination}
        </>
      ) : null}

      <Modal open={recycleOpen} title="月度工资回收站" onClose={() => setRecycleOpen(false)}>
        {recycleQuery.isPending ? <RouteLoadingState /> : null}
        {recycleQuery.error ? <DataErrorState message={recycleQuery.error instanceof Error ? recycleQuery.error.message : undefined} onRetry={() => void recycleQuery.refetch()} /> : null}
        {!recycleQuery.isPending && !recycleQuery.error ? (
          (recycleQuery.data ?? []).length === 0 ? (
            <div className="empty-state">回收站里还没有已删除的工资表。</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>月份</th><th>标题</th><th>人数</th><th>实发</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>{(recycleQuery.data ?? []).map((monthly) => (
                  <tr key={monthly.id}>
                    <td>{monthly.salaryMonth}</td>
                    <td>{monthly.title}</td>
                    <td>{monthly.totalEmployeeCount ?? 0}</td>
                    <td>{money(monthly.totalActualAmount)}</td>
                    <td><span className={`badge ${statusClass[monthly.status] ?? "badge-yellow"}`}>{statusText(monthly.status)}</span></td>
                    <td><Button type="button" variant="secondary" onClick={() => openAction(monthly, "restore")} disabled={action.isPending}><RotateCcw size={16} />恢复</Button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )
        ) : null}
      </Modal>

      {pendingAction && confirmationCopy ? (
        <ActionConfirmationModal
          open
          title={confirmationCopy.title}
          description={confirmationCopy.description}
          confirmLabel={confirmationCopy.label}
          tone={confirmationCopy.tone}
          pending={action.isPending}
          onClose={closeAction}
          onConfirm={confirmAction}
          summary={<><strong>{pendingAction.monthly.title}</strong><span>{pendingAction.monthly.salaryMonth} · {statusText(pendingAction.monthly.status)}</span><span>实发合计：{money(pendingAction.monthly.totalActualAmount)}</span></>}
        >
          {confirmationCopy.needsRemark ? (
            <Field label="驳回原因">
              <TextArea value={pendingAction.remark} onChange={(event) => setPendingAction({ ...pendingAction, remark: event.target.value })} placeholder="说明需要复核或调整的内容" required />
            </Field>
          ) : null}
          {confirmationError ? <div className="form-error" role="alert">{confirmationError}</div> : null}
        </ActionConfirmationModal>
      ) : null}
    </Page>
  );
}
