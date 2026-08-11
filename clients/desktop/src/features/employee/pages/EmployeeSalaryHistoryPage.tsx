import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { useState } from "react";
import { useParams } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile, getData, postData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";

type RecordRow = { salaryMonth: string; monthlyId: string; detailId: string; payableAmount: number; deductionAmount: number; actualAmount: number; paymentStatus: string; paymentDate?: string; paymentMethod?: string; payslipStatus?: string; payslipFeedback?: string; remark?: string };
type YearSummary = { year: string; payableAmount: number; deductionAmount: number; actualAmount: number; paidCount: number; unpaidCount: number };
type History = { employeeId: string; employeeName: string; totalActualAmount: number; latestPaymentRecord?: RecordRow; records: RecordRow[]; yearlySummaries: YearSummary[] };

export function EmployeeSalaryHistoryPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [error, setError] = useState("");
  const { data } = useQuery({ queryKey: ["employee-salary-history", id], queryFn: () => getData<History>(`/statistics/salary/employee-history/${id}`) });
  const confirm = useMutation({
    mutationFn: (detailId: string) => postData(`/salary/details/${detailId}/payslip/confirm`),
    onSuccess: async () => {
      setError("");
      await queryClient.invalidateQueries({ queryKey: ["employee-salary-history", id] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "确认失败")
  });
  const feedback = useMutation({
    mutationFn: ({ detailId, text }: { detailId: string; text: string }) => postData(`/salary/details/${detailId}/payslip/feedback`, { feedback: text }),
    onSuccess: async () => {
      setError("");
      await queryClient.invalidateQueries({ queryKey: ["employee-salary-history", id] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "反馈失败")
  });

  function submitFeedback(row: RecordRow) {
    const text = window.prompt("异常反馈", row.payslipFeedback ?? "");
    if (text && text.trim()) {
      feedback.mutate({ detailId: row.detailId, text: text.trim() });
    }
  }

  return (
    <Page title="历史工资" actions={<Button onClick={() => downloadFile(`/salary/employees/${id}/details/export`, `员工工资明细-${id}.xlsx`)}><Download size={16} />导出明细</Button>}>
      {error && <div className="form-error page-message">{error}</div>}
      <div className="grid grid-3">
        <div className="card"><div className="muted">员工</div><h3>{data?.employeeName ?? "-"}</h3></div>
        <div className="card"><div className="muted">累计实发</div><h3>{money(data?.totalActualAmount)}</h3></div>
        <div className="card"><div className="muted">最近发放</div><h3>{data?.latestPaymentRecord?.paymentDate ?? "-"}</h3></div>
      </div>
      <div className="panel table-wrap" style={{ marginTop: 16 }}>
        <table>
          <thead><tr><th>年度</th><th>应发</th><th>扣减</th><th>实发</th><th>已发</th><th>未发</th></tr></thead>
          <tbody>{(data?.yearlySummaries ?? []).map((row) => <tr key={row.year}><td>{row.year}</td><td>{money(row.payableAmount)}</td><td>{money(row.deductionAmount)}</td><td>{money(row.actualAmount)}</td><td>{row.paidCount}</td><td>{row.unpaidCount}</td></tr>)}</tbody>
        </table>
      </div>
      <div className="panel table-wrap" style={{ marginTop: 16 }}>
        <table>
          <thead><tr><th>月份</th><th>应发</th><th>扣减</th><th>实发</th><th>发放状态</th><th>发放日期</th><th>方式</th><th>工资条</th><th>反馈</th><th>操作</th></tr></thead>
          <tbody>{(data?.records ?? []).map((row) => <tr key={row.detailId}><td>{row.salaryMonth}</td><td>{money(row.payableAmount)}</td><td>{money(row.deductionAmount)}</td><td>{money(row.actualAmount)}</td><td>{statusText(row.paymentStatus)}</td><td>{row.paymentDate ?? "-"}</td><td>{row.paymentMethod ?? "-"}</td><td>{payslipStatusText(row.payslipStatus)}</td><td>{row.payslipFeedback ?? "-"}</td><td><div className="table-actions"><Button variant="secondary" onClick={() => confirm.mutate(row.detailId)} disabled={confirm.isPending || row.payslipStatus === "confirmed"}>确认</Button><Button variant="secondary" onClick={() => submitFeedback(row)} disabled={feedback.isPending}>反馈异常</Button></div></td></tr>)}</tbody>
        </table>
      </div>
    </Page>
  );
}

function payslipStatusText(status?: string) {
  if (status === "confirmed") return "已确认";
  if (status === "exception") return "有异常";
  return "待确认";
}
