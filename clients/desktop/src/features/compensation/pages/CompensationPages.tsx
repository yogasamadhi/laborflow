import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { getAllPages, getData, postData, putData } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";
import type { Employee } from "../../employee/types";

type Project = { id: string; name: string };
type Position = { id: string; name: string };
type Attendance = { id: string; employeeId: string; attendanceMonth: string; projectId?: string; workDays: number; leaveDays: number; overtimeDays: number; absentDays: number; fieldDays: number; projectSiteDays: number; deductionAmount: number };
type SalaryDeduction = { id: string; employeeId: string; deductionMonth: string; deductionType: string; salaryItemId?: string; salaryItemName: string; amount: number; status: string; remark?: string };
type Indicator = { id: string; positionId?: string; name: string; category?: string; scoreWeight: number; dataSource: string; metricCode?: string; scoringDirection: string; targetValue?: number; status: string; remark?: string };
type BonusPool = { id: string; projectId: string; bonusMonth?: string; finalGrossProfit: number; bonusRatio: number; bonusPoolAmount: number; releaseCondition?: string; releaseStatus: string; dashboardSnapshot?: string };
type BonusAllocation = { id: string; bonusPoolId: string; projectId: string; employeeId: string; roleName?: string; allocationRatio: number; allocationAmount: number; paymentMonth: string; status: string };
type Dashboard = { month?: string; attendanceDeductionAmount: number; salaryDeductionAmount: number; performanceAmount: number; projectBonusAmount: number; salaryActualAmount: number; scoredEmployeeCount: number; confirmedScoreCount: number; bonusAllocationCount: number };

function useCompLookups() {
  const employees = useQuery({ queryKey: ["comp-employees"], queryFn: () => getAllPages<Employee>("/reference/employees", { status: "active" }) });
  const projects = useQuery({ queryKey: ["comp-projects"], queryFn: () => getAllPages<Project>("/projects") });
  const positions = useQuery({ queryKey: ["comp-positions"], queryFn: () => getAllPages<Position>("/reference/positions", { status: "active" }) });
  const pools = useQuery({ queryKey: ["comp-bonus-pools"], queryFn: () => getAllPages<BonusPool>("/compensation/bonus-pools", { status: "released" }) });
  return {
    employees: employees.data ?? [],
    projects: projects.data ?? [],
    positions: positions.data ?? [],
    pools: pools.data ?? []
  };
}

function nameOf<T extends { id: string; name: string }>(rows: T[], id?: string) {
  return rows.find(row => row.id === id)?.name ?? id ?? "-";
}

function Badge({ status }: { status?: string }) {
  const cls = status === "confirmed" || status === "released" || status === "active" ? "badge-green" : status === "rejected" ? "badge-red" : "badge-yellow";
  return <span className={`badge ${cls}`}>{statusText(status)}</span>;
}

const thisMonth = new Date().toISOString().slice(0, 7);

const deductionTypes = [
  { value: "social_security", label: "社保扣款" },
  { value: "housing_fund", label: "公积金扣款" },
  { value: "individual_tax", label: "个税扣款" },
  { value: "advance", label: "借支扣款" },
  { value: "other", label: "其他扣款" }
];

function deductionTypeText(type?: string) {
  return deductionTypes.find(item => item.value === type)?.label ?? type ?? "-";
}

