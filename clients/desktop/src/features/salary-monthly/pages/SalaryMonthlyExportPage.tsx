import { CheckCircle, Download } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile, getData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";

type Monthly = { id: string; title: string; salaryMonth: string; status: string; totalEmployeeCount: number; totalActualAmount: number };

type ExportItem = { label: string; description: string; url: string; filename: string };

export function SalaryMonthlyExportPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const [runningExport, setRunningExport] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const monthlyQuery = useQuery({
    queryKey: ["salary-monthly", monthlyId],
    queryFn: () => getData<Monthly>(`/salary/monthly/${monthlyId}`)
  });
  const exports: ExportItem[] = [
    { label: "月度工资表", description: "按员工汇总应发、扣减和实发金额。", url: `/salary/monthly/${monthlyId}/export`, filename: `月度工资表-${monthlyId}.xlsx` },
    { label: "部门工资汇总", description: "按部门汇总工资人数和金额，便于管理复核。", url: `/salary/monthly/${monthlyId}/export/department-summary`, filename: `部门工资汇总-${monthlyId}.xlsx` },
    { label: "工资发放记录", description: "导出已登记的发放日期、方式和状态。", url: `/salary/monthly/${monthlyId}/export/payment-records`, filename: `工资发放记录-${monthlyId}.xlsx` },
    { label: "银行代发", description: "提供银行代发所需的员工和金额清单。", url: `/salary/monthly/${monthlyId}/export/bank-payment`, filename: `银行代发-${monthlyId}.xlsx` }
  ];

  async function runExport(item: ExportItem) {
    setRunningExport(item.url);
    setError("");
    setSuccess("");
    try {
      await downloadFile(item.url, item.filename);
      setSuccess(`已开始下载“${item.label}”。`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "导出失败，请稍后重试。");
    } finally {
      setRunningExport(null);
    }
  }

  return (
    <Page title="工资表导出" description="选择适合当前复核或发放场景的 Excel 文件。" actions={<Link to={`/salary/monthly/${monthlyId}`}><Button variant="secondary">返回工资明细</Button></Link>}>
      {monthlyQuery.isPending ? <RouteLoadingState /> : null}
      {monthlyQuery.error ? <DataErrorState message={monthlyQuery.error instanceof Error ? monthlyQuery.error.message : undefined} onRetry={() => void monthlyQuery.refetch()} /> : null}
      {!monthlyQuery.isPending && !monthlyQuery.error ? (
        <>
          {error ? <div className="form-error page-message" role="alert">{error}</div> : null}
          {success ? <div className="form-success page-message" role="status"><CheckCircle size={16} />{success}</div> : null}
          <div className="summary-strip export-context">
            <strong>{monthlyQuery.data?.title}</strong>
            <span>{monthlyQuery.data?.salaryMonth}</span>
            <span>{monthlyQuery.data?.totalEmployeeCount ?? 0} 人</span>
            <span>{statusText(monthlyQuery.data?.status)}</span>
          </div>
          <div className="export-grid">
            {exports.map((item) => (
              <section className="export-option" key={item.url}>
                <div>
                  <h3>{item.label}</h3>
                  <p>{item.description}</p>
                </div>
                <Button type="button" variant="secondary" disabled={runningExport !== null} onClick={() => void runExport(item)}>
                  <Download size={16} />
                  {runningExport === item.url ? "正在准备" : "导出 Excel"}
                </Button>
              </section>
            ))}
          </div>
        </>
      ) : null}
    </Page>
  );
}
