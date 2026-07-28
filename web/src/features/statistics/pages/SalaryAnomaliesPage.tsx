import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { ActionConfirmationModal } from "../../../components/common/ActionConfirmationModal";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, TextArea } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, postData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Anomaly = { anomalyKey: string; type: string; level: string; salaryMonth?: string; monthlyId?: string; employeeId?: string; employeeName?: string; actualAmount?: number; compareAmount?: number; message: string; processStatus?: string; processRemark?: string; processedBy?: string };
type HandlingStatus = "handled" | "ignored" | "adjustment";
type PendingHandling = { item: Anomaly; status: HandlingStatus; remark: string };

export function SalaryAnomaliesPage() {
  const queryClient = useQueryClient();
  const [pendingHandling, setPendingHandling] = useState<PendingHandling | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const anomaliesQuery = useQuery({ queryKey: ["salary-anomalies"], queryFn: () => getData<Anomaly[]>("/statistics/salary/anomalies", { fluctuationRatio: 0.3 }) });
  const handle = useMutation({
    mutationFn: (pending: PendingHandling) => postData("/statistics/salary/anomalies/handle", { anomalyKey: pending.item.anomalyKey, status: pending.status, remark: pending.remark.trim() || undefined }),
    onSuccess: async (_result, pending) => {
      setError("");
      setSuccess(`异常已标记为“${processStatusText(pending.status)}”。`);
      setPendingHandling(null);
      await queryClient.invalidateQueries({ queryKey: ["salary-anomalies"] });
    },
    onError: (reason) => setError(reason instanceof Error ? reason.message : "异常处理失败，请稍后重试。")
  });
  const anomalies = anomaliesQuery.data ?? [];
  const dangerCount = anomalies.filter((item) => item.level === "danger").length;
  const pendingCount = anomalies.filter((item) => !item.processStatus).length;

  function openHandling(item: Anomaly, status: HandlingStatus) {
    setError("");
    setPendingHandling({ item, status, remark: item.processRemark ?? "" });
  }

  return (
    <Page title="工资异常提醒" description="处理工资波动或金额异常，并保留可追溯的处理说明。">
      {anomaliesQuery.isPending ? <RouteLoadingState /> : null}
      {anomaliesQuery.error ? <DataErrorState message={anomaliesQuery.error instanceof Error ? anomaliesQuery.error.message : undefined} onRetry={() => void anomaliesQuery.refetch()} /> : null}
      {!anomaliesQuery.isPending && !anomaliesQuery.error ? (
        <>
          {error ? <div className="form-error page-message" role="alert">{error}</div> : null}
          {success ? <div className="form-success page-message" role="status">{success}</div> : null}
          <div className="grid grid-3">
            <div className="card"><div className="muted">异常总数</div><h3>{anomalies.length}</h3></div>
            <div className="card"><div className="muted">高风险</div><h3>{dangerCount}</h3></div>
            <div className="card"><div className="muted">待处理</div><h3>{pendingCount}</h3></div>
          </div>
          <div className="panel table-wrap">
            {anomalies.length === 0 ? <div className="empty-state">当前筛选条件下没有工资异常，继续按月复核即可。</div> : (
              <table>
                <thead><tr><th>级别</th><th>月份</th><th>员工</th><th>异常</th><th>实发</th><th>对比值</th><th>处理</th><th>备注</th><th>操作</th></tr></thead>
                <tbody>{anomalies.map((item, index) => <tr key={`${item.anomalyKey}-${index}`}>
                  <td><span className={item.level === "danger" ? "badge badge-red" : "badge badge-yellow"}>{item.level}</span></td>
                  <td>{item.salaryMonth ?? "-"}</td>
                  <td>{item.employeeName ?? "未关联员工"}</td>
                  <td>{item.message}</td>
                  <td>{item.actualAmount == null ? "-" : money(item.actualAmount)}</td>
                  <td>{item.compareAmount == null ? "-" : money(item.compareAmount)}</td>
                  <td>{processStatusText(item.processStatus)}</td>
                  <td>{item.processRemark ?? "-"}</td>
                  <td><div className="table-actions"><Button variant="secondary" onClick={() => openHandling(item, "handled")} disabled={handle.isPending}>已处理</Button><Button variant="secondary" onClick={() => openHandling(item, "ignored")} disabled={handle.isPending}>忽略</Button><Button variant="secondary" onClick={() => openHandling(item, "adjustment")} disabled={handle.isPending}>转调整</Button></div></td>
                </tr>)}</tbody>
              </table>
            )}
          </div>
        </>
      ) : null}
      {pendingHandling ? (
        <ActionConfirmationModal
          open
          title={`确认${processStatusText(pendingHandling.status)}异常`}
          description="该处理状态会写入异常记录。请填写必要的说明，便于后续工资复核。"
          confirmLabel={`确认${processStatusText(pendingHandling.status)}`}
          tone={pendingHandling.status === "ignored" ? "danger" : "primary"}
          pending={handle.isPending}
          onClose={() => { if (!handle.isPending) setPendingHandling(null); }}
          onConfirm={() => handle.mutate(pendingHandling)}
          summary={<><strong>{pendingHandling.item.employeeName ?? "未关联员工"}</strong><span>{pendingHandling.item.salaryMonth ?? "-"}</span><span>{pendingHandling.item.message}</span></>}
        >
          <Field label="处理备注">
            <TextArea value={pendingHandling.remark} onChange={(event) => setPendingHandling({ ...pendingHandling, remark: event.target.value })} placeholder="说明核对结果、忽略原因或需要调整的事项" />
          </Field>
        </ActionConfirmationModal>
      ) : null}
    </Page>
  );
}

function processStatusText(status?: string) {
  if (status === "handled") return "已处理";
  if (status === "ignored") return "已忽略";
  if (status === "adjustment") return "转调整";
  return "待处理";
}
