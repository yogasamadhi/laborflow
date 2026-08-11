import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { AsyncLookupSelect } from "../../../components/common/AsyncLookupSelect";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Pagination } from "../../../components/common/Pagination";
import { Button } from "../../../components/ui/Button";
import { getAllPages, getData, PageResult, postData, putData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";

type Indicator = {
  id: string;
  name: string;
  category?: string;
  scoreWeight: number;
  dataSource: string;
  metricCode?: string;
  scoringDirection: string;
  targetValue?: number;
  status: string;
};

type Plan = {
  id: string;
  name: string;
  performanceMonth: string;
  projectId?: string;
  positionId?: string;
  status: "draft" | "active" | "closed";
  remark?: string;
  projectName?: string;
};

type PlanIndicator = Indicator & {
  performancePlanId: string;
  indicatorId?: string;
  sortOrder: number;
};

type PlanDetail = { plan: Plan; indicators: PlanIndicator[] };

type Score = {
  id: string;
  performancePlanId: string;
  performanceMonth: string;
  projectId?: string;
  employeeId: string;
  positionId?: string;
  score: number;
  performanceBaseSnapshot: number;
  performanceAmount: number;
  autoMetricSnapshot?: string;
  status: "draft" | "confirmed";
  confirmedAt?: string;
  remark?: string;
  employeeName?: string;
  planName?: string;
  projectName?: string;
};

type ScoreItem = {
  id: string;
  performanceScoreId: string;
  planIndicatorId: string;
  actualValue?: number;
  autoScore?: number;
  finalScore: number;
  overrideReason?: string;
  evidenceSnapshot?: string;
};

type ScoreDetail = { score: Score; items: ScoreItem[] };

const currentMonth = new Date().toISOString().slice(0, 7);

function Status({ value }: { value: string }) {
  const cls = value === "active" || value === "confirmed" ? "badge-green" : value === "closed" ? "badge-red" : "badge-yellow";
  return <span className={`badge ${cls}`}>{statusText(value)}</span>;
}

export function PerformancePlanPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [month, setMonth] = useState(currentMonth);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Partial<Plan> & { indicatorIds: string[] }>({
    performanceMonth: currentMonth,
    indicatorIds: []
  });
  const plans = useQuery({
    queryKey: ["performance-plans", page, month],
    queryFn: () => getData<PageResult<Plan>>("/compensation/plans", { page, pageSize: 20, month: month || undefined })
  });
  const indicators = useQuery({
    queryKey: ["performance-plan-indicators"],
    queryFn: () => getAllPages<Indicator>("/compensation/indicators", { status: "active" })
  });
  const save = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name,
        performanceMonth: form.performanceMonth,
        projectId: form.projectId || null,
        positionId: form.positionId || null,
        indicatorIds: form.indicatorIds,
        remark: form.remark
      };
      return form.id
        ? putData(`/compensation/plans/${form.id}`, payload)
        : postData("/compensation/plans", payload);
    },
    onSuccess: () => {
      setOpen(false);
      setForm({ performanceMonth: currentMonth, indicatorIds: [] });
      queryClient.invalidateQueries({ queryKey: ["performance-plans"] });
    }
  });
  const transition = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "activate" | "close" }) =>
      postData(`/compensation/plans/${id}/${action}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["performance-plans"] })
  });

  async function edit(row: Plan) {
    const detail = await getData<PlanDetail>(`/compensation/plans/${row.id}`);
    setForm({ ...detail.plan, indicatorIds: detail.indicators.map(item => item.indicatorId).filter(Boolean) as string[] });
    setOpen(true);
  }

  function toggleIndicator(id: string) {
    setForm(current => ({
      ...current,
      indicatorIds: current.indicatorIds.includes(id)
        ? current.indicatorIds.filter(value => value !== id)
        : [...current.indicatorIds, id]
    }));
  }

  const totalWeight = (indicators.data ?? [])
    .filter(item => form.indicatorIds.includes(item.id))
    .reduce((sum, item) => sum + Number(item.scoreWeight), 0);
  return (
    <Page title="绩效方案" description="方案启用时固化指标、权重、目标值和数据来源。"
      actions={<Button onClick={() => { setForm({ performanceMonth: month || currentMonth, indicatorIds: [] }); setOpen(true); }}>新建方案</Button>}>
      <div className="panel toolbar">
        <Field label="月份"><TextInput type="month" value={month} onChange={event => { setPage(1); setMonth(event.target.value); }} /></Field>
      </div>
      <div className="panel table-wrap">
        <table>
          <thead><tr><th>方案</th><th>月份</th><th>项目</th><th>岗位</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>{(plans.data?.records ?? []).map(row => <tr key={row.id}>
            <td>{row.name}</td><td>{row.performanceMonth}</td>
            <td>{row.projectId ? row.projectName ?? "已删除项目" : "通用"}</td>
            <td>{row.positionId ? `岗位 #${row.positionId}` : "通用"}</td>
            <td><Status value={row.status} /></td>
            <td><div className="table-actions">
              {row.status === "draft" ? <Button variant="secondary" onClick={() => void edit(row)}>编辑</Button> : null}
              {row.status === "draft" ? <Button onClick={() => transition.mutate({ id: row.id, action: "activate" })}>启用</Button> : null}
              {row.status === "active" ? <Button variant="danger" onClick={() => transition.mutate({ id: row.id, action: "close" })}>关闭</Button> : null}
            </div></td>
          </tr>)}</tbody>
        </table>
      </div>
      <Pagination page={page} pageSize={20} total={Number(plans.data?.total ?? 0)} onPageChange={setPage} />
      <Modal open={open} title={form.id ? "编辑绩效方案" : "新建绩效方案"} onClose={() => setOpen(false)}
        footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button disabled={save.isPending || totalWeight !== 100} onClick={() => save.mutate()}>保存方案</Button></>}>
        <div className="grid grid-2">
          <Field label="方案名称"><TextInput value={form.name ?? ""} onChange={event => setForm({ ...form, name: event.target.value })} /></Field>
          <Field label="月份"><TextInput type="month" value={form.performanceMonth ?? currentMonth} onChange={event => setForm({ ...form, performanceMonth: event.target.value })} /></Field>
          <Field label="项目"><AsyncLookupSelect endpoint="/projects" value={form.projectId} onChange={value => setForm({ ...form, projectId: value || undefined })} emptyLabel="通用方案" /></Field>
          <Field label="岗位"><AsyncLookupSelect endpoint="/reference/positions" value={form.positionId} onChange={value => setForm({ ...form, positionId: value || undefined })} params={{ status: "active" }} emptyLabel="通用岗位" /></Field>
          <Field label="备注"><TextArea value={form.remark ?? ""} onChange={event => setForm({ ...form, remark: event.target.value })} /></Field>
        </div>
        <div className="section-heading"><h3>方案指标</h3><span className={totalWeight === 100 ? "badge badge-green" : "badge badge-red"}>权重 {totalWeight}/100</span></div>
        <div className="panel">
          {(indicators.data ?? []).map(indicator => (
            <label key={indicator.id} className="row">
              <span><input type="checkbox" checked={form.indicatorIds.includes(indicator.id)} onChange={() => toggleIndicator(indicator.id)} /> {indicator.name}</span>
              <span className="muted">{indicator.dataSource} · {indicator.scoreWeight}% · 目标 {indicator.targetValue ?? "-"}</span>
            </label>
          ))}
        </div>
      </Modal>
    </Page>
  );
}

export function PerformanceScoreWorkflowPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [month, setMonth] = useState(currentMonth);
  const [planId, setPlanId] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [selectedId, setSelectedId] = useState("");
  const [draftItems, setDraftItems] = useState<Record<string, { finalScore: number; overrideReason: string }>>({});
  const [remark, setRemark] = useState("");
  const scores = useQuery({
    queryKey: ["performance-scores", page, month],
    queryFn: () => getData<PageResult<Score>>("/compensation/scores", { page, pageSize: 20, month })
  });
  const plans = useQuery({
    queryKey: ["active-performance-plans", month],
    queryFn: () => getAllPages<Plan>("/compensation/plans", { month })
  });
  const detail = useQuery({
    queryKey: ["performance-score-detail", selectedId],
    queryFn: () => getData<ScoreDetail>(`/compensation/scores/${selectedId}`),
    enabled: Boolean(selectedId)
  });
  const planDetail = useQuery({
    queryKey: ["performance-plan-detail", detail.data?.score.performancePlanId],
    queryFn: () => getData<PlanDetail>(`/compensation/plans/${detail.data?.score.performancePlanId}`),
    enabled: Boolean(detail.data?.score.performancePlanId)
  });
  useEffect(() => {
    if (!detail.data) return;
    setDraftItems(Object.fromEntries(detail.data.items.map(item => [item.planIndicatorId, {
      finalScore: Number(item.finalScore),
      overrideReason: item.overrideReason ?? ""
    }])));
    setRemark(detail.data.score.remark ?? "");
  }, [detail.data]);
  const indicators = useMemo(() => new Map((planDetail.data?.indicators ?? []).map(item => [item.id, item])), [planDetail.data]);
  const generate = useMutation({
    mutationFn: () => postData(`/compensation/plans/${planId}/scores/generate`, { employeeIds: [employeeId] }),
    onSuccess: () => {
      setEmployeeId("");
      queryClient.invalidateQueries({ queryKey: ["performance-scores"] });
    }
  });
  const save = useMutation({
    mutationFn: () => putData(`/compensation/scores/${selectedId}`, {
      items: Object.entries(draftItems).map(([planIndicatorId, value]) => ({ planIndicatorId, ...value })),
      remark
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["performance-score-detail", selectedId] });
      queryClient.invalidateQueries({ queryKey: ["performance-scores"] });
    }
  });
  const refresh = useMutation({
    mutationFn: () => postData(`/compensation/scores/${selectedId}/refresh`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["performance-score-detail", selectedId] })
  });
  const confirm = useMutation({
    mutationFn: () => postData(`/compensation/scores/${selectedId}/confirm`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["performance-score-detail", selectedId] });
      queryClient.invalidateQueries({ queryKey: ["performance-scores"] });
    }
  });
  return (
    <Page title="绩效评分" description="自动指标保留来源证据；绩效金额由绩效基数和加权总分计算。">
      <div className="panel toolbar">
        <Field label="月份"><TextInput type="month" value={month} onChange={event => { setMonth(event.target.value); setPage(1); setPlanId(""); }} /></Field>
        <Field label="启用方案"><SelectInput value={planId} onChange={event => setPlanId(event.target.value)}>
          <option value="">请选择</option>
          {(plans.data ?? []).filter(item => item.status === "active").map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </SelectInput></Field>
        <Field label="员工"><AsyncLookupSelect endpoint="/reference/employees" value={employeeId} onChange={setEmployeeId} params={{ status: "active" }} allowEmpty={false} /></Field>
        <Button disabled={!planId || !employeeId || generate.isPending} onClick={() => generate.mutate()}>生成评分</Button>
      </div>
      <div className="panel table-wrap">
        <table><thead><tr><th>员工</th><th>月份</th><th>方案</th><th>总分</th><th>绩效基数</th><th>绩效金额</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>{(scores.data?.records ?? []).map(row => <tr key={row.id}>
            <td>{row.employeeName ?? "已删除员工"}</td><td>{row.performanceMonth}</td><td>{row.planName ?? "已删除方案"}</td>
            <td>{row.score}</td><td>{money(row.performanceBaseSnapshot)}</td><td>{money(row.performanceAmount)}</td>
            <td><Status value={row.status} /></td><td><Button variant="secondary" onClick={() => setSelectedId(row.id)}>查看评分</Button></td>
          </tr>)}</tbody>
        </table>
      </div>
      <Pagination page={page} pageSize={20} total={Number(scores.data?.total ?? 0)} onPageChange={setPage} />
      <Modal open={Boolean(selectedId)} title="绩效评分明细" onClose={() => setSelectedId("")}
        footer={detail.data?.score.status === "draft" ? <>
          <Button variant="secondary" disabled={refresh.isPending} onClick={() => refresh.mutate()}>刷新自动指标</Button>
          <Button variant="secondary" disabled={save.isPending} onClick={() => save.mutate()}>保存草稿</Button>
          <Button disabled={confirm.isPending} onClick={() => confirm.mutate()}>确认评分</Button>
        </> : <Button onClick={() => setSelectedId("")}>关闭</Button>}>
        {detail.data ? <>
          <div className="grid grid-3">
            <div className="card"><span className="muted">加权总分</span><h3>{detail.data.score.score}</h3></div>
            <div className="card"><span className="muted">绩效基数</span><h3>{money(detail.data.score.performanceBaseSnapshot)}</h3></div>
            <div className="card"><span className="muted">绩效金额</span><h3>{money(detail.data.score.performanceAmount)}</h3></div>
          </div>
          {(detail.data.items ?? []).map(item => {
            const indicator = indicators.get(item.planIndicatorId);
            const draft = draftItems[item.planIndicatorId] ?? { finalScore: Number(item.finalScore), overrideReason: "" };
            return <div className="panel" key={item.id}>
              <div className="section-heading"><h3>{indicator?.name ?? `指标 #${item.planIndicatorId}`}</h3><span className="muted">权重 {indicator?.scoreWeight ?? "-"}%</span></div>
              <div className="grid grid-2">
                <div><span className="muted">实际值</span><div>{item.actualValue ?? "手工录入"}</div></div>
                <div><span className="muted">自动分</span><div>{item.autoScore ?? "-"}</div></div>
                <Field label="最终分"><TextInput type="number" min={0} max={100} step="0.01" disabled={detail.data.score.status !== "draft"} value={draft.finalScore} onChange={event => setDraftItems({ ...draftItems, [item.planIndicatorId]: { ...draft, finalScore: Number(event.target.value) } })} /></Field>
                {item.autoScore !== undefined && Number(item.autoScore) !== draft.finalScore ? <Field label="覆盖原因"><TextInput disabled={detail.data.score.status !== "draft"} value={draft.overrideReason} onChange={event => setDraftItems({ ...draftItems, [item.planIndicatorId]: { ...draft, overrideReason: event.target.value } })} /></Field> : null}
              </div>
              <p className="muted">{item.evidenceSnapshot ?? "无自动数据证据"}</p>
            </div>;
          })}
          <Field label="评语"><TextArea disabled={detail.data.score.status !== "draft"} value={remark} onChange={event => setRemark(event.target.value)} /></Field>
          {detail.data.score.autoMetricSnapshot ? <p className="muted">确认快照：{detail.data.score.autoMetricSnapshot}</p> : null}
        </> : <div className="empty-state">正在加载评分明细…</div>}
      </Modal>
    </Page>
  );
}