export function AttendancePage() {
  const queryClient = useQueryClient();
  const lookups = useCompLookups();
  const [form, setForm] = useState<Partial<Attendance>>({ attendanceMonth: thisMonth, workDays: 0, leaveDays: 0, overtimeDays: 0, absentDays: 0, fieldDays: 0, projectSiteDays: 0, deductionAmount: 0 });
  const listing = usePagedQuery<Attendance>(["attendance-records"], "/compensation/attendance");
  const data = listing.data;
  const save = useMutation({ mutationFn: () => form.id ? putData(`/compensation/attendance/${form.id}`, form) : postData("/compensation/attendance", form), onSuccess: () => { setForm({ attendanceMonth: thisMonth, workDays: 0, leaveDays: 0, overtimeDays: 0, absentDays: 0, fieldDays: 0, projectSiteDays: 0, deductionAmount: 0 }); queryClient.invalidateQueries({ queryKey: ["attendance-records"] }); } });
  return <Page title="考勤管理">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}>
      <Field label="员工"><SelectInput value={form.employeeId ?? ""} onChange={e => setForm({ ...form, employeeId: e.target.value })}>{lookups.employees.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}</SelectInput></Field>
      <Field label="月份"><TextInput type="month" value={form.attendanceMonth ?? thisMonth} onChange={e => setForm({ ...form, attendanceMonth: e.target.value })} /></Field>
      <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value || undefined })}><option value="">无</option>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      {(["workDays", "leaveDays", "overtimeDays", "absentDays", "fieldDays", "projectSiteDays"] as const).map(key => <Field key={key} label={{ workDays: "出勤", leaveDays: "请假", overtimeDays: "加班", absentDays: "缺勤", fieldDays: "外勤", projectSiteDays: "驻场" }[key]}><TextInput type="number" value={form[key] ?? 0} onChange={e => setForm({ ...form, [key]: Number(e.target.value) })} /></Field>)}
      <Field label="扣款"><TextInput type="number" value={form.deductionAmount ?? 0} onChange={e => setForm({ ...form, deductionAmount: Number(e.target.value) })} /></Field>
      <Button type="submit">{form.id ? "保存" : "录入"}</Button>
    </form>
    <div className="panel table-wrap"><table><thead><tr><th>员工</th><th>月份</th><th>出勤</th><th>请假</th><th>加班</th><th>缺勤</th><th>驻场</th><th>扣款</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.employees, row.employeeId)}</td><td>{row.attendanceMonth}</td><td>{row.workDays}</td><td>{row.leaveDays}</td><td>{row.overtimeDays}</td><td>{row.absentDays}</td><td>{row.projectSiteDays}</td><td>{money(row.deductionAmount)}</td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button></td></tr>)}</tbody></table></div>
    {listing.pagination}
  </Page>;
}

export function SalaryDeductionPage() {
  const queryClient = useQueryClient();
  const { employees } = useCompLookups();
  const [form, setForm] = useState<Partial<SalaryDeduction>>({ deductionMonth: thisMonth, deductionType: "social_security", amount: 0, status: "active" });
  const listing = usePagedQuery<SalaryDeduction>(["salary-deductions"], "/compensation/deductions");
  const data = listing.data;
  const save = useMutation({ mutationFn: () => form.id ? putData(`/compensation/deductions/${form.id}`, form) : postData("/compensation/deductions", form), onSuccess: () => { setForm({ deductionMonth: thisMonth, deductionType: "social_security", amount: 0, status: "active" }); queryClient.invalidateQueries({ queryKey: ["salary-deductions"] }); } });
  const remove = useMutation({ mutationFn: (id: string) => postData(`/compensation/deductions/${id}/delete`), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-deductions"] }) });
  return <Page title="工资扣款">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}>
      <Field label="员工"><SelectInput value={form.employeeId ?? ""} onChange={e => setForm({ ...form, employeeId: e.target.value })}>{employees.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}</SelectInput></Field>
      <Field label="月份"><TextInput type="month" value={form.deductionMonth ?? thisMonth} onChange={e => setForm({ ...form, deductionMonth: e.target.value })} /></Field>
      <Field label="类型"><SelectInput value={form.deductionType ?? "other"} onChange={e => setForm({ ...form, deductionType: e.target.value })}>{deductionTypes.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</SelectInput></Field>
      <Field label="金额"><TextInput type="number" step="0.01" value={form.amount ?? 0} onChange={e => setForm({ ...form, amount: Number(e.target.value) })} /></Field>
      <Field label="名称"><TextInput value={form.salaryItemName ?? ""} onChange={e => setForm({ ...form, salaryItemName: e.target.value })} placeholder="默认按类型生成" /></Field>
      <Field label="状态"><SelectInput value={form.status ?? "active"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="active">生效</option><option value="inactive">停用</option></SelectInput></Field>
      <Field label="备注"><TextInput value={form.remark ?? ""} onChange={e => setForm({ ...form, remark: e.target.value })} /></Field>
      <Button type="submit" disabled={save.isPending}>{form.id ? "保存" : "录入"}</Button>
    </form>
    <div className="panel table-wrap"><table><thead><tr><th>员工</th><th>月份</th><th>类型</th><th>名称</th><th>金额</th><th>状态</th><th>备注</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(employees, row.employeeId)}</td><td>{row.deductionMonth}</td><td>{deductionTypeText(row.deductionType)}</td><td>{row.salaryItemName}</td><td>{money(row.amount)}</td><td><Badge status={row.status} /></td><td>{row.remark ?? "-"}</td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button>{" "}<Button variant="danger" onClick={() => remove.mutate(row.id)} disabled={remove.isPending}>删除</Button></td></tr>)}</tbody></table></div>
    {listing.pagination}
  </Page>;
}

