import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Eye, RefreshCw, Save } from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData, putData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";
import type { Employee } from "../../employee/types";

type Monthly = {
  id: string;
  salaryMonth: string;
  title: string;
  totalEmployeeCount: number;
  totalPayableAmount: number;
  totalDeductionAmount: number;
  totalActualAmount: number;
  status: string;
  remark?: string;
};

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

type Profile = {
  id: string;
  status: string;
};

const badgeClass: Record<string, string> = {
  draft: "badge-yellow",
  pending_approval: "badge-yellow",
  approved: "badge-green",
  paid: "badge-green",
  unpaid: "badge-yellow"
};

export function SalaryMonthlyDetailPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [editing, setEditing] = useState<Detail | null>(null);
  const [viewing, setViewing] = useState<Detail | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const { data: monthly } = useQuery({
    queryKey: ["salary-monthly", monthlyId],
    queryFn: () => getData<Monthly>(`/salary/monthly/${monthlyId}`)
  });
  const { data: detailsData } = useQuery({
    queryKey: ["salary-details", monthlyId],
    queryFn: () => getData<PageResult<Detail>>(`/salary/monthly/${monthlyId}/details`, { page: 1, pageSize: 500 })
  });
  const { data: employeesData } = useQuery({
    queryKey: ["employees", "salary-monthly-detail-options"],
    queryFn: () => getData<PageResult<Employee>>("/employees", { page: 1, pageSize: 1000 })
  });
  const { data: monthlyDetailItems } = useQuery({
    queryKey: ["salary-monthly-detail-items", monthlyId],
    queryFn: () => getData<DetailItem[]>(`/salary/monthly/${monthlyId}/detail-items`)
  });
  const { data: profilesData } = useQuery({
    queryKey: ["salary-profiles", "monthly-detail-diagnostics"],
    queryFn: () => getData<PageResult<Profile>>("/salary/profiles", { page: 1, pageSize: 500 })
  });

  const employees = employeesData?.records ?? [];
  const employeeMap = useMemo(() => new Map(employees.map((employee) => [employee.id, employee])), [employees]);
  const details = detailsData?.records ?? [];
  const detailItemMap = useMemo(() => {
    const map = new Map<string, DetailItem[]>();
    for (const item of monthlyDetailItems ?? []) {
      const rows = map.get(item.salaryDetailId) ?? [];
      rows.push(item);
      map.set(item.salaryDetailId, rows);
    }
    return map;
  }, [monthlyDetailItems]);
  const visibleDetails = details.filter((detail) => {
    const employee = employeeMap.get(detail.employeeId);
    const matchesKeyword = !keyword || String(detail.employeeId).includes(keyword) || employee?.name?.includes(keyword);
    const matchesStatus = !paymentStatus || detail.paymentStatus === paymentStatus;
    return matchesKeyword && matchesStatus;
  });
  const paidCount = details.filter((detail) => detail.paymentStatus === "paid").length;
  const paidAmount = details.reduce((sum, detail) => sum + (detail.paymentStatus === "paid" ? Number(detail.actualAmount ?? 0) : 0), 0);
  const editable = monthly?.status === "draft";
  const activeProfileCount = (profilesData?.records ?? []).filter((profile) => profile.status === "active").length;

  const generate = useMutation({
    mutationFn: () => postData<Monthly>(`/salary/monthly/${monthlyId}/generate`),
    onSuccess: async (result) => {
      setError("");
      setSuccess(`重新生成完成：${result.totalEmployeeCount ?? 0} 人，实发合计 ${money(result.totalActualAmount)}。`);
      await queryClient.invalidateQueries({ queryKey: ["salary-details", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly-detail-items", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => {
      setSuccess("");
      setError(reason instanceof Error ? reason.message : "生成失败");
    }
  });
  const update = useMutation({
    mutationFn: (detail: Detail) => putData<Detail>(`/salary/details/${detail.id}`, detail),
    onSuccess: async () => {
      setError("");
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-details", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "保存失败")
  });

  function submitEdit(event: FormEvent) {
    event.preventDefault();
    if (editing) update.mutate(editing);
  }

  function patchEditing(patch: Partial<Detail>) {
    setEditing((current) => current ? { ...current, ...patch } : current);
  }

  return (
    <Page
      title={monthly?.title ?? "工资明细"}
      actions={
        <div className="table-actions">
          {monthly?.status === "draft" && <Button type="button" variant="secondary" onClick={() => generate.mutate()} disabled={generate.isPending}><RefreshCw size={16} />{generate.isPending ? "生成中" : "重新生成"}</Button>}
          {monthly && <Link to={`/salary/monthly/${monthly.id}/payment`}><Button>发放登记</Button></Link>}
        </div>
      }
    >
      {error && <div className="form-error page-message">{error}</div>}
      {success && <div className="form-success page-message">{success}</div>}
      <div className="grid grid-3">
        <div className="card"><div className="muted">工资月份</div><h3>{monthly?.salaryMonth ?? "-"}</h3></div>
        <div className="card"><div className="muted">状态</div><h3>{statusText(monthly?.status)}</h3></div>
        <div className="card"><div className="muted">人数</div><h3>{monthly?.totalEmployeeCount ?? 0}</h3></div>
        <div className="card"><div className="muted">应发</div><h3>{money(monthly?.totalPayableAmount)}</h3></div>
        <div className="card"><div className="muted">扣减</div><h3>{money(monthly?.totalDeductionAmount)}</h3></div>
        <div className="card"><div className="muted">实发</div><h3>{money(monthly?.totalActualAmount)}</h3></div>
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
        {details.length === 0 && (
          <div className="empty-state">
            当前工资表还没有工资明细。系统检测到 {activeProfileCount} 个启用工资档案。
            {monthly?.status === "draft" ? " 请点击页面右上角“重新生成”同步工资档案。" : " 当前工资表不是草稿状态，不能重新生成。"}
          </div>
        )}
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
                <TextInput type="number" step="0.01" value={editing.actualAmount} onChange={(event) => patchEditing({ actualAmount: Number(event.target.value) })} />
              </Field>
            </div>
            <Button type="button" variant="secondary" onClick={() => patchEditing({ actualAmount: Number(editing.payableAmount ?? 0) - Number(editing.deductionAmount ?? 0) })}>
              按应发减扣款重算
            </Button>
            <Field label="备注">
              <TextArea value={editing.remark ?? ""} onChange={(event) => patchEditing({ remark: event.target.value })} />
            </Field>
          </form>
        )}
      </Modal>

      <Modal open={Boolean(viewing)} title="工资项组成" onClose={() => setViewing(null)}>
        {viewing && (detailItemMap.get(viewing.id) ?? []).length === 0 && (
          <div className="empty-state">这条工资明细没有工资项快照。请确认生成工资表前该员工已有启用工资档案，并在草稿状态重新生成。</div>
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
      </Modal>
    </Page>
  );
}
