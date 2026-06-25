import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Link } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";

type Monthly = { id: number; salaryMonth: string; title: string; totalEmployeeCount: number; totalActualAmount: number; status: string };

export function SalaryMonthlyListPage() {
  const queryClient = useQueryClient();
  const { data } = useQuery({ queryKey: ["salary-monthly"], queryFn: () => getData<PageResult<Monthly>>("/salary/monthly", { page: 1, pageSize: 50 }) });
  const action = useMutation({ mutationFn: ({ id, op }: { id: number; op: string }) => postData(`/salary/monthly/${id}/${op}`), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-monthly"] }) });
  return (
    <Page title="月度工资表" actions={<Link to="/salary/monthly/create"><Button><Plus size={16} />创建工资月份</Button></Link>}>
      <div className="panel table-wrap"><table><thead><tr><th>月份</th><th>标题</th><th>人数</th><th>实发</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(monthly => <tr key={monthly.id}><td>{monthly.salaryMonth}</td><td><Link to={`/salary/monthly/${monthly.id}`}>{monthly.title}</Link></td><td>{monthly.totalEmployeeCount}</td><td>{money(monthly.totalActualAmount)}</td><td>{statusText(monthly.status)}</td><td><Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "generate" })}>生成</Button>{" "}<Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "submit" })}>提交</Button>{" "}<Button variant="secondary" onClick={() => action.mutate({ id: monthly.id, op: "approve" })}>审核</Button>{" "}<Link to={`/salary/monthly/${monthly.id}/payment`}><Button>发放</Button></Link></td></tr>)}</tbody></table></div>
    </Page>
  );
}

