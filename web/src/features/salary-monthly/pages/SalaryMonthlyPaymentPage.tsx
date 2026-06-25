import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Detail = { id: number; employeeId: number; actualAmount: number; paymentStatus: string };

export function SalaryMonthlyPaymentPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ["salary-payment", id], queryFn: () => getData<PageResult<Detail>>(`/salary/monthly/${id}/details`, { page: 1, pageSize: 100 }) });
  const markPaid = useMutation({
    mutationFn: (detailId: number) => postData(`/salary/details/${detailId}/mark-paid`, { paymentDate: new Date().toISOString().slice(0, 10), paymentMethod: "bank_transfer" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-payment", id] })
  });
  const batch = useMutation({
    mutationFn: () => postData("/salary/details/batch-mark-paid", { monthlyId: Number(id), paymentDate: new Date().toISOString().slice(0, 10), paymentMethod: "bank_transfer" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-payment", id] })
  });
  return (
    <Page title="工资发放" actions={<Button onClick={() => batch.mutate()}>批量标记发放</Button>}>
      <div className="panel table-wrap"><table><thead><tr><th>员工 ID</th><th>实发金额</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(detail => <tr key={detail.id}><td>{detail.employeeId}</td><td>{money(detail.actualAmount)}</td><td>{detail.paymentStatus}</td><td><Button variant="secondary" onClick={() => markPaid.mutate(detail.id)}>标记发放</Button></td></tr>)}</tbody></table></div>
    </Page>
  );
}

