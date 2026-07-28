import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { DataErrorState, RouteLoadingState } from "../../../components/common/AccessState";
import { Field, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type SalaryOverview = {
  payableAmount: number;
  deductionAmount: number;
  actualAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  paidCount: number;
  unpaidCount: number;
};

type ProjectPortfolio = {
  projectCount: number;
  inProgressCount: number;
  contractAmount: number;
  revenueAmount: number;
  totalCostAmount: number;
  grossProfit: number;
  receivableAmount: number;
  receivedAmount: number;
  riskCount: number;
};

type CompensationOverview = {
  totals: {
    attendanceDeductionAmount: number;
    salaryDeductionAmount: number;
    performanceAmount: number;
    projectBonusAmount: number;
    confirmedScoreCount: number;
  };
  activeProfileCount: number;
  pendingPerformancePlanCount: number;
  draftPerformanceScoreCount: number;
  draftBonusPoolCount: number;
};

type Overview = {
  month: string;
  capabilities: {
    salary: boolean;
    projects: boolean;
    compensation: boolean;
    employeeHandoff: boolean;
  };
  salary?: SalaryOverview;
  projects?: ProjectPortfolio;
  compensation?: CompensationOverview;
  employee?: { message: string; employeeBound: boolean };
};

const currentMonth = new Date().toISOString().slice(0, 7);

function Cards({ rows }: { rows: Array<[string, string | number]> }) {
  return <div className="grid grid-3">{rows.map(([label, value]) => (
    <div className="card" key={label}><div className="muted">{label}</div><h3>{value}</h3></div>
  ))}</div>;
}

export function DashboardPage() {
  const [month, setMonth] = useState(currentMonth);
  const query = useQuery({
    queryKey: ["dashboard-overview", month],
    queryFn: () => getData<Overview>("/dashboard/overview", { month })
  });
  const data = query.data;
  return (
    <Page
      title="综合工作台"
      description="仅展示当前账号获授权的工资、项目和薪酬指标。"
      actions={<Field label="月份"><TextInput type="month" value={month} onChange={event => setMonth(event.target.value)} /></Field>}
    >
      {query.isPending ? <RouteLoadingState /> : null}
      {query.error ? <DataErrorState message={query.error instanceof Error ? query.error.message : undefined} onRetry={() => void query.refetch()} /> : null}
      {data?.capabilities.employeeHandoff ? (
        <div className="panel empty-state">
          <h3>{data.employee?.employeeBound ? "个人业务请在小程序办理" : "当前账号尚未绑定员工"}</h3>
          <p className="muted">{data.employee?.message}</p>
        </div>
      ) : null}
      {data?.salary ? (
        <section>
          <div className="section-heading"><h2>工资执行</h2><Link to="/salary/monthly"><Button variant="secondary">查看工资表</Button></Link></div>
          <Cards rows={[
            ["本月应发", money(data.salary.payableAmount)],
            ["本月扣款", money(data.salary.deductionAmount)],
            ["本月实发", money(data.salary.actualAmount)],
            ["已发金额", money(data.salary.paidAmount)],
            ["未发金额", money(data.salary.unpaidAmount)],
            ["发放进度", `${data.salary.paidCount} / ${data.salary.paidCount + data.salary.unpaidCount}`]
          ]} />
        </section>
      ) : null}
      {data?.projects ? (
        <section>
          <div className="section-heading"><h2>项目经营</h2><Link to="/projects/dashboard"><Button variant="secondary">查看项目看板</Button></Link></div>
          <Cards rows={[
            ["项目数", data.projects.projectCount],
            ["在建项目", data.projects.inProgressCount],
            ["合同额", money(data.projects.contractAmount)],
            ["确认收入", money(data.projects.revenueAmount)],
            ["总成本", money(data.projects.totalCostAmount)],
            ["毛利", money(data.projects.grossProfit)],
            ["应收", money(data.projects.receivableAmount)],
            ["已收", money(data.projects.receivedAmount)],
            ["风险数", data.projects.riskCount]
          ]} />
        </section>
      ) : null}
      {data?.compensation ? (
        <section>
          <div className="section-heading"><h2>薪酬与绩效</h2><Link to="/compensation/plans"><Button variant="secondary">处理绩效</Button></Link></div>
          <Cards rows={[
            ["有效工资档案", data.compensation.activeProfileCount],
            ["待启用绩效方案", data.compensation.pendingPerformancePlanCount],
            ["待确认绩效成绩", data.compensation.draftPerformanceScoreCount],
            ["草稿奖金池", data.compensation.draftBonusPoolCount],
            ["已确认绩效", data.compensation.totals.confirmedScoreCount],
            ["绩效金额", money(data.compensation.totals.performanceAmount)]
          ]} />
        </section>
      ) : null}
    </Page>
  );
}
