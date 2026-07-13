import { useQuery } from "@tanstack/react-query";
import { getData } from "../../../lib/axios";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { money } from "../../../lib/utils";
import { Page } from "../../../components/common/Page";

type Overview = {
  payableAmount: number;
  deductionAmount: number;
  actualAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  paidCount: number;
  unpaidCount: number;
};

export function DashboardPage() {
  const { data, error, isPending, refetch } = useQuery({
    queryKey: ["salary-overview"],
    queryFn: () => getData<Overview>("/statistics/salary/overview")
  });

  const cards = [
    ["本月应发", money(data?.payableAmount)],
    ["本月扣款", money(data?.deductionAmount)],
    ["本月实发", money(data?.actualAmount)],
    ["已发金额", money(data?.paidAmount)],
    ["未发金额", money(data?.unpaidAmount)],
    ["发放进度", `${data?.paidCount ?? 0} / ${(data?.paidCount ?? 0) + (data?.unpaidCount ?? 0)}`]
  ];

  return (
    <Page title="经营概览" description="工资执行与支付情况">
      {isPending ? <RouteLoadingState /> : null}
      {error ? <DataErrorState message={error instanceof Error ? error.message : undefined} onRetry={() => void refetch()} /> : null}
      {!isPending && !error ? (
      <div className="grid grid-3">
        {cards.map(([label, value]) => (
          <div className="card" key={label}>
            <div className="muted">{label}</div>
            <h3>{value}</h3>
          </div>
        ))}
      </div>
      ) : null}
    </Page>
  );
}
