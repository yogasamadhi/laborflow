import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, putData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Detail = { id: number; employeeId: number; payableAmount: number; deductionAmount: number; actualAmount: number; paymentStatus: string };

export function SalaryMonthlyDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ["salary-details", id], queryFn: () => getData<PageResult<Detail>>(`/salary/monthly/${id}/details`, { page: 1, pageSize: 100 }) });
  const adjust = useMutation({
    mutationFn: (detail: Detail) => putData(`/salary/details/${detail.id}`, { ...detail, actualAmount: detail.actualAmount + 100 }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-details", id] })
  });
  return (
    <Page title="工资明细">
      <div className="panel table-wrap"><table><thead><tr><th>员工 ID</th><th>应发</th><th>扣款</th><th>实发</th><th>发放状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(detail => <tr key={detail.id}><td>{detail.employeeId}</td><td>{money(detail.payableAmount)}</td><td>{money(detail.deductionAmount)}</td><td>{money(detail.actualAmount)}</td><td>{detail.paymentStatus}</td><td><Button variant="secondary" onClick={() => adjust.mutate(detail)}>实发 +100</Button></td></tr>)}</tbody></table></div>
    </Page>
  );
}

