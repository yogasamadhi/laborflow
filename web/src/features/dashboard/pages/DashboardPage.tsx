import { useQuery } from "@tanstack/react-query";
import { getData } from "../../../lib/axios";
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
  const { data } = useQuery({
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
    <Page title="仪表盘">
      <div className="grid grid-3">
        {cards.map(([label, value]) => (
          <div className="card" key={label}>
            <div className="muted">{label}</div>
            <h3>{value}</h3>
          </div>
        ))}
      </div>
    </Page>
  );
}

