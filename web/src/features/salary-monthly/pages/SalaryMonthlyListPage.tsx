import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Download, FileSpreadsheet, Plus, RefreshCw, RotateCcw, Send, Trash2, Wallet, XCircle } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";

type Monthly = {
  id: string;
  salaryMonth: string;
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
  const { data } = useQuery({
    queryKey: ["salary-monthly", dataScope],
    queryFn: () => getData<PageResult<Monthly>>("/salary/monthly", { page: 1, pageSize: 100, deleted: dataScope === "all" ? -1 : Number(dataScope) })
  });
  const { data: recycleData } = useQuery({
    queryKey: ["salary-monthly", "recycle-bin"],
    queryFn: () => getData<PageResult<Monthly>>("/salary/monthly", { page: 1, pageSize: 100, deleted: 1 }),
    enabled: recycleOpen
  });
  const [monthFilter, setMonthFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const rows = data?.records ?? [];
  const visibleRows = rows.filter((row) => {
    return (!monthFilter || row.salaryMonth.includes(monthFilter)) && (!statusFilter || row.status === statusFilter);
  });
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
    mutationFn: ({ id, op }: { id: string; op: string }) => postData<Monthly | null>(`/salary/monthly/${id}/${op}`),
    onSuccess: async (monthly, variables) => {
      setError("");
      if (variables.op === "generate") {
        setSuccess(monthly ? `生成完成：${monthly.totalEmployeeCount ?? 0} 人，实发合计 ${money(monthly.totalActualAmount)}。` : "生成完成，列表已刷新。");
      } else if (variables.op === "restore") {
        setSuccess("工资表已恢复。");
      } else if (variables.op === "delete") {
        setSuccess("工资表已移入回收站。");
      } else {
        setSuccess("操作成功，列表已刷新。");
      }
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
      await queryClient.refetchQueries({ queryKey: ["salary-monthly", dataScope] });
    },
    onError: (reason) => {
      setSuccess("");
      setError(reason instanceof Error ? reason.message : "操作失败");
    }
  });

  function can(row: Monthly, op: "generate" | "submit" | "approve" | "reject" | "payment") {
    if (row.deleted === 1) return false;
    if (op === "generate") return row.status === "draft";
    if (op === "submit") return row.status === "draft" && Number(row.totalEmployeeCount ?? 0) > 0;
    if (op === "approve" || op === "reject") return row.status === "pending_approval";
    if (op === "payment") return row.status === "approved" || row.status === "paid";
    return false;
  }

  function deleteMonthly(id: string, title: string) {
    if (window.confirm(`确认删除“${title}”？删除后可在回收站恢复。`)) {
      action.mutate({ id, op: "delete" });
    }
  }

  return (
    <Page
      title="月度工资表"
      actions={
        <div className="table-actions">
          <Button type="button" variant="secondary" onClick={() => setRecycleOpen(true)}>
            <Trash2 size={16} />
            回收站
          </Button>
          <Link to="/salary/monthly/create">
            <Button>
              <Plus size={16} />
              创建工资月份
            </Button>
          </Link>
        </div>
      }
    >
      {error && <div className="form-error page-message">{error}</div>}
      {success && <div className="form-success page-message">{success}</div>}
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
        <Button type="button" variant="secondary" onClick={() => { setMonthFilter(""); setStatusFilter(""); setDataScope("0"); }}>
          <RefreshCw size={16} />
          重置
        </Button>
      </div>

      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>月份</th>
              <th>标题</th>
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
            {visibleRows.map((monthly) => (
              <tr key={monthly.id}>
                <td>{monthly.salaryMonth}</td>
                <td><Link to={`/salary/monthly/${monthly.id}`}>{monthly.title}</Link></td>
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
                      <Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "restore" })} disabled={action.isPending}><RotateCcw size={16} />恢复</Button>
                    ) : (
                      <>
                        {can(monthly, "generate") && <Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "generate" })} disabled={action.isPending}><RefreshCw size={16} />{action.isPending && action.variables?.id === monthly.id && action.variables?.op === "generate" ? "生成中" : "生成"}</Button>}
                        {can(monthly, "submit") && <Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "submit" })} disabled={action.isPending}><Send size={16} />提交</Button>}
                        {can(monthly, "approve") && <Button onClick={() => action.mutate({ id: monthly.id, op: "approve" })} disabled={action.isPending}><CheckCircle size={16} />通过</Button>}
                        {can(monthly, "reject") && <Button variant="danger" onClick={() => action.mutate({ id: monthly.id, op: "reject" })} disabled={action.isPending}><XCircle size={16} />驳回</Button>}
                        {can(monthly, "payment") && <Link to={`/salary/monthly/${monthly.id}/payment`}><Button><Wallet size={16} />发放</Button></Link>}
                        <Link to={`/salary/monthly/${monthly.id}/export`}><Button variant="secondary"><Download size={16} />导出</Button></Link>
                        <Link to={`/salary/monthly/${monthly.id}/payslip`}><Button variant="secondary">工资条</Button></Link>
                        <Button variant="danger" onClick={() => deleteMonthly(monthly.id, monthly.title)} disabled={action.isPending}><Trash2 size={16} />删除</Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal open={recycleOpen} title="月度工资回收站" onClose={() => setRecycleOpen(false)}>
        {(recycleData?.records ?? []).length === 0 ? (
          <div className="empty-state">回收站里还没有已删除的工资表。</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>月份</th>
                  <th>标题</th>
                  <th>人数</th>
                  <th>实发</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {(recycleData?.records ?? []).map((monthly) => (
                  <tr key={monthly.id}>
                    <td>{monthly.salaryMonth}</td>
                    <td>{monthly.title}</td>
                    <td>{monthly.totalEmployeeCount ?? 0}</td>
                    <td>{money(monthly.totalActualAmount)}</td>
                    <td><span className={`badge ${statusClass[monthly.status] ?? "badge-yellow"}`}>{statusText(monthly.status)}</span></td>
                    <td>
                      <Button type="button" variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "restore" })} disabled={action.isPending}>
                        <RotateCcw size={16} />
                        恢复
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>
    </Page>
  );
}
