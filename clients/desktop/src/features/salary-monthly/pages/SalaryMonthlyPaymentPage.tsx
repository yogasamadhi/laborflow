import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Wallet } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ActionConfirmationModal } from "../../../components/common/ActionConfirmationModal";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getAllPages, getData, postData, uploadFile } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";
import type { Employee } from "../../employee/types";

type Monthly = {
  id: string;
  salaryMonth: string;
  title: string;
  status: string;
  totalActualAmount: number;
};

type Detail = {
  id: string;
  employeeId: string;
  actualAmount: number;
  paymentStatus: string;
  paymentDate?: string;
  paymentMethod?: string;
  paymentProofUrl?: string;
};

type PaymentForm = {
  paymentDate: string;
  paymentMethod: string;
  paymentProofUrl: string;
};

type PendingPayment =
  | { kind: "single"; details: [Detail] }
  | { kind: "batch"; details: Detail[] };

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function SalaryMonthlyPaymentPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [form, setForm] = useState<PaymentForm>({ paymentDate: today(), paymentMethod: "bank_transfer", paymentProofUrl: "" });
  const [proofName, setProofName] = useState("");
  const [proofUploading, setProofUploading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [pendingPayment, setPendingPayment] = useState<PendingPayment | null>(null);
  const monthlyQuery = useQuery({
    queryKey: ["salary-monthly", monthlyId],
    queryFn: () => getData<Monthly>(`/salary/monthly/${monthlyId}`)
  });
  const detailsQuery = useQuery({
    queryKey: ["salary-payment", monthlyId],
    queryFn: () => getAllPages<Detail>(`/salary/monthly/${monthlyId}/details`)
  });
  const { data: employeesData } = useQuery({
    queryKey: ["employees", "salary-payment-options"],
    queryFn: () => getAllPages<Employee>("/reference/employees")
  });

  const monthly = monthlyQuery.data;
  const details = detailsQuery.data ?? [];
  const unpaidDetails = details.filter((detail) => detail.paymentStatus !== "paid");
  const paidDetails = details.filter((detail) => detail.paymentStatus === "paid");
  const selectedDetails = details.filter((detail) => selectedIds.includes(detail.id));
  const batchDetails = selectedDetails.length > 0 ? selectedDetails : unpaidDetails;
  const batchAmount = batchDetails.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0);
  const paidAmount = paidDetails.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0);
  const employeeMap = useMemo(() => new Map((employeesData ?? []).map((employee) => [employee.id, employee])), [employeesData]);
  const canPay = monthly?.status === "approved";
  const coreError = monthlyQuery.error || detailsQuery.error;
  const isCorePending = monthlyQuery.isPending || detailsQuery.isPending;

  const payment = useMutation({
    mutationFn: (pending: PendingPayment) => {
      if (pending.kind === "single") return postData(`/salary/details/${pending.details[0].id}/mark-paid`, form);
      return postData("/salary/details/batch-mark-paid", {
        detailIds: selectedIds.length > 0 ? pending.details.map((detail) => detail.id) : undefined,
        monthlyId: selectedIds.length === 0 ? monthlyId : undefined,
        ...form
      });
    },
    onSuccess: async (_result, pending) => {
      const amount = pending.details.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0);
      setError("");
      setSuccess(`${pending.kind === "single" ? "单人" : "批量"}发放已登记：${pending.details.length} 人，金额 ${money(amount)}。`);
      setSelectedIds([]);
      setPendingPayment(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-payment", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "发放登记失败，请稍后重试。")
  });

  function toggleSelected(detailId: string) {
    setSelectedIds((current) => current.includes(detailId) ? current.filter((item) => item !== detailId) : [...current, detailId]);
  }

  function selectAllUnpaid() {
    setSelectedIds(unpaidDetails.map((detail) => detail.id));
  }

  function openBatchConfirmation() {
    if (!canPay || batchDetails.length === 0) return;
    setError("");
    setPendingPayment({ kind: "batch", details: batchDetails });
  }

  function openSingleConfirmation(detail: Detail) {
    if (!canPay || detail.paymentStatus === "paid") return;
    setError("");
    setPendingPayment({ kind: "single", details: [detail] });
  }

  async function attachProof(file?: File) {
    if (!file) return;
    setProofUploading(true);
    try {
      const uploaded = await uploadFile(file);
      setForm((current) => ({ ...current, paymentProofUrl: `/api/files/${uploaded.id}/download` }));
      setProofName(uploaded.originalName ?? file.name);
      setError("");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "凭证上传失败");
    } finally {
      setProofUploading(false);
    }
  }

  const pendingAmount = pendingPayment?.details.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0) ?? 0;

  return (
    <Page
      title="工资发放"
      description="仅已审核工资表可登记发放。确认金额、方式和凭证后再写入发放记录。"
      actions={
        <div className="table-actions">
          <Link to={`/salary/monthly/${monthlyId}`}><Button variant="secondary">返回明细</Button></Link>
          <Button onClick={openBatchConfirmation} disabled={!canPay || payment.isPending || unpaidDetails.length === 0}>
            <Wallet size={16} />
            {selectedIds.length > 0 ? "发放选中" : "发放全部未发"}
          </Button>
        </div>
      }
    >
      {isCorePending ? <RouteLoadingState /> : null}
      {coreError ? <DataErrorState message={coreError instanceof Error ? coreError.message : undefined} onRetry={() => { void monthlyQuery.refetch(); void detailsQuery.refetch(); }} /> : null}
      {!isCorePending && !coreError ? (
        <>
          {error ? <div className="form-error page-message" role="alert">{error}</div> : null}
          {success ? <div className="form-success page-message" role="status">{success}</div> : null}
          {monthly && monthly.status !== "approved" && monthly.status !== "paid" ? <div className="form-warning page-message">当前工资表状态为“{statusText(monthly.status)}”，需要审核通过后才能登记发放。</div> : null}
          {monthly?.status === "paid" ? <div className="form-info page-message">工资表已完成发放登记；如需核对，请查看明细与审批证据链。</div> : null}

          <div className="grid grid-3">
            <div className="card"><div className="muted">工资月份</div><h3>{monthly?.salaryMonth ?? "-"}</h3></div>
            <div className="card"><div className="muted">已发人数</div><h3>{paidDetails.length}/{details.length}</h3></div>
            <div className="card"><div className="muted">已发金额</div><h3>{money(paidAmount)}</h3></div>
          </div>

          <div className="panel grid payment-panel">
            <div className="grid grid-3">
              <Field label="发放日期">
                <TextInput type="date" value={form.paymentDate} onChange={(event) => setForm({ ...form, paymentDate: event.target.value })} disabled={!canPay} />
              </Field>
              <Field label="发放方式">
                <SelectInput value={form.paymentMethod} onChange={(event) => setForm({ ...form, paymentMethod: event.target.value })} disabled={!canPay}>
                  <option value="bank_transfer">银行转账</option>
                  <option value="cash">现金</option>
                  <option value="wechat">微信</option>
                  <option value="alipay">支付宝</option>
                  <option value="other">其他</option>
                </SelectInput>
              </Field>
              <Field label="发放凭证">
                <input type="file" disabled={!canPay || proofUploading} onChange={(event) => void attachProof(event.target.files?.[0])} />
                {form.paymentProofUrl ? <span className="muted">{proofName || "已上传"}</span> : null}
              </Field>
            </div>
            <div className="summary-strip">
              <span>未发人数：{unpaidDetails.length}</span>
              <span>已选人数：{selectedDetails.length}</span>
              <strong>本次金额：{money(batchAmount)}</strong>
              <Button type="button" variant="secondary" onClick={selectAllUnpaid} disabled={!canPay || unpaidDetails.length === 0}>选择全部未发</Button>
            </div>
          </div>

          <div className="panel table-wrap">
            {details.length === 0 ? <div className="empty-state">当前工资表还没有可发放的工资明细。</div> : (
              <table>
                <thead>
                  <tr><th>选择</th><th>员工</th><th>实发金额</th><th>状态</th><th>发放日期</th><th>方式</th><th>凭证</th><th>操作</th></tr>
                </thead>
                <tbody>
                  {details.map((detail) => {
                    const employee = employeeMap.get(detail.employeeId);
                    const paid = detail.paymentStatus === "paid";
                    return (
                      <tr key={detail.id}>
                        <td><input type="checkbox" checked={selectedIds.includes(detail.id)} disabled={!canPay || paid} onChange={() => toggleSelected(detail.id)} aria-label={`选择员工 ${employee?.name ?? detail.employeeId}`} /></td>
                        <td>{employee ? `${employee.name} / ${employee.id}` : detail.employeeId}</td>
                        <td>{money(detail.actualAmount)}</td>
                        <td><span className={paid ? "badge badge-green" : "badge badge-yellow"}>{statusText(detail.paymentStatus)}</span></td>
                        <td>{detail.paymentDate ?? "-"}</td>
                        <td>{detail.paymentMethod ?? "-"}</td>
                        <td>{detail.paymentProofUrl ? "已登记" : "-"}</td>
                        <td><Button type="button" variant="secondary" disabled={!canPay || paid || payment.isPending} onClick={() => openSingleConfirmation(detail)}><CheckCircle size={16} />单人发放</Button></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </>
      ) : null}

      {pendingPayment ? (
        <ActionConfirmationModal
          open
          title={pendingPayment.kind === "single" ? "确认登记单人发放" : "确认登记批量发放"}
          description="确认后会写入发放日期、方式和可选凭证，并在工资审批证据链中留下记录。"
          confirmLabel={pendingPayment.kind === "single" ? "确认发放" : "确认批量发放"}
          pending={payment.isPending}
          onClose={() => { if (!payment.isPending) setPendingPayment(null); }}
          onConfirm={() => payment.mutate(pendingPayment)}
          summary={<><strong>{pendingPayment.details.length} 人</strong><span>金额：{money(pendingAmount)}</span><span>{form.paymentDate} · {paymentMethodText(form.paymentMethod)}</span>{form.paymentProofUrl ? <span>已附凭证</span> : <span>未附凭证</span>}</>}
        >
          {pendingPayment.kind === "single" ? <div className="form-info">本次仅登记所选员工的发放记录。</div> : <div className="form-info">本次将登记当前选择的未发明细；未选择时将登记全部未发明细。</div>}
        </ActionConfirmationModal>
      ) : null}
    </Page>
  );
}

function paymentMethodText(method: string) {
  const map: Record<string, string> = {
    bank_transfer: "银行转账",
    cash: "现金",
    wechat: "微信",
    alipay: "支付宝",
    other: "其他"
  };
  return map[method] ?? method;
}
