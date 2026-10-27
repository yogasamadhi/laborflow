import { useQuery } from "@tanstack/react-query";
import * as echarts from "echarts/core";
import { LineChart } from "echarts/charts";
import { GridComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useRef, useState } from "react";
import { Field, SelectInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { getAllPages, getData } from "../../../lib/axios";
import { money } from "../../../lib/utils";
import type { Department } from "../../department/types";

// Register only the pieces this page actually uses, instead of pulling in the
// full echarts bundle (~1MB). Keeps the route chunk well under 200KB.
echarts.use([LineChart, GridComponent, CanvasRenderer]);

type Overview = { payableAmount: number; deductionAmount: number; actualAmount: number; paidAmount: number; unpaidAmount: number; paidCount: number; unpaidCount: number };
type Trend = { salaryMonth: string; actualAmount: number };
type DepartmentSummary = { departmentId: string; employeeCount: number; actualAmount: number };

export function SalaryStatisticsPage() {
  const chartRef = useRef<HTMLDivElement>(null);
  const [departmentId, setDepartmentId] = useState("");
  const params = departmentId ? { departmentId } : undefined;
  const { data: departmentsData } = useQuery({ queryKey: ["departments", "salary-statistics"], queryFn: () => getAllPages<Department>("/reference/departments") });
  const { data: overview } = useQuery({ queryKey: ["salary-overview", departmentId], queryFn: () => getData<Overview>("/statistics/salary/overview", params) });
  const { data: trend } = useQuery({ queryKey: ["salary-trend", departmentId], queryFn: () => getData<Trend[]>("/statistics/salary/monthly-trend", params) });
  const { data: departmentSummary } = useQuery({ queryKey: ["salary-department-summary", departmentId], queryFn: () => getData<DepartmentSummary[]>("/statistics/salary/department-summary", params) });
  const departments = departmentsData ?? [];
  const departmentMap = new Map(departments.map((department) => [department.id, department.name]));
  useEffect(() => {
    if (!chartRef.current) return;
    const chart = echarts.init(chartRef.current);
    chart.setOption({
      xAxis: { type: "category", data: (trend ?? []).map((item) => item.salaryMonth) },
      yAxis: { type: "value" },
      series: [{ type: "line", data: (trend ?? []).map((item) => item.actualAmount) }]
    });
    return () => chart.dispose();
  }, [trend]);
  return (
    <Page title="工资统计">
      <div className="panel toolbar"><Field label="部门"><SelectInput value={departmentId} onChange={(event) => setDepartmentId(event.target.value)}><option value="">全公司</option>{departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}</SelectInput></Field></div>
      <div className="grid grid-3">
        <div className="card"><div className="muted">应发</div><h3>{money(overview?.payableAmount)}</h3></div>
        <div className="card"><div className="muted">扣款</div><h3>{money(overview?.deductionAmount)}</h3></div>
        <div className="card"><div className="muted">实发</div><h3>{money(overview?.actualAmount)}</h3></div>
      </div>
      <div className="panel" style={{ marginTop: 16 }}><div ref={chartRef} style={{ height: 360 }} /></div>
      <div className="panel table-wrap"><table><thead><tr><th>部门</th><th>人数</th><th>实发金额</th></tr></thead><tbody>{(departmentSummary ?? []).map((row) => <tr key={row.departmentId}><td>{departmentMap.get(String(row.departmentId)) ?? (Number(row.departmentId) === 0 ? "历史全公司" : row.departmentId)}</td><td>{row.employeeCount}</td><td>{money(row.actualAmount)}</td></tr>)}</tbody></table></div>
    </Page>
  );
}
