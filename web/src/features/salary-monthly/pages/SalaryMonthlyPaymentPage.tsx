import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Wallet } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData } from "../../../lib/axios";
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

function today() {
  return new Date().toISOString().slice(0, 10);
}

export function SalaryMonthlyPaymentPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [form, setForm] = useState<PaymentForm>({ paymentDate: today(), paymentMethod: "bank_transfer", paymentProofUrl: "" });
  const [error, setError] = useState("");

  const { data: monthly } = useQuery({
    queryKey: ["salary-monthly", monthlyId],
    queryFn: () => getData<Monthly>(`/salary/monthly/${monthlyId}`)
  });
  const { data } = useQuery({
    queryKey: ["salary-payment", monthlyId],
    queryFn: () => getData<PageResult<Detail>>(`/salary/monthly/${monthlyId}/details`, { page: 1, pageSize: 500 })
  });
  const { data: employeesData } = useQuery({
    queryKey: ["employees", "salary-payment-options"],
    queryFn: () => getData<PageResult<Employee>>("/employees", { page: 1, pageSize: 1000 })
  });

  const details = data?.records ?? [];
  const unpaidDetails = details.filter((detail) => detail.paymentStatus !== "paid");
  const paidDetails = details.filter((detail) => detail.paymentStatus === "paid");
  const selectedDetails = details.filter((detail) => selectedIds.includes(detail.id));
  const selectedAmount = selectedDetails.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0);
  const paidAmount = paidDetails.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0);
  const employeeMap = useMemo(() => new Map((employeesData?.records ?? []).map((employee) => [employee.id, employee])), [employeesData]);
  const canPay = monthly?.status === "approved";

  const markPaid = useMutation({
    mutationFn: (detailId: string) => postData(`/salary/details/${detailId}/mark-paid`, form),
    onSuccess: async () => {
      setError("");
      await queryClient.invalidateQueries({ queryKey: ["salary-payment", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "发放登记失败")
  });
  const batch = useMutation({
    mutationFn: () => postData("/salary/details/batch-mark-paid", {
      detailIds: selectedIds.length > 0 ? selectedIds : undefined,
      monthlyId: selectedIds.length === 0 ? monthlyId : undefined,
      ...form
    }),
    onSuccess: async () => {
      setError("");
      setSelectedIds([]);
      await queryClient.invalidateQueries({ queryKey: ["salary-payment", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-audits", monthlyId] });
      await queryClient.invalidateQueries({ queryKey: ["salary-monthly"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "批量发放失败")
  });

  function toggleSelected(detailId: string) {
    setSelectedIds((current) => current.includes(detailId) ? current.filter((id) => id !== detailId) : [...current, detailId]);
  }

  function selectAllUnpaid() {
    setSelectedIds(unpaidDetails.map((detail) => detail.id));
  }

  return (
    <Page
      title="工资发放"
      actions={
        <div className="table-actions">
          <Link to={`/salary/monthly/${monthlyId}`}><Button variant="secondary">返回明细</Button></Link>
          <Button onClick={() => batch.mutate()} disabled={!canPay || batch.isPending || unpaidDetails.length === 0}>
            <Wallet size={16} />
            {selectedIds.length > 0 ? "发放选中" : "发放全部未发"}
          </Button>
        </div>
      }
    >
      {error && <div className="form-error page-message">{error}</div>}
      {monthly && monthly.status !== "approved" && monthly.status !== "paid" && (
        <div className="form-error page-message">当前工资表状态为“{statusText(monthly.status)}”，需要审核通过后才能登记发放。</div>
      )}
      <div className="grid grid-3">
        <div className="card"><div className="muted">工资月份</div><h3>{monthly?.salaryMonth ?? "-"}</h3></div>
        <div className="card"><div className="muted">已发人数</div><h3>{paidDetails.length}/{details.length}</h3></div>
        <div className="card"><div className="muted">已发金额</div><h3>{money(paidAmount)}</h3></div>
      </div>

      <div className="panel grid payment-panel">
        <div className="grid grid-3">
          <Field label="发放日期">
            <TextInput type="date" value={form.paymentDate} onChange={(event) => setForm({ ...form, paymentDate: event.target.value })} />
          </Field>
          <Field label="发放方式">
            <SelectInput value={form.paymentMethod} onChange={(event) => setForm({ ...form, paymentMethod: event.target.value })}>
              <option value="bank_transfer">银行转账</option>
              <option value="cash">现金</option>
              <option value="wechat">微信</option>
              <option value="alipay">支付宝</option>
              <option value="other">其他</option>
            </SelectInput>
          </Field>
          <Field label="凭证地址">
            <TextInput value={form.paymentProofUrl} onChange={(event) => setForm({ ...form, paymentProofUrl: event.target.value })} placeholder="可填上传后的凭证 URL" />
          </Field>
        </div>
        <div className="summary-strip">
          <span>未发人数：{unpaidDetails.length}</span>
          <span>已选人数：{selectedDetails.length}</span>
          <strong>本次金额：{money(selectedIds.length > 0 ? selectedAmount : unpaidDetails.reduce((sum, detail) => sum + Number(detail.actualAmount ?? 0), 0))}</strong>
          <Button type="button" variant="secondary" onClick={selectAllUnpaid} disabled={!canPay || unpaidDetails.length === 0}>选择全部未发</Button>
        </div>
      </div>

      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>选择</th>
              <th>员工</th>
              <th>实发金额</th>
              <th>状态</th>
              <th>发放日期</th>
              <th>方式</th>
              <th>凭证</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {details.map((detail) => {
              const employee = employeeMap.get(detail.employeeId);
              const paid = detail.paymentStatus === "paid";
              return (
                <tr key={detail.id}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(detail.id)}
                      disabled={!canPay || paid}
                      onChange={() => toggleSelected(detail.id)}
                    />
                  </td>
                  <td>{employee ? `${employee.name} / ${employee.id}` : detail.employeeId}</td>
                  <td>{money(detail.actualAmount)}</td>
                  <td><span className={paid ? "badge badge-green" : "badge badge-yellow"}>{statusText(detail.paymentStatus)}</span></td>
                  <td>{detail.paymentDate ?? "-"}</td>
                  <td>{detail.paymentMethod ?? "-"}</td>
                  <td>{detail.paymentProofUrl ? "已登记" : "-"}</td>
                  <td>
                    <Button type="button" variant="secondary" disabled={!canPay || paid || markPaid.isPending} onClick={() => markPaid.mutate(detail.id)}>
                      <CheckCircle size={16} />
                      单人发放
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Page>
  );
}