export function PerformanceIndicatorPage() {
  const queryClient = useQueryClient();
  const { positions } = useCompLookups();
  const emptyIndicator: Partial<Indicator> = { scoreWeight: 0, dataSource: "manual", scoringDirection: "higher_better", status: "active" };
  const [form, setForm] = useState<Partial<Indicator>>(emptyIndicator);
  const listing = usePagedQuery<Indicator>(["performance-indicators"], "/compensation/indicators");
  const data = listing.data;
  const save = useMutation({ mutationFn: () => form.id ? putData(`/compensation/indicators/${form.id}`, form) : postData("/compensation/indicators", form), onSuccess: () => { setForm(emptyIndicator); queryClient.invalidateQueries({ queryKey: ["performance-indicators"] }); } });
  return <Page title="绩效指标">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}>
      <Field label="岗位"><SelectInput value={form.positionId ?? ""} onChange={e => setForm({ ...form, positionId: e.target.value || undefined })}><option value="">通用</option>{positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      <Field label="指标名称"><TextInput value={form.name ?? ""} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
      <Field label="分类"><TextInput value={form.category ?? ""} onChange={e => setForm({ ...form, category: e.target.value })} /></Field>
      <Field label="权重"><TextInput type="number" min={0} max={100} step="0.01" value={form.scoreWeight ?? 0} onChange={e => setForm({ ...form, scoreWeight: Number(e.target.value) })} /></Field>
      <Field label="来源"><SelectInput value={form.dataSource ?? "manual"} onChange={e => setForm({ ...form, dataSource: e.target.value, metricCode: e.target.value === "manual" ? undefined : form.metricCode })}><option value="manual">手工</option><option value="project">项目经营</option><option value="attendance">考勤</option><option value="salary">工资</option></SelectInput></Field>
      {form.dataSource !== "manual" ? <Field label="指标编码"><SelectInput value={form.metricCode ?? ""} onChange={e => setForm({ ...form, metricCode: e.target.value })}><option value="">请选择</option>
        {[
          ["project_profit_rate", "利润率"], ["project_payment_progress", "回款进度"], ["project_cost_rate", "成本率"], ["project_risk_count", "风险数量"],
          ["attendance_work_days", "出勤天数"], ["attendance_absent_days", "缺勤天数"], ["attendance_leave_days", "请假天数"], ["attendance_project_site_days", "项目现场天数"],
          ["salary_payment_completed", "发放完成状态"], ["salary_payslip_no_exception", "工资条无异议状态"]
        ].map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </SelectInput></Field> : null}
      <Field label="评分方向"><SelectInput value={form.scoringDirection ?? "higher_better"} onChange={e => setForm({ ...form, scoringDirection: e.target.value })}><option value="higher_better">越高越好</option><option value="lower_better">越低越好</option></SelectInput></Field>
      <Field label="目标值"><TextInput type="number" min={0} step="0.01" value={form.targetValue ?? 0} onChange={e => setForm({ ...form, targetValue: Number(e.target.value) })} /></Field>
      <Field label="状态"><SelectInput value={form.status ?? "active"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="active">启用</option><option value="inactive">停用</option></SelectInput></Field>
      <Button type="submit" disabled={save.isPending}>保存</Button>
    </form>
    <div className="panel table-wrap"><table><thead><tr><th>岗位</th><th>指标</th><th>分类</th><th>权重</th><th>来源</th><th>编码</th><th>方向</th><th>目标</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(positions, row.positionId)}</td><td>{row.name}</td><td>{row.category ?? "-"}</td><td>{row.scoreWeight}</td><td>{row.dataSource}</td><td>{row.metricCode ?? "-"}</td><td>{row.scoringDirection === "lower_better" ? "越低越好" : "越高越好"}</td><td>{row.targetValue ?? "-"}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button></td></tr>)}</tbody></table></div>
    {listing.pagination}
  </Page>;
}

