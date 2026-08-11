import { useQuery } from "@tanstack/react-query";
import { CheckCircle, Download } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, SelectInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile, getAllPages } from "../../../lib/axios";
import type { Employee } from "../../employee/types";

type Detail = { id: string; employeeId: string; paymentStatus: string };

export function SalaryMonthlyPayslipPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const [employeeId, setEmployeeId] = useState("");
  const [isExporting, setIsExporting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const detailsQuery = useQuery({
    queryKey: ["salary-payslip-details", monthlyId],
    queryFn: () => getAllPages<Detail>(`/salary/monthly/${monthlyId}/details`)
  });
  const employeesQuery = useQuery({
    queryKey: ["employees", "salary-payslip-options"],
    queryFn: () => getAllPages<Employee>("/reference/employees")
  });
  const employeeMap = useMemo(() => new Map((employeesQuery.data ?? []).map((employee) => [employee.id, employee])), [employeesQuery.data]);
  const records = detailsQuery.data ?? [];

  async function exportPayslips() {
    setIsExporting(true);
    setError("");
    setSuccess("");
    try {
      await downloadFile(`/salary/monthly/${monthlyId}/payslip`, `工资条-${monthlyId}.xlsx`, employeeId ? { employeeId } : undefined);
      setSuccess(employeeId ? "已开始下载所选员工的工资条。" : "已开始下载全部员工的工资条。");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "工资条导出失败，请稍后重试。");
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <Page title="工资条导出" description="按员工筛选工资条，导出前确认范围，避免包含不需要的个人薪酬信息。" actions={<Link to={`/salary/monthly/${monthlyId}`}><Button variant="secondary">返回工资明细</Button></Link>}>
      {detailsQuery.isPending ? <RouteLoadingState /> : null}
      {detailsQuery.error ? <DataErrorState message={detailsQuery.error instanceof Error ? detailsQuery.error.message : undefined} onRetry={() => void detailsQuery.refetch()} /> : null}
      {!detailsQuery.isPending && !detailsQuery.error ? (
        <>
          {error ? <div className="form-error page-message" role="alert">{error}</div> : null}
          {success ? <div className="form-success page-message" role="status"><CheckCircle size={16} />{success}</div> : null}
          {records.length === 0 ? <div className="empty-state">当前工资表还没有工资明细，暂时无法导出工资条。</div> : (
            <div className="panel payslip-export-panel">
              <Field label="员工">
                <SelectInput value={employeeId} onChange={(event) => setEmployeeId(event.target.value)}>
                  <option value="">全部员工（{records.length} 人）</option>
                  {records.map((detail) => {
                    const employee = employeeMap.get(detail.employeeId);
                    return <option key={detail.id} value={detail.employeeId}>{employee ? employee.name : "员工记录已删除"}</option>;
                  })}
                </SelectInput>
              </Field>
              <div className="summary-strip">
                <span>{employeeId ? "当前仅导出所选员工" : `当前导出 ${records.length} 名员工`}</span>
                <span>请仅向有权限的收件人发送工资条。</span>
              </div>
              <Button type="button" onClick={() => void exportPayslips()} disabled={isExporting}>
                <Download size={16} />
                {isExporting ? "正在准备" : "导出工资条"}
              </Button>
            </div>
          )}
        </>
      ) : null}
    </Page>
  );
}
