import { useQuery } from "@tanstack/react-query";
import { Page } from "../../../components/common/Page";
import { getData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Anomaly = { type: string; level: string; salaryMonth?: string; monthlyId?: number; employeeId?: number; employeeName?: string; actualAmount?: number; compareAmount?: number; message: string };

export function SalaryAnomaliesPage() {
  const { data } = useQuery({ queryKey: ["salary-anomalies"], queryFn: () => getData<Anomaly[]>("/statistics/salary/anomalies", { fluctuationRatio: 0.3 }) });
  return (
    <Page title="工资异常提醒">
      <div className="panel table-wrap">
        <table>
          <thead><tr><th>级别</th><th>月份</th><th>员工</th><th>异常</th><th>实发</th><th>对比值</th><th>工资表</th></tr></thead>
          <tbody>{(data ?? []).map((item, index) => <tr key={`${item.type}-${item.monthlyId}-${item.employeeId}-${index}`}><td><span className={item.level === "danger" ? "badge badge-red" : "badge badge-yellow"}>{item.level}</span></td><td>{item.salaryMonth ?? "-"}</td><td>{item.employeeName ?? item.employeeId ?? "-"}</td><td>{item.message}</td><td>{item.actualAmount == null ? "-" : money(item.actualAmount)}</td><td>{item.compareAmount == null ? "-" : money(item.compareAmount)}</td><td>{item.monthlyId ?? "-"}</td></tr>)}</tbody>
        </table>
      </div>
    </Page>
  );
}