export function BonusPoolPage() {
  const queryClient = useQueryClient();
  const { projects } = useCompLookups();
  const [form, setForm] = useState<Partial<BonusPool>>({ bonusMonth: thisMonth, bonusRatio: 0, finalGrossProfit: 0, releaseStatus: "draft" });
  const listing = usePagedQuery<BonusPool>(["bonus-pools"], "/compensation/bonus-pools");
  const data = listing.data;
  const save = useMutation({ mutationFn: () => form.id ? putData(`/compensation/bonus-pools/${form.id}`, form) : postData("/compensation/bonus-pools", form), onSuccess: () => { setForm({ bonusMonth: thisMonth, bonusRatio: 0, finalGrossProfit: 0, releaseStatus: "draft" }); queryClient.invalidateQueries({ queryKey: ["bonus-pools"] }); queryClient.invalidateQueries({ queryKey: ["comp-bonus-pools"] }); } });
  const release = useMutation({ mutationFn: (id: string) => postData(`/compensation/bonus-pools/${id}/release`), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["bonus-pools"] }) });
  return <Page title="项目奖金池">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}><Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="月份"><TextInput type="month" value={form.bonusMonth ?? thisMonth} onChange={e => setForm({ ...form, bonusMonth: e.target.value })} /></Field><Field label="最终毛利"><TextInput type="number" value={form.finalGrossProfit ?? 0} onChange={e => setForm({ ...form, finalGrossProfit: Number(e.target.value) })} /></Field><Field label="奖金比例"><TextInput type="number" step="0.01" value={form.bonusRatio ?? 0} onChange={e => setForm({ ...form, bonusRatio: Number(e.target.value) })} /></Field><Field label="发放条件"><TextInput value={form.releaseCondition ?? ""} onChange={e => setForm({ ...form, releaseCondition: e.target.value })} /></Field><Button type="submit">保存奖金池</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>月份</th><th>最终毛利</th><th>比例</th><th>奖金池</th><th>经营快照</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(projects, row.projectId)}</td><td>{row.bonusMonth ?? "-"}</td><td>{money(row.finalGrossProfit)}</td><td>{row.bonusRatio}</td><td>{money(row.bonusPoolAmount)}</td><td>{row.dashboardSnapshot ?? "-"}</td><td><Badge status={row.releaseStatus} /></td><td>{row.releaseStatus === "draft" ? <><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button>{" "}<Button onClick={() => release.mutate(row.id)}>发布</Button></> : "-"}</td></tr>)}</tbody></table></div>
    {listing.pagination}
  </Page>;
}

