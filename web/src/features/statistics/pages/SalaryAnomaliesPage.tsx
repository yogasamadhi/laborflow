import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, postData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Anomaly = { anomalyKey: string; type: string; level: string; salaryMonth?: string; monthlyId?: string; employeeId?: string; employeeName?: string; actualAmount?: number; compareAmount?: number; message: string; processStatus?: string; processRemark?: string; processedBy?: string };

export function SalaryAnomaliesPage() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ["salary-anomalies"], queryFn: () => getData<Anomaly[]>("/statistics/salary/anomalies", { fluctuationRatio: 0.3 }) });
  const handle = useMutation({
    mutationFn: ({ anomalyKey, status, remark }: { anomalyKey: string; status: string; remark?: string }) => postData("/statistics/salary/anomalies/handle", { anomalyKey, status, remark }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-anomalies"] })
  });

  function submit(item: Anomaly, status: "handled" | "ignored" | "adjustment") {
    const remark = window.prompt(processStatusText(status), item.processRemark ?? "");
    handle.mutate({ anomalyKey: item.anomalyKey, status, remark: remark ?? undefined });
  }

  return (
    <Page title="工资异常提醒">
      <div className="panel table-wrap">
        <table>
          <thead><tr><th>级别</th><th>月份</th><th>员工</th><th>异常</th><th>实发</th><th>对比值</th><th>处理</th><th>备注</th><th>操作</th></tr></thead>
          <tbody>{(data ?? []).map((item, index) => <tr key={`${item.anomalyKey}-${index}`}><td><span className={item.level === "danger" ? "badge badge-red" : "badge badge-yellow"}>{item.level}</span></td><td>{item.salaryMonth ?? "-"}</td><td>{item.employeeName ?? item.employeeId ?? "-"}</td><td>{item.message}</td><td>{item.actualAmount == null ? "-" : money(item.actualAmount)}</td><td>{item.compareAmount == null ? "-" : money(item.compareAmount)}</td><td>{processStatusText(item.processStatus)}</td><td>{item.processRemark ?? "-"}</td><td><div className="table-actions"><Button variant="secondary" onClick={() => submit(item, "handled")} disabled={handle.isPending}>已处理</Button><Button variant="secondary" onClick={() => submit(item, "ignored")} disabled={handle.isPending}>忽略</Button><Button variant="secondary" onClick={() => submit(item, "adjustment")} disabled={handle.isPending}>转调整</Button></div></td></tr>)}</tbody>
        </table>
      </div>
    </Page>
  );
}

function processStatusText(status?: string) {
  if (status === "handled") return "已处理";
  if (status === "ignored") return "已忽略";
  if (status === "adjustment") return "转调整";
  return "待处理";
}
