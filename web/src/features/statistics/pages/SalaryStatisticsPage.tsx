import { useQuery } from "@tanstack/react-query";
import * as echarts from "echarts/core";
import { LineChart } from "echarts/charts";
import { GridComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { useEffect, useRef } from "react";
import { Page } from "../../../components/common/Page";
import { getData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

// Register only the pieces this page actually uses, instead of pulling in the
// full echarts bundle (~1MB). Keeps the route chunk well under 200KB.
echarts.use([LineChart, GridComponent, CanvasRenderer]);

type Overview = { payableAmount: number; deductionAmount: number; actualAmount: number; paidAmount: number; unpaidAmount: number; paidCount: number; unpaidCount: number };
type Trend = { salaryMonth: string; actualAmount: number };

export function SalaryStatisticsPage() {
  const chartRef = useRef<HTMLDivElement>(null);
  const { data: overview } = useQuery({ queryKey: ["salary-overview"], queryFn: () => getData<Overview>("/statistics/salary/overview") });
  const { data: trend } = useQuery({ queryKey: ["salary-trend"], queryFn: () => getData<Trend[]>("/statistics/salary/monthly-trend") });
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
      <div className="grid grid-3">
        <div className="card"><div className="muted">应发</div><h3>{money(overview?.payableAmount)}</h3></div>
        <div className="card"><div className="muted">扣款</div><h3>{money(overview?.deductionAmount)}</h3></div>
        <div className="card"><div className="muted">实发</div><h3>{money(overview?.actualAmount)}</h3></div>
      </div>
      <div className="panel" style={{ marginTop: 16 }}><div ref={chartRef} style={{ height: 360 }} /></div>
    </Page>
  );
}