export function BonusAllocationPage() {
  const queryClient = useQueryClient();
  const lookups = useCompLookups();
  const [form, setForm] = useState<Partial<BonusAllocation>>({ paymentMonth: thisMonth, allocationRatio: 0, allocationAmount: 0, status: "allocated" });
  const listing = usePagedQuery<BonusAllocation>(["bonus-allocations"], "/compensation/bonus-allocations");
  const data = listing.data;
  const save = useMutation({ mutationFn: () => form.id ? putData(`/compensation/bonus-allocations/${form.id}`, form) : postData("/compensation/bonus-allocations", form), onSuccess: () => { setForm({ paymentMonth: thisMonth, allocationRatio: 0, allocationAmount: 0, status: "allocated" }); queryClient.invalidateQueries({ queryKey: ["bonus-allocations"] }); } });
  const confirm = useMutation({ mutationFn: (id: string) => postData(`/compensation/bonus-allocations/${id}/confirm`), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["bonus-allocations"] }) });
  return <Page title="奖金分配">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}><Field label="奖金池"><SelectInput value={form.bonusPoolId ?? ""} onChange={e => setForm({ ...form, bonusPoolId: e.target.value })}>{lookups.pools.map(p => <option key={p.id} value={p.id}>{nameOf(lookups.projects, p.projectId)} / {money(p.bonusPoolAmount)}</option>)}</SelectInput></Field><Field label="员工"><SelectInput value={form.employeeId ?? ""} onChange={e => setForm({ ...form, employeeId: e.target.value })}>{lookups.employees.map(e => <option key={e.id} value={e.id}>{e.name}</option>)}</SelectInput></Field><Field label="角色"><SelectInput value={form.roleName ?? "项目经理"} onChange={e => setForm({ ...form, roleName: e.target.value })}><option value="项目经理">项目经理</option><option value="施工负责人">施工负责人</option><option value="商务/资料">商务/资料</option><option value="财务">财务</option><option value="其他支持人员">其他支持人员</option></SelectInput></Field><Field label="比例"><TextInput type="number" step="0.01" value={form.allocationRatio ?? 0} onChange={e => setForm({ ...form, allocationRatio: Number(e.target.value) })} /></Field><Field label="金额"><TextInput type="number" value={form.allocationAmount ?? 0} onChange={e => setForm({ ...form, allocationAmount: Number(e.target.value) })} /></Field><Field label="发放月份"><TextInput type="month" value={form.paymentMonth ?? thisMonth} onChange={e => setForm({ ...form, paymentMonth: e.target.value })} /></Field><Button type="submit">保存分配</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>员工</th><th>角色</th><th>比例</th><th>金额</th><th>发放月份</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.employees, row.employeeId)}</td><td>{row.roleName ?? "-"}</td><td>{row.allocationRatio}</td><td>{money(row.allocationAmount)}</td><td>{row.paymentMonth}</td><td><Badge status={row.status} /></td><td>{row.status === "allocated" ? <><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button>{" "}<Button onClick={() => confirm.mutate(row.id)}>确认</Button></> : "-"}</td></tr>)}</tbody></table></div>
    {listing.pagination}
  </Page>;
}

export function CompensationSalaryPage() {
  return <Page title="工资表"><div className="panel"><Link to="/salary/monthly"><Button>进入月度工资表</Button></Link></div></Page>;
}

export function CompensationDashboardPage() {
  const [month, setMonth] = useState(thisMonth);
  const { data } = useQuery({ queryKey: ["compensation-dashboard", month], queryFn: () => getData<Dashboard>("/compensation/dashboard", { month }) });
  const cards = [
    ["考勤扣款", money(data?.attendanceDeductionAmount)],
    ["工资扣款", money(data?.salaryDeductionAmount)],
    ["绩效工资", money(data?.performanceAmount)],
    ["项目奖金", money(data?.projectBonusAmount)],
    ["工资实发", money(data?.salaryActualAmount)],
    ["评分人数", data?.scoredEmployeeCount ?? 0],
    ["已确认评分", data?.confirmedScoreCount ?? 0],
    ["奖金分配数", data?.bonusAllocationCount ?? 0]
  ];
  return <Page title="薪酬绩效看板"><div className="panel toolbar"><Field label="月份"><TextInput type="month" value={month} onChange={e => setMonth(e.target.value)} /></Field></div><div className="grid grid-3">{cards.map(([label, value]) => <div className="card" key={label}><div className="muted">{label}</div><h3>{value}</h3></div>)}</div></Page>;
}
