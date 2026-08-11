import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Download, Eye, RefreshCw, Save, Settings } from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ActionConfirmationModal } from "../../../components/common/ActionConfirmationModal";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile, getAllPages, getData, postData, putData } from "../../../lib/axios";
import { openExternalUrl } from "../../../lib/desktop";
import { money, statusText } from "../../../lib/utils";
import type { Employee } from "../../employee/types";
import { canRunMonthlyAction, getMonthlyActionCopy, monthlyWorkflowStage } from "../salaryWorkflow";

type Monthly = {
  id: string;
  salaryMonth: string;
  title: string;
  includeFixedSalary?: number;
  includeAttendanceDeduction?: number;
  includePerformanceSalary?: number;
  includeProjectBonus?: number;
  totalEmployeeCount: number;
  totalPayableAmount: number;
  totalDeductionAmount: number;
  totalActualAmount: number;
  status: string;
  remark?: string;
};

type RuleKey = "includeFixedSalary" | "includeAttendanceDeduction" | "includePerformanceSalary" | "includeProjectBonus";

type MonthlyRuleForm = Pick<Monthly, "salaryMonth" | "title" | "remark"> & Record<RuleKey, number>;

type Detail = {
  id: string;
  salaryMonthlyId: string;
  employeeId: string;
  departmentId?: string;
  positionId?: string;
  payableAmount: number;
  deductionAmount: number;
  actualAmount: number;
  paymentStatus: string;
  paymentDate?: string;
  paymentMethod?: string;
  paymentProofUrl?: string;
  remark?: string;
};

type DetailItem = {
  id: string;
  salaryDetailId: string;
  salaryItemName: string;
  salaryItemType: "earning" | "deduction";
  amount: number;
  remark?: string;
};

type SourceRecord = {
  id: string;
  salaryDetailId: string;
  employeeId: string;
  sourceType: string;
  sourceId?: string;
  sourceName?: string;
  amount: number;
  remark?: string;
  createdAt?: string;
};

type Profile = {
  id: string;
  status: string;
};

type AuditRecord = {
  id: string;
  salaryDetailId?: string;
  action: string;
  operatorName?: string;
  statusBefore?: string;
  statusAfter?: string;
  remark?: string;
  proofUrl?: string;
  createdAt?: string;
};

type PendingWorkflowAction =
  | { action: "generate"; remark: string }
  | { action: "submit" | "approve" | "reject"; remark: string };

type DetailUpdatePayload = Pick<Detail, "id" | "payableAmount" | "deductionAmount" | "remark">;

const badgeClass: Record<string, string> = {
  draft: "badge-yellow",
  pending_approval: "badge-yellow",
  approved: "badge-green",
  paid: "badge-green",
  unpaid: "badge-yellow"
};

const generationRules: Array<{ key: RuleKey; label: string }> = [
  { key: "includeFixedSalary", label: "固定工资" },
  { key: "includeAttendanceDeduction", label: "考勤扣款" },
  { key: "includePerformanceSalary", label: "绩效工资" },
  { key: "includeProjectBonus", label: "项目奖金" }
];

const ruleSummaryLabels: Record<RuleKey, string> = {
  includeFixedSalary: "固定",
  includeAttendanceDeduction: "考勤",
  includePerformanceSalary: "绩效",
  includeProjectBonus: "奖金"
};

export function SalaryMonthlyDetailPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [editing, setEditing] = useState<Detail | null>(null);
  const [editingMonthly, setEditingMonthly] = useState<MonthlyRuleForm | null>(null);
  const [viewing, setViewing] = useState<Detail | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [pendingWorkflowAction, setPendingWorkflowAction] = useState<PendingWorkflowAction | null>(null);
  const [confirmationError, setConfirmationError] = useState("");

  const monthlyQuery = useQuery({
    queryKey: ["salary-monthly", monthlyId],
    queryFn: () => getData<Monthly>(`/salary/monthly/${monthlyId}`)
  });
  const detailsQuery = useQuery({
    queryKey: ["salary-details", monthlyId],
    queryFn: () => getAllPages<Detail>(`/salary/monthly/${monthlyId}/details`)
  });
  const { data: employeesData } = useQuery({
    queryKey: ["employees", "salary-monthly-detail-options"],
    queryFn: () => getAllPages<Employee>("/reference/employees")
  });
  const { data: monthlyDetailItems } = useQuery({
    queryKey: ["salary-monthly-detail-items", monthlyId],
    queryFn: () => getData<DetailItem[]>(`/salary/monthly/${monthlyId}/detail-items`)
  });
  const { data: audits } = useQuery({
    queryKey: ["salary-audits", monthlyId],
    queryFn: () => getData<AuditRecord[]>(`/salary/monthly/${monthlyId}/audits`)
  });
  const { data: sourceRecords } = useQuery({
    queryKey: ["salary-sources", monthlyId],
    queryFn: () => getData<SourceRecord[]>(`/salary/monthly/${monthlyId}/sources`)
  });
  const { data: profilesData } = useQuery({
    queryKey: ["salary-profiles", "monthly-detail-diagnostics"],
    queryFn: () => getAllPages<Profile>("/salary/profiles")
  });

  const monthly = monthlyQuery.data;
  const detailsData = detailsQuery.data;
  const employees = employeesData ?? [];
  const employeeMap = useMemo(() => new Map(employees.map((employee) => [employee.id, employee])), [employees]);
  const details = detailsData ?? [];
  const detailItemMap = useMemo(() => {
    const map = new Map<string, DetailItem[]>();
    for (const item of monthlyDetailItems ?? []) {
      const rows = map.get(item.salaryDetailId) ?? [];
      rows.push(item);
      map.set(item.salaryDetailId, rows);
    }
    return map;
  }, [monthlyDetailItems]);
  const sourceMap = useMemo(() => {
    const map = new Map<string, SourceRecord[]>();
    for (const item of sourceRecords ?? []) {
      const rows = map.get(item.salaryDetailId) ?? [];
      rows.push(item);
      map.set(item.salaryDetailId, rows);
    }
    return map;
  }, [sourceRecords]);
  const visibleDetails = details.filter((detail) => {
    const employee = employeeMap.get(detail.employeeId);
    const matchesKeyword = !keyword || String(detail.employeeId).includes(keyword) || employee?.name?.includes(keyword);
    const matchesStatus = !paymentStatus || detail.paymentStatus === paymentStatus;
    return matchesKeyword && matchesStatus;
  });
  const paidCount = details.filter((detail) => detail.paymentStatus === "paid").length;
  const paidAmount = details.reduce((sum, detail) => sum + (detail.paymentStatus === "paid" ? Number(detail.actualAmount ?? 0) : 0), 0);
  const editable = monthly?.status === "draft";
  const activeProfileCount = (profilesData ?? []).filter((profile) => profile.status === "active").length;
  const coreError = monthlyQuery.error || detailsQuery.error;
  const isCorePending = monthlyQuery.isPending || detailsQuery.isPending;

  const generate = useMutation({
    mutationFn: () => postData<Monthly>(`/salary/monthly/${monthlyId}/generate`),
    onSuccess: async (result) => {
      setError("");
      setConfirmationError("");
      setPendingWorkflowAction(null);
      setSuccess(`重新生成完成：${result.totalEmployeeCount ?? 0} 人，实发合计 ${money(result.totalActualAmount)}。`);
      await queryClient.invalidateQueries({ queryKey: ["salary-details", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly-detail-items", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-sources", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => {
      setSuccess("");
      setConfirmationError(reason instanceof Error ? reason.message : "生成失败");
    }
  });
  const workflowAction = useMutation({
    mutationFn: (pending: Exclude<PendingWorkflowAction, { action: "generate" }>) => {
      const payload = pending.action === "approve" || pending.action === "reject" ? { remark: pending.remark.trim() || undefined } : undefined;
      return postData<Monthly | null>(`/salary/monthly/${monthlyId}/${pending.action}`, payload);
    },
    onSuccess: async (_result, pending) => {
      setError("");
      setConfirmationError("");
      setSuccess(getMonthlyActionCopy(pending.action).success);
      setPendingWorkflowAction(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
    },
    onError: (reason) => {
      setSuccess("");
      setConfirmationError(reason instanceof Error ? reason.message : "操作失败，请稍后重试。");
    }
  });
  const update = useMutation({
    mutationFn: (detail: DetailUpdatePayload) => putData<Detail>(`/salary/details/${detail.id}`, detail),
    onSuccess: async () => {
      setError("");
      setSuccess("工资明细已保存。");
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-details", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "保存失败")
  });
  const updateMonthly = useMutation({
    mutationFn: (payload: MonthlyRuleForm) => putData<Monthly>(`/salary/monthly/${monthlyId}`, payload),
    onSuccess: async () => {
      setError("");
      setSuccess("工资生成规则已保存，请重新生成明细后生效。");
      setEditingMonthly(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "规则保存失败")
  });

  function submitEdit(event: FormEvent) {
    event.preventDefault();
    if (editing) {
      update.mutate({
        id: editing.id,
        payableAmount: editing.payableAmount,
        deductionAmount: editing.deductionAmount,
        remark: editing.remark
      });
    }
  }

  function submitMonthlyRules(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!editingMonthly) return;
    if (!generationRules.some((rule) => editingMonthly[rule.key] === 1)) {
      setError("至少启用一项工资生成规则");
      return;
    }
    updateMonthly.mutate(editingMonthly);
  }

  function patchEditing(patch: Partial<Detail>) {
    setEditing((current) => current ? { ...current, ...patch } : current);
  }

  function patchMonthlyRule(key: RuleKey) {
    setEditingMonthly((current) => current ? { ...current, [key]: current[key] === 1 ? 0 : 1 } : current);
  }

  function openMonthlyRules() {
    if (!monthly) return;
    setEditingMonthly({
      salaryMonth: monthly.salaryMonth,
      title: monthly.title,
      remark: monthly.remark,
      includeFixedSalary: monthly.includeFixedSalary ?? 1,
      includeAttendanceDeduction: monthly.includeAttendanceDeduction ?? 1,
      includePerformanceSalary: monthly.includePerformanceSalary ?? 1,
      includeProjectBonus: monthly.includeProjectBonus ?? 1
    });
  }

  function openWorkflowAction(action: PendingWorkflowAction["action"]) {
    if (!monthly || !canRunMonthlyAction(monthly, action)) return;
    setConfirmationError("");
    setPendingWorkflowAction({ action, remark: "" });
  }

  function closeWorkflowAction() {
    if (!generate.isPending && !workflowAction.isPending) setPendingWorkflowAction(null);
  }

  function confirmWorkflowAction() {
    if (!pendingWorkflowAction) return;
    const copy = getMonthlyActionCopy(pendingWorkflowAction.action);
    if (copy.needsRemark && !pendingWorkflowAction.remark.trim()) {
      setConfirmationError("请填写驳回原因，便于后续复核。");
      return;
    }
    if (pendingWorkflowAction.action === "generate") {
      generate.mutate();
      return;
    }
    workflowAction.mutate(pendingWorkflowAction);
  }

  async function openProof(proofUrl: string) {
    setError("");
    try {
      const privatePath = proofUrl.match(/^\/(?:api\/)?files\/\d+\/download$/);
      if (privatePath) {
        const url = proofUrl.startsWith("/api/") ? proofUrl.slice(4) : proofUrl;
        await downloadFile(url, "工资凭证");
        return;
      }
      await openExternalUrl(proofUrl);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "凭证打开失败");
    }
  }

  const workflowCopy = pendingWorkflowAction ? getMonthlyActionCopy(pendingWorkflowAction.action) : null;

  return (
    <Page
      title={monthly?.title ?? "工资明细"}
      description="核对工资构成、审核证据与发放进度后再推进下一状态。"
      actions={
        <div className="table-actions">
          {monthly?.status === "draft" && <Button type="button" variant="secondary" onClick={openMonthlyRules}><Settings size={16} />规则设置</Button>}
          {monthly && canRunMonthlyAction(monthly, "generate") ? <Button type="button" variant="secondary" onClick={() => openWorkflowAction("generate")} disabled={generate.isPending}><RefreshCw size={16} />重新生成</Button> : null}
          {monthly && canRunMonthlyAction(monthly, "submit") ? <Button type="button" variant="secondary" onClick={() => openWorkflowAction("submit")} disabled={workflowAction.isPending}><CheckCircle size={16} />提交审核</Button> : null}
          {monthly && canRunMonthlyAction(monthly, "approve") ? <Button type="button" onClick={() => openWorkflowAction("approve")} disabled={workflowAction.isPending}><CheckCircle size={16} />审核通过</Button> : null}
          {monthly && canRunMonthlyAction(monthly, "reject") ? <Button type="button" variant="danger" onClick={() => openWorkflowAction("reject")} disabled={workflowAction.isPending}>驳回</Button> : null}
          {monthly && (monthly.status === "approved" || monthly.status === "paid") ? <Link to={`/salary/monthly/${monthly.id}/payment`}><Button>发放登记</Button></Link> : null}
          {monthly ? <Link to={`/salary/monthly/${monthly.id}/export`}><Button variant="secondary"><Download size={16} />导出</Button></Link> : null}
        </div>
      }
    >
      {isCorePending ? <RouteLoadingState /> : null}
      {coreError ? <DataErrorState message={coreError instanceof Error ? coreError.message : undefined} onRetry={() => { void monthlyQuery.refetch(); void detailsQuery.refetch(); }} /> : null}
      {!isCorePending && !coreError ? (
        <>
      {error && <div className="form-error page-message" role="alert">{error}</div>}
      {success && <div className="form-success page-message" role="status">{success}</div>}
      <div className="workflow-progress" aria-label={`工资流程：${statusText(monthly?.status)}`}>
        {["草稿", "审核", "发放"].map((label, index) => {
          const stage = monthlyWorkflowStage(monthly?.status);
          const state = index < stage ? "is-complete" : index === stage ? "is-current" : "";
          return <span className={state} key={label}>{label}</span>;
        })}
      </div>
      <div className="grid grid-3">
        <div className="card"><div className="muted">工资月份</div><h3>{monthly?.salaryMonth ?? "-"}</h3></div>
        <div className="card"><div className="muted">状态</div><h3>{statusText(monthly?.status)}</h3></div>
        <div className="card"><div className="muted">人数</div><h3>{monthly?.totalEmployeeCount ?? 0}</h3></div>
        <div className="card"><div className="muted">生成规则</div><h3>{ruleSummary(monthly)}</h3></div>
        <div className="card"><div className="muted">锁账状态</div><h3>{lockStatusText(monthly?.status)}</h3></div>
        <div className="card"><div className="muted">应发</div><h3>{money(monthly?.totalPayableAmount)}</h3></div>
        <div className="card"><div className="muted">扣减</div><h3>{money(monthly?.totalDeductionAmount)}</h3></div>
        <div className="card"><div className="muted">实发</div><h3>{money(monthly?.totalActualAmount)}</h3></div>
      </div>

      <div className="panel audit-panel">
        <div className="section-heading">
          <h3>审批证据链</h3>
          <span className="muted">{audits?.length ?? 0} 条记录</span>
        </div>
        {(audits ?? []).length === 0 ? (
          <div className="empty-state">暂无工资审批证据。</div>
        ) : (
          <div className="audit-timeline">
            {(audits ?? []).map((item) => (
              <div className="audit-row" key={item.id}>
                <div className="audit-time">{item.createdAt ? item.createdAt.replace("T", " ").slice(0, 16) : "-"}</div>
                <div className="audit-main">
                  <div className="audit-title">
                    <strong>{auditActionText(item.action)}</strong>
                    <span className="muted">{item.operatorName ?? "系统"}</span>
                    {item.salaryDetailId && <span className="badge badge-yellow">明细 {item.salaryDetailId}</span>}
                  </div>
                  <div className="audit-meta">
                    {(item.statusBefore || item.statusAfter) && <span>{statusText(item.statusBefore)} → {statusText(item.statusAfter)}</span>}
                    {item.remark && <span>{item.remark}</span>}
                    {item.proofUrl && (
                      <Button type="button" variant="secondary" onClick={() => void openProof(item.proofUrl!)}>
                        <Download size={14} />
                        查看凭证
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="panel toolbar monthly-toolbar">
        <Field label="员工">
          <TextInput value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="姓名或 ID" />
        </Field>
        <Field label="发放状态">
          <SelectInput value={paymentStatus} onChange={(event) => setPaymentStatus(event.target.value)}>
            <option value="">全部</option>
            <option value="unpaid">未发放</option>
            <option value="paid">已发放</option>
          </SelectInput>
        </Field>
        <div className="summary-strip compact-summary">
          <span>已发：{paidCount}/{details.length}</span>
          <strong>{money(paidAmount)}</strong>
        </div>
      </div>

      <div className="panel table-wrap">
        {details.length === 0 ? (
          <div className="empty-state">
            当前工资表还没有工资明细。系统检测到 {activeProfileCount} 个启用工资档案。
            {monthly?.status === "draft" ? " 请点击页面右上角“重新生成”同步工资档案。" : " 当前工资表不是草稿状态，不能重新生成。"}
          </div>
        ) : visibleDetails.length === 0 ? (
          <div className="empty-state">没有符合当前员工或发放状态筛选条件的工资明细。</div>
        ) : (
        <table>
          <thead>
            <tr>
              <th>员工</th>
              <th>工资项组成</th>
              <th>应发</th>
              <th>扣款</th>
              <th>实发</th>
              <th>发放状态</th>
              <th>发放日期</th>
              <th>发放方式</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {visibleDetails.map((detail) => {
              const employee = employeeMap.get(detail.employeeId);
              const items = detailItemMap.get(detail.id) ?? [];
              return (
                <tr key={detail.id}>
                  <td>{employee ? `${employee.name} / ${employee.id}` : detail.employeeId}</td>
                  <td>
                    {items.length > 0 ? (
                      <div className="item-chips">
                        {items.slice(0, 3).map((item) => (
                          <span className={item.salaryItemType === "deduction" ? "badge badge-yellow" : "badge badge-green"} key={item.id}>
                            {item.salaryItemName} {money(item.amount)}
                          </span>
                        ))}
                        {items.length > 3 && <span className="muted">+{items.length - 3}</span>}
                      </div>
                    ) : (
                      <span className="muted">未生成工资项</span>
                    )}
                  </td>
                  <td>{money(detail.payableAmount)}</td>
                  <td>{money(detail.deductionAmount)}</td>
                  <td>{money(detail.actualAmount)}</td>
                  <td><span className={`badge ${badgeClass[detail.paymentStatus] ?? "badge-yellow"}`}>{statusText(detail.paymentStatus)}</span></td>
                  <td>{detail.paymentDate ?? "-"}</td>
                  <td>{detail.paymentMethod ?? "-"}</td>
                  <td>{detail.remark ?? "-"}</td>
                  <td>
                    <div className="table-actions">
                      <Button type="button" variant="secondary" onClick={() => setViewing(detail)}><Eye size={16} />组成</Button>
                      <Button type="button" variant="secondary" onClick={() => setEditing(detail)} disabled={!editable || detail.paymentStatus === "paid"}>编辑</Button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
        )}
      </div>

      <Modal
        open={Boolean(editing)}
        title="编辑工资明细"
        onClose={() => setEditing(null)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setEditing(null)}>取消</Button>
            <Button type="submit" form="salary-detail-edit-form" disabled={update.isPending}><Save size={16} />保存</Button>
          </>
        }
      >
        {editing && (
          <form id="salary-detail-edit-form" className="grid" onSubmit={submitEdit}>
            <div className="grid grid-3">
              <Field label="应发金额">
                <TextInput type="number" step="0.01" value={editing.payableAmount} onChange={(event) => patchEditing({ payableAmount: Number(event.target.value) })} />
              </Field>
              <Field label="扣减金额">
                <TextInput type="number" step="0.01" value={editing.deductionAmount} onChange={(event) => patchEditing({ deductionAmount: Number(event.target.value) })} />
              </Field>
              <Field label="实发金额">
                <TextInput
                  type="number"
                  step="0.01"
                  value={Number(editing.payableAmount ?? 0) - Number(editing.deductionAmount ?? 0)}
                  readOnly
                />
              </Field>
            </div>
            <div className="muted">实发金额由系统按“应发金额 - 扣减金额”计算。</div>
            <Field label="备注">
              <TextArea value={editing.remark ?? ""} onChange={(event) => patchEditing({ remark: event.target.value })} />
            </Field>
          </form>
        )}
      </Modal>

      <Modal
        open={Boolean(editingMonthly)}
        title="工资生成规则"
        onClose={() => setEditingMonthly(null)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setEditingMonthly(null)}>取消</Button>
            <Button type="submit" form="salary-monthly-rule-form" disabled={updateMonthly.isPending}><Save size={16} />保存</Button>
          </>
        }
      >
        {editingMonthly && (
          <form id="salary-monthly-rule-form" className="grid" onSubmit={submitMonthlyRules}>
            <div className="summary-strip">
              <span>{editingMonthly.salaryMonth}</span>
              <strong>{editingMonthly.title}</strong>
            </div>
            <Field label="生成规则">
              <div className="checkbox-chips">
                {generationRules.map((rule) => (
                  <label className="checkbox-chip" key={rule.key}>
                    <input type="checkbox" checked={editingMonthly[rule.key] === 1} onChange={() => patchMonthlyRule(rule.key)} />
                    {rule.label}
                  </label>
                ))}
              </div>
            </Field>
          </form>
        )}
      </Modal>

      <Modal open={Boolean(viewing)} title="工资项组成" onClose={() => setViewing(null)}>
        {viewing && (detailItemMap.get(viewing.id) ?? []).length === 0 && (
          <div className="empty-state">这条工资明细没有工资项快照。请确认生成工资表前该员工已有启用工资档案，并在草稿状态重新生成。</div>
        )}
        {viewing && (sourceMap.get(viewing.id) ?? []).length === 0 && (
          <div className="empty-state">暂无来源快照。重新生成后系统会记录固定工资、考勤、绩效和项目奖金来源。</div>
        )}
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>类型</th><th>工资项</th><th>金额</th><th>备注</th></tr>
            </thead>
            <tbody>
              {(viewing ? detailItemMap.get(viewing.id) ?? [] : []).map((item) => (
                <tr key={item.id}>
                  <td>{item.salaryItemType === "deduction" ? "扣减项" : "应发项"}</td>
                  <td>{item.salaryItemName}</td>
                  <td>{money(item.amount)}</td>
                  <td>{item.remark ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {(viewing ? sourceMap.get(viewing.id) ?? [] : []).length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>来源</th><th>来源名称</th><th>金额</th><th>备注</th></tr>
              </thead>
              <tbody>
                {(viewing ? sourceMap.get(viewing.id) ?? [] : []).map((item) => (
                  <tr key={item.id}>
                    <td>{sourceTypeText(item.sourceType)}</td>
                    <td>{item.sourceName ?? item.sourceId ?? "-"}</td>
                    <td>{money(item.amount)}</td>
                    <td>{item.remark ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>
      {pendingWorkflowAction && workflowCopy ? (
        <ActionConfirmationModal
          open
          title={workflowCopy.title}
          description={workflowCopy.description}
          confirmLabel={workflowCopy.label}
          tone={workflowCopy.tone}
          pending={generate.isPending || workflowAction.isPending}
          onClose={closeWorkflowAction}
          onConfirm={confirmWorkflowAction}
          summary={<><strong>{monthly?.title}</strong><span>{monthly?.salaryMonth} · {statusText(monthly?.status)}</span><span>实发合计：{money(monthly?.totalActualAmount)}</span></>}
        >
          {workflowCopy.needsRemark ? (
            <Field label="驳回原因">
              <TextArea value={pendingWorkflowAction.remark} onChange={(event) => setPendingWorkflowAction({ ...pendingWorkflowAction, remark: event.target.value })} placeholder="说明需要复核或调整的内容" required />
            </Field>
          ) : null}
          {confirmationError ? <div className="form-error" role="alert">{confirmationError}</div> : null}
        </ActionConfirmationModal>
      ) : null}
        </>
      ) : null}
    </Page>
  );
}

function ruleSummary(monthly?: Monthly) {
  if (!monthly) return "-";
  const rules = generationRules.map((rule) => ({ enabled: monthly[rule.key] !== 0, label: ruleSummaryLabels[rule.key] }));
  const enabled = rules.filter((item) => item.enabled).map((item) => item.label);
  return enabled.length > 0 ? enabled.join(" / ") : "未启用";
}

function sourceTypeText(type: string) {
  const map: Record<string, string> = {
    fixed_salary: "固定工资",
    attendance_deduction: "考勤扣款",
    performance_salary: "绩效工资",
    project_bonus: "项目奖金",
    salary_deduction: "工资扣款"
  };
  return map[type] ?? type;
}

function lockStatusText(status?: string) {
  if (!status) return "-";
  if (status === "draft") return "未锁账";
  if (status === "pending_approval") return "已提交";
  if (status === "approved") return "已锁定";
  if (status === "paid") return "已发放";
  return statusText(status);
}

function auditActionText(action: string) {
  const map: Record<string, string> = {
    create_monthly: "创建工资表",
    update_monthly: "修改工资表",
    generate_monthly: "生成工资",
    submit_approval: "提交审核",
    approve: "审核通过",
    reject: "审核驳回",
    update_detail: "修改明细",
    single_payment: "单人发放",
    batch_payment: "批量发放",
    upload_proof: "上传凭证"
  };
  return map[action] ?? action;
}
