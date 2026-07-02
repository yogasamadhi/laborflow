import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useMemo, useState } from "react";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData, putData, uploadFile } from "../../../lib/axios";
import { money, statusText } from "../../../lib/utils";

type Project = { id: string; name: string; clientName?: string; contractAmount?: number; status?: string; projectManager?: string; address?: string; scope?: string; remark?: string };
type Section = { id: string; projectId: string; name: string; bidSectionName?: string; startStakeNo?: string; endStakeNo?: string; startWellNo?: string; endWellNo?: string; length?: number; remark?: string };
type Process = { id: string; name: string; unit: string; sortOrder?: number; status?: string };
type Team = { id: string; name: string; leaderName?: string; phone?: string; workType?: string; settlementMethod?: string; status?: string };
type DailyReport = { id: string; workDate: string; projectId: string; projectSectionId: string; contractItemId?: string; workProcessId: string; completedQuantity: number; unit: string; outputValue?: number; pricingBasis?: string; teamId?: string; workerCount?: number; machineryUsage?: string; reporterName?: string; status: string; exceptionNote?: string; rejectedReason?: string };
type TeamUnitPrice = { id: string; projectId: string; teamId: string; workProcessId: string; unit: string; unitPrice: number; effectiveDate?: string; status?: string };
type ProcessPrice = { id: string; projectId: string; workProcessId: string; unit: string; unitPrice: number; effectiveDate?: string; status?: string };
type ContractItem = { id: string; projectId: string; projectSectionId?: string; workProcessId: string; itemCode?: string; itemName: string; unit: string; contractQuantity: number; unitPrice: number; contractAmount: number; status?: string; remark?: string };
type Settlement = { id: string; projectId: string; teamId: string; projectName?: string; teamName?: string; settlementMonth: string; confirmedQuantity: number; settlementAmount: number; compensationAmount: number; deductionAmount: number; actualPayAmount: number; settlementStatus: string; paymentStatus: string; rejectedReason?: string };
type Variation = { id: string; projectId: string; projectSectionId?: string; occurrenceDate: string; variationType: string; reason?: string; responsibleParty?: string; increasedQuantity?: number; estimatedAmount?: number; confirmedAmount?: number; confirmRemark?: string; laborCount?: number; status: string; rejectedReason?: string };
type CostRecord = { id: string; projectId: string; costDate: string; costType: string; amount: number; description?: string };
type PaymentRecord = { id: string; projectId: string; recordType: string; paymentDate: string; amount: number; payerName?: string; invoiceNo?: string; invoiceDate?: string; retentionDueDate?: string; voucherFileId?: string; remark?: string };
type MachineryEquipment = { id: string; name: string; equipmentType: string; ownerName?: string; unitPrice: number; status?: string };
type MachineryWorkRecord = { id: string; projectId: string; projectSectionId?: string; machineryEquipmentId: string; workDate: string; shiftCount: number; unitPrice: number; amount: number; idleReason?: string; operatorName?: string };
type MaterialRecord = { id: string; projectId: string; projectSectionId?: string; materialName: string; recordType: string; recordDate: string; quantity: number; unit?: string; unitPrice: number; amount: number; supplierName?: string; impactNote?: string };
type DocumentArchive = { id: string; projectId: string; projectSectionId?: string; workProcessId?: string; archiveDate: string; documentType: string; fileId: string; uploaderName?: string; remark?: string };
type FinanceBreakdown = { category: string; amount: number; ratio: number; source: string };
type Dashboard = { projectName: string; contractAmount: number; completedOutputValue: number; confirmedQuantity: number; revenueAmount: number; teamPayableAmount: number; teamPaidAmount: number; salaryAllocatedCostAmount: number; otherCostAmount: number; machineryCostAmount: number; materialCostAmount: number; totalCostAmount: number; grossProfit: number; profitRate: number; costRate: number; receivableAmount: number; invoicedAmount: number; confirmedVariationAmount: number; pendingVariationAmount: number; receivedAmount: number; cashInAmount: number; cashOutAmount: number; cashBalanceAmount: number; retentionAmount: number; retentionReturnedAmount: number; retentionBalanceAmount: number; paymentProgress: number; revenueBreakdowns: FinanceBreakdown[]; costBreakdowns: FinanceBreakdown[]; cashFlowBreakdowns: FinanceBreakdown[]; risks: { type: string; level: string; message: string }[] };
type ApprovalRecord = { id: string; confirmStage: string; confirmStatus: string; confirmerName?: string; statusBefore?: string; statusAfter?: string; remark?: string; createdAt?: string };
type EvidenceFile = { id: string; fileId: string; fileType: string; createdAt?: string };
type EvidenceTarget = { type: "daily" | "variation"; id: string; title: string };

function useLookups() {
  const projects = useQuery({ queryKey: ["project-lookup"], queryFn: () => getData<PageResult<Project>>("/projects", { page: 1, pageSize: 500 }) });
  const sections = useQuery({ queryKey: ["section-lookup"], queryFn: () => getData<PageResult<Section>>("/project-sections", { page: 1, pageSize: 500 }) });
  const processes = useQuery({ queryKey: ["process-lookup"], queryFn: () => getData<PageResult<Process>>("/work-processes", { page: 1, pageSize: 500 }) });
  const teams = useQuery({ queryKey: ["team-lookup"], queryFn: () => getData<PageResult<Team>>("/teams", { page: 1, pageSize: 500 }) });
  const machinery = useQuery({ queryKey: ["machinery-lookup"], queryFn: () => getData<PageResult<MachineryEquipment>>("/machinery-equipments", { page: 1, pageSize: 500 }) });
  return {
    projects: projects.data?.records ?? [],
    sections: sections.data?.records ?? [],
    processes: processes.data?.records ?? [],
    teams: teams.data?.records ?? [],
    machinery: machinery.data?.records ?? []
  };
}

function nameOf<T extends { id: string; name: string }>(rows: T[], id?: string) {
  return rows.find((item) => item.id === id)?.name ?? id ?? "-";
}

function contractItemText(item?: ContractItem) {
  if (!item) return "-";
  return item.itemCode ? `${item.itemCode} / ${item.itemName}` : item.itemName;
}

function percent(value?: number | string | null) {
  return `${Number(value ?? 0).toFixed(1)}%`;
}

function Badge({ status }: { status?: string }) {
  const cls = status === "confirmed" || status === "active" || status === "paid" ? "badge-green" : status === "rejected" ? "badge-red" : "badge-yellow";
  return <span className={`badge ${cls}`}>{statusText(status)}</span>;
}

export function ProjectLedgerPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<Project>>({});
  const [open, setOpen] = useState(false);
  const { data } = useQuery({ queryKey: ["projects"], queryFn: () => getData<PageResult<Project>>("/projects", { page: 1, pageSize: 100 }) });
  const save = useMutation({
    mutationFn: () => form.id ? putData(`/projects/${form.id}`, form) : postData("/projects", form),
    onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["projects"] }); queryClient.invalidateQueries({ queryKey: ["project-lookup"] }); }
  });
  const rows = data?.records ?? [];
  return (
    <Page title="项目台账" actions={<Button onClick={() => { setForm({ status: "not_started", contractAmount: 0 }); setOpen(true); }}>新增项目</Button>}>
      <div className="panel table-wrap"><table><thead><tr><th>项目名称</th><th>甲方/总包</th><th>合同金额</th><th>项目经理</th><th>状态</th><th>操作</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td>{row.name}</td><td>{row.clientName ?? "-"}</td><td>{money(row.contractAmount)}</td><td>{row.projectManager ?? "-"}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button></td></tr>)}</tbody></table></div>
      <Modal open={open} title={form.id ? "编辑项目" : "新增项目"} onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
        <div className="grid grid-2">
          <Field label="项目名称"><TextInput value={form.name ?? ""} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="甲方/总包"><TextInput value={form.clientName ?? ""} onChange={e => setForm({ ...form, clientName: e.target.value })} /></Field>
          <Field label="合同金额"><TextInput type="number" value={form.contractAmount ?? 0} onChange={e => setForm({ ...form, contractAmount: Number(e.target.value) })} /></Field>
          <Field label="项目经理"><TextInput value={form.projectManager ?? ""} onChange={e => setForm({ ...form, projectManager: e.target.value })} /></Field>
          <Field label="项目状态"><SelectInput value={form.status ?? "not_started"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="not_started">未开工</option><option value="in_progress">施工中</option><option value="paused">暂停</option><option value="completed">已完工</option><option value="settled">已结算</option></SelectInput></Field>
          <Field label="地址"><TextInput value={form.address ?? ""} onChange={e => setForm({ ...form, address: e.target.value })} /></Field>
        </div>
        <Field label="施工范围"><TextArea value={form.scope ?? ""} onChange={e => setForm({ ...form, scope: e.target.value })} /></Field>
      </Modal>
    </Page>
  );
}

export function ProjectSectionPage() {
  const queryClient = useQueryClient();
  const { projects } = useLookups();
  const [projectId, setProjectId] = useState("");
  const [form, setForm] = useState<Partial<Section>>({});
  const [open, setOpen] = useState(false);
  const { data } = useQuery({ queryKey: ["project-sections", projectId], queryFn: () => getData<PageResult<Section>>("/project-sections", { page: 1, pageSize: 200, filterProjectId: projectId || undefined }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/project-sections/${form.id}`, form) : postData("/project-sections", form), onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["project-sections"] }); queryClient.invalidateQueries({ queryKey: ["section-lookup"] }); } });
  return (
    <Page title="施工段管理" actions={<Button onClick={() => { setForm({ projectId: projectId || projects[0]?.id, length: 0 }); setOpen(true); }}>新增施工段</Button>}>
      <div className="panel toolbar"><Field label="项目"><SelectInput value={projectId} onChange={e => setProjectId(e.target.value)}><option value="">全部项目</option>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field></div>
      <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>施工段</th><th>起止桩号</th><th>起止井号</th><th>长度</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(projects, row.projectId)}</td><td>{row.name}</td><td>{row.startStakeNo ?? "-"} / {row.endStakeNo ?? "-"}</td><td>{row.startWellNo ?? "-"} / {row.endWellNo ?? "-"}</td><td>{row.length ?? 0}</td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button></td></tr>)}</tbody></table></div>
      <Modal open={open} title="施工段" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
        <div className="grid grid-2">
          <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
          <Field label="施工段名称"><TextInput value={form.name ?? ""} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="起始桩号"><TextInput value={form.startStakeNo ?? ""} onChange={e => setForm({ ...form, startStakeNo: e.target.value })} /></Field>
          <Field label="结束桩号"><TextInput value={form.endStakeNo ?? ""} onChange={e => setForm({ ...form, endStakeNo: e.target.value })} /></Field>
          <Field label="起始井号"><TextInput value={form.startWellNo ?? ""} onChange={e => setForm({ ...form, startWellNo: e.target.value })} /></Field>
          <Field label="结束井号"><TextInput value={form.endWellNo ?? ""} onChange={e => setForm({ ...form, endWellNo: e.target.value })} /></Field>
          <Field label="长度"><TextInput type="number" value={form.length ?? 0} onChange={e => setForm({ ...form, length: Number(e.target.value) })} /></Field>
        </div>
      </Modal>
    </Page>
  );
}

export function WorkProcessPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<Process>>({});
  const [open, setOpen] = useState(false);
  const { data } = useQuery({ queryKey: ["work-processes"], queryFn: () => getData<PageResult<Process>>("/work-processes", { page: 1, pageSize: 300 }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/work-processes/${form.id}`, form) : postData("/work-processes", form), onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["work-processes"] }); queryClient.invalidateQueries({ queryKey: ["process-lookup"] }); } });
  return (
    <Page title="工序库" actions={<Button onClick={() => { setForm({ unit: "m", status: "active", sortOrder: 0 }); setOpen(true); }}>新增工序</Button>}>
      <div className="panel table-wrap"><table><thead><tr><th>工序</th><th>单位</th><th>排序</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{row.name}</td><td>{row.unit}</td><td>{row.sortOrder ?? 0}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button></td></tr>)}</tbody></table></div>
      <Modal open={open} title="工序" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
        <div className="grid grid-2"><Field label="工序名称"><TextInput value={form.name ?? ""} onChange={e => setForm({ ...form, name: e.target.value })} /></Field><Field label="单位"><TextInput value={form.unit ?? ""} onChange={e => setForm({ ...form, unit: e.target.value })} /></Field><Field label="排序"><TextInput type="number" value={form.sortOrder ?? 0} onChange={e => setForm({ ...form, sortOrder: Number(e.target.value) })} /></Field><Field label="状态"><SelectInput value={form.status ?? "active"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="active">启用</option><option value="inactive">停用</option></SelectInput></Field></div>
      </Modal>
    </Page>
  );
}

export function TeamPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<Team>>({});
  const [open, setOpen] = useState(false);
  const { data } = useQuery({ queryKey: ["teams"], queryFn: () => getData<PageResult<Team>>("/teams", { page: 1, pageSize: 200 }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/teams/${form.id}`, form) : postData("/teams", form), onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["teams"] }); queryClient.invalidateQueries({ queryKey: ["team-lookup"] }); } });
  return (
    <Page title="班组管理" actions={<Button onClick={() => { setForm({ status: "active", settlementMethod: "quantity" }); setOpen(true); }}>新增班组</Button>}>
      <div className="panel table-wrap"><table><thead><tr><th>班组</th><th>负责人</th><th>电话</th><th>工种</th><th>结算方式</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{row.name}</td><td>{row.leaderName ?? "-"}</td><td>{row.phone ?? "-"}</td><td>{row.workType ?? "-"}</td><td>{row.settlementMethod ?? "-"}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button></td></tr>)}</tbody></table></div>
      <Modal open={open} title="班组" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
        <div className="grid grid-2"><Field label="班组名称"><TextInput value={form.name ?? ""} onChange={e => setForm({ ...form, name: e.target.value })} /></Field><Field label="负责人"><TextInput value={form.leaderName ?? ""} onChange={e => setForm({ ...form, leaderName: e.target.value })} /></Field><Field label="电话"><TextInput value={form.phone ?? ""} onChange={e => setForm({ ...form, phone: e.target.value })} /></Field><Field label="工种"><TextInput value={form.workType ?? ""} onChange={e => setForm({ ...form, workType: e.target.value })} /></Field><Field label="结算方式"><TextInput value={form.settlementMethod ?? "quantity"} onChange={e => setForm({ ...form, settlementMethod: e.target.value })} /></Field><Field label="状态"><SelectInput value={form.status ?? "active"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="active">启用</option><option value="inactive">停用</option></SelectInput></Field></div>
      </Modal>
    </Page>
  );
}

export function DailyWorkReportPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [form, setForm] = useState<Partial<DailyReport>>({});
  const [open, setOpen] = useState(false);
  const [evidence, setEvidence] = useState<EvidenceTarget | null>(null);
  const { data } = useQuery({ queryKey: ["daily-work-reports"], queryFn: () => getData<PageResult<DailyReport>>("/daily-work-reports", { page: 1, pageSize: 200 }) });
  const action = useMutation({ mutationFn: ({ id, op }: { id: string; op: string }) => postData(`/daily-work-reports/${id}/${op}`, {}), onSuccess: (_result, variables) => { queryClient.invalidateQueries({ queryKey: ["daily-work-reports"] }); queryClient.invalidateQueries({ queryKey: ["project-evidence", "daily", variables.id] }); } });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/daily-work-reports/${form.id}`, form) : postData("/daily-work-reports", form), onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["daily-work-reports"] }); } });
  async function attach(row: DailyReport, file?: File) {
    if (!file) return;
    const uploaded = await uploadFile(file);
    await postData(`/daily-work-reports/${row.id}/files`, { fileId: uploaded.id, fileType: "photo" });
    await queryClient.invalidateQueries({ queryKey: ["project-evidence", "daily", row.id] });
  }
  return (
    <Page title="工程量日报" actions={<Button onClick={() => { setForm({ workDate: new Date().toISOString().slice(0, 10), completedQuantity: 0, workerCount: 0, projectId: lookups.projects[0]?.id, projectSectionId: lookups.sections[0]?.id, workProcessId: lookups.processes[0]?.id, teamId: lookups.teams[0]?.id, unit: lookups.processes[0]?.unit ?? "m" }); setOpen(true); }}>新增日报</Button>}>
      <div className="panel table-wrap"><table><thead><tr><th>日期</th><th>项目</th><th>施工段</th><th>工序</th><th>工程量</th><th>产值</th><th>计价口径</th><th>班组</th><th>状态</th><th>照片</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{row.workDate}</td><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.sections, row.projectSectionId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{row.completedQuantity} {row.unit}</td><td>{money(row.outputValue)}</td><td title={row.pricingBasis ?? ""}>{row.pricingBasis ?? "-"}</td><td>{nameOf(lookups.teams, row.teamId)}</td><td><Badge status={row.status} /></td><td><input type="file" onChange={e => attach(row, e.target.files?.[0])} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button>{" "}<Button variant="secondary" onClick={() => action.mutate({ id: row.id, op: "submit" })}>提交</Button>{" "}<Button variant="secondary" onClick={() => setEvidence({ type: "daily", id: row.id, title: `日报 ${row.workDate}` })}>证据链</Button></td></tr>)}</tbody></table></div>
      <DailyReportModal open={open} form={form} setForm={setForm} lookups={lookups} onClose={() => setOpen(false)} onSave={() => save.mutate()} />
      <ProjectEvidenceModal target={evidence} onClose={() => setEvidence(null)} />
    </Page>
  );
}

function DailyReportModal({ open, form, setForm, lookups, onClose, onSave }: { open: boolean; form: Partial<DailyReport>; setForm: (form: Partial<DailyReport>) => void; lookups: ReturnType<typeof useLookups>; onClose: () => void; onSave: () => void }) {
  const contractItems = useQuery({ queryKey: ["daily-contract-items", form.projectId], enabled: Boolean(form.projectId), queryFn: () => getData<PageResult<ContractItem>>("/project-contract-items", { page: 1, pageSize: 200, projectId: form.projectId, status: "active" }) });
  const availableContractItems = contractItems.data?.records ?? [];
  return <Modal open={open} title="工程量日报" onClose={onClose} footer={<><Button variant="secondary" onClick={onClose}>取消</Button><Button onClick={onSave}>保存</Button></>}>
    <div className="grid grid-2">
      <Field label="施工日期"><TextInput type="date" value={form.workDate ?? ""} onChange={e => setForm({ ...form, workDate: e.target.value })} /></Field>
      <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      <Field label="施工段"><SelectInput value={form.projectSectionId ?? ""} onChange={e => setForm({ ...form, projectSectionId: e.target.value })}>{lookups.sections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</SelectInput></Field>
      <Field label="合同清单"><SelectInput value={form.contractItemId ?? ""} onChange={e => { const item = availableContractItems.find(row => row.id === e.target.value); setForm({ ...form, contractItemId: e.target.value || undefined, projectSectionId: item?.projectSectionId ?? form.projectSectionId, workProcessId: item?.workProcessId ?? form.workProcessId, unit: item?.unit ?? form.unit }); }}><option value="">自动匹配</option>{availableContractItems.map(item => <option key={item.id} value={item.id}>{contractItemText(item)}</option>)}</SelectInput></Field>
      <Field label="工序"><SelectInput value={form.workProcessId ?? ""} onChange={e => { const p = lookups.processes.find(item => item.id === e.target.value); setForm({ ...form, workProcessId: e.target.value, unit: p?.unit ?? form.unit }); }}>{lookups.processes.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      <Field label="完成工程量"><TextInput type="number" value={form.completedQuantity ?? 0} onChange={e => setForm({ ...form, completedQuantity: Number(e.target.value) })} /></Field>
      <Field label="单位"><TextInput value={form.unit ?? ""} onChange={e => setForm({ ...form, unit: e.target.value })} /></Field>
      <Field label="施工班组"><SelectInput value={form.teamId ?? ""} onChange={e => setForm({ ...form, teamId: e.target.value })}>{lookups.teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</SelectInput></Field>
      <Field label="施工人数"><TextInput type="number" value={form.workerCount ?? 0} onChange={e => setForm({ ...form, workerCount: Number(e.target.value) })} /></Field>
      <Field label="机械使用"><TextInput value={form.machineryUsage ?? ""} onChange={e => setForm({ ...form, machineryUsage: e.target.value })} /></Field>
      <Field label="填报人"><TextInput value={form.reporterName ?? ""} onChange={e => setForm({ ...form, reporterName: e.target.value })} /></Field>
    </div>
    <Field label="异常说明"><TextArea value={form.exceptionNote ?? ""} onChange={e => setForm({ ...form, exceptionNote: e.target.value })} /></Field>
  </Modal>;
}

export function WorkConfirmationPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [evidence, setEvidence] = useState<EvidenceTarget | null>(null);
  const { data } = useQuery({ queryKey: ["work-confirmation"], queryFn: () => getData<PageResult<DailyReport>>("/daily-work-reports", { page: 1, pageSize: 200 }) });
  const rows = (data?.records ?? []).filter(row => ["submitted", "pm_confirmed", "rejected", "confirmed"].includes(row.status));
  const action = useMutation({ mutationFn: ({ id, op }: { id: string; op: string }) => postData(`/daily-work-reports/${id}/${op}`, {}), onSuccess: (_result, variables) => { queryClient.invalidateQueries({ queryKey: ["work-confirmation"] }); queryClient.invalidateQueries({ queryKey: ["project-evidence", "daily", variables.id] }); } });
  return <Page title="工程量确认"><div className="panel table-wrap"><table><thead><tr><th>日期</th><th>项目</th><th>工序</th><th>工程量</th><th>状态</th><th>操作</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td>{row.workDate}</td><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{row.completedQuantity} {row.unit}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => action.mutate({ id: row.id, op: "pm-confirm" })}>项目经理确认</Button>{" "}<Button onClick={() => action.mutate({ id: row.id, op: "final-confirm" })}>商务终审</Button>{" "}<Button variant="danger" onClick={() => action.mutate({ id: row.id, op: "reject" })}>驳回</Button>{" "}<Button variant="secondary" onClick={() => setEvidence({ type: "daily", id: row.id, title: `日报 ${row.workDate}` })}>证据链</Button></td></tr>)}</tbody></table></div><ProjectEvidenceModal target={evidence} onClose={() => setEvidence(null)} /></Page>;
}

export function TeamUnitPricePage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [teamPrice, setTeamPrice] = useState<Partial<TeamUnitPrice>>({});
  const [processPrice, setProcessPrice] = useState<Partial<ProcessPrice>>({});
  const teamPrices = useQuery({ queryKey: ["team-unit-prices"], queryFn: () => getData<PageResult<TeamUnitPrice>>("/team-unit-prices", { page: 1, pageSize: 200 }) });
  const processPrices = useQuery({ queryKey: ["project-process-prices"], queryFn: () => getData<PageResult<ProcessPrice>>("/project-process-prices", { page: 1, pageSize: 200 }) });
  const saveTeamPrice = useMutation({ mutationFn: () => postData("/team-unit-prices", teamPrice), onSuccess: () => { setTeamPrice({}); queryClient.invalidateQueries({ queryKey: ["team-unit-prices"] }); } });
  const saveProcessPrice = useMutation({ mutationFn: () => postData("/project-process-prices", processPrice), onSuccess: () => { setProcessPrice({}); queryClient.invalidateQueries({ queryKey: ["project-process-prices"] }); } });
  return <Page title="班组单价">
    <div className="panel toolbar"><LookupPriceFields lookups={lookups} value={teamPrice} onChange={setTeamPrice} includeTeam /><Button onClick={() => saveTeamPrice.mutate()}>保存班组单价</Button></div>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>班组</th><th>工序</th><th>单位</th><th>单价</th><th>状态</th></tr></thead><tbody>{(teamPrices.data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.teams, row.teamId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{row.unit}</td><td>{money(row.unitPrice)}</td><td><Badge status={row.status} /></td></tr>)}</tbody></table></div>
    <div className="panel toolbar" style={{ marginTop: 16 }}><LookupPriceFields lookups={lookups} value={processPrice} onChange={setProcessPrice} /><Button onClick={() => saveProcessPrice.mutate()}>保存产值单价</Button></div>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>工序</th><th>单位</th><th>产值单价</th><th>状态</th></tr></thead><tbody>{(processPrices.data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{row.unit}</td><td>{money(row.unitPrice)}</td><td><Badge status={row.status} /></td></tr>)}</tbody></table></div>
  </Page>;
}

export function ProjectContractItemPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [projectId, setProjectId] = useState("");
  const [form, setForm] = useState<Partial<ContractItem>>({});
  const [open, setOpen] = useState(false);
  const { data } = useQuery({ queryKey: ["project-contract-items", projectId], queryFn: () => getData<PageResult<ContractItem>>("/project-contract-items", { page: 1, pageSize: 300, projectId: projectId || undefined }) });
  const save = useMutation({
    mutationFn: () => form.id ? putData(`/project-contract-items/${form.id}`, form) : postData("/project-contract-items", form),
    onSuccess: () => {
      setOpen(false);
      setForm({});
      queryClient.invalidateQueries({ queryKey: ["project-contract-items"] });
      queryClient.invalidateQueries({ queryKey: ["daily-contract-items"] });
    }
  });
  const defaultProjectId = projectId || lookups.projects[0]?.id;
  const defaultProcess = lookups.processes[0];
  return <Page title="合同清单" actions={<Button onClick={() => { setForm({ projectId: defaultProjectId, workProcessId: defaultProcess?.id, unit: defaultProcess?.unit ?? "m", contractQuantity: 0, unitPrice: 0, status: "active" }); setOpen(true); }}>新增清单</Button>}>
    <div className="panel toolbar"><Field label="项目"><SelectInput value={projectId} onChange={e => setProjectId(e.target.value)}><option value="">全部项目</option>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field></div>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>施工段</th><th>工序</th><th>清单</th><th>工程量</th><th>单位</th><th>单价</th><th>合同额</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.sections, row.projectSectionId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{contractItemText(row)}</td><td>{row.contractQuantity}</td><td>{row.unit}</td><td>{money(row.unitPrice)}</td><td>{money(row.contractAmount)}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button></td></tr>)}</tbody></table></div>
    <Modal open={open} title="合同清单" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
      <div className="grid grid-2">
        <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
        <Field label="施工段"><SelectInput value={form.projectSectionId ?? ""} onChange={e => setForm({ ...form, projectSectionId: e.target.value || undefined })}><option value="">全项目</option>{lookups.sections.filter(s => !form.projectId || s.projectId === form.projectId).map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</SelectInput></Field>
        <Field label="工序"><SelectInput value={form.workProcessId ?? ""} onChange={e => { const process = lookups.processes.find(item => item.id === e.target.value); setForm({ ...form, workProcessId: e.target.value, unit: process?.unit ?? form.unit }); }}>{lookups.processes.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
        <Field label="清单编码"><TextInput value={form.itemCode ?? ""} onChange={e => setForm({ ...form, itemCode: e.target.value })} /></Field>
        <Field label="清单名称"><TextInput value={form.itemName ?? ""} onChange={e => setForm({ ...form, itemName: e.target.value })} /></Field>
        <Field label="单位"><TextInput value={form.unit ?? ""} onChange={e => setForm({ ...form, unit: e.target.value })} /></Field>
        <Field label="合同工程量"><TextInput type="number" value={form.contractQuantity ?? 0} onChange={e => setForm({ ...form, contractQuantity: Number(e.target.value) })} /></Field>
        <Field label="单价"><TextInput type="number" value={form.unitPrice ?? 0} onChange={e => setForm({ ...form, unitPrice: Number(e.target.value) })} /></Field>
        <Field label="状态"><SelectInput value={form.status ?? "active"} onChange={e => setForm({ ...form, status: e.target.value })}><option value="active">启用</option><option value="inactive">停用</option></SelectInput></Field>
      </div>
      <Field label="备注"><TextArea value={form.remark ?? ""} onChange={e => setForm({ ...form, remark: e.target.value })} /></Field>
    </Modal>
  </Page>;
}

function LookupPriceFields({ lookups, value, onChange, includeTeam = false }: { lookups: ReturnType<typeof useLookups>; value: Partial<TeamUnitPrice & ProcessPrice>; onChange: (value: Partial<TeamUnitPrice & ProcessPrice>) => void; includeTeam?: boolean }) {
  return <>
    <Field label="项目"><SelectInput value={value.projectId ?? ""} onChange={e => onChange({ ...value, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
    {includeTeam && <Field label="班组"><SelectInput value={value.teamId ?? ""} onChange={e => onChange({ ...value, teamId: e.target.value })}>{lookups.teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</SelectInput></Field>}
    <Field label="工序"><SelectInput value={value.workProcessId ?? ""} onChange={e => { const p = lookups.processes.find(item => item.id === e.target.value); onChange({ ...value, workProcessId: e.target.value, unit: p?.unit ?? value.unit }); }}>{lookups.processes.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
    <Field label="单位"><TextInput value={value.unit ?? ""} onChange={e => onChange({ ...value, unit: e.target.value })} /></Field>
    <Field label="单价"><TextInput type="number" value={value.unitPrice ?? 0} onChange={e => onChange({ ...value, unitPrice: Number(e.target.value), status: "active" })} /></Field>
  </>;
}

function ProjectEvidenceModal({ target, onClose }: { target: EvidenceTarget | null; onClose: () => void }) {
  const confirmsUrl = target?.type === "daily" ? `/daily-work-reports/${target.id}/confirms` : `/variation-orders/${target?.id}/confirms`;
  const filesUrl = target?.type === "daily" ? `/daily-work-reports/${target.id}/files` : `/variation-orders/${target?.id}/files`;
  const confirms = useQuery({
    queryKey: ["project-evidence", target?.type, target?.id, "confirms"],
    queryFn: () => getData<ApprovalRecord[]>(confirmsUrl),
    enabled: Boolean(target)
  });
  const files = useQuery({
    queryKey: ["project-evidence", target?.type, target?.id, "files"],
    queryFn: () => getData<EvidenceFile[]>(filesUrl),
    enabled: Boolean(target)
  });
  return (
    <Modal open={Boolean(target)} title={target?.title ?? "证据链"} onClose={onClose}>
      <div className="grid">
        <div>
          <div className="section-heading"><h3>审批记录</h3><span className="muted">{confirms.data?.length ?? 0} 条</span></div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>时间</th><th>动作</th><th>状态</th><th>操作者</th><th>意见</th></tr></thead>
              <tbody>{(confirms.data ?? []).map(item => <tr key={item.id}><td>{timeText(item.createdAt)}</td><td>{approvalStageText(item.confirmStage, item.confirmStatus)}</td><td>{statusText(item.statusBefore)} → {statusText(item.statusAfter)}</td><td>{item.confirmerName ?? "-"}</td><td>{item.remark ?? "-"}</td></tr>)}</tbody>
            </table>
          </div>
        </div>
        <div>
          <div className="section-heading"><h3>附件证据</h3><span className="muted">{files.data?.length ?? 0} 个</span></div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>时间</th><th>文件 ID</th><th>类型</th></tr></thead>
              <tbody>{(files.data ?? []).map(item => <tr key={item.id}><td>{timeText(item.createdAt)}</td><td>{item.fileId}</td><td>{item.fileType}</td></tr>)}</tbody>
            </table>
          </div>
        </div>
      </div>
    </Modal>
  );
}

function approvalStageText(stage?: string, status?: string) {
  const map: Record<string, string> = {
    submit: "提交",
    project_manager: "项目经理确认",
    commercial: "商务确认",
    reject: "驳回",
    attachment: "附件"
  };
  return `${map[stage ?? ""] ?? stage ?? "-"} / ${statusText(status)}`;
}

function timeText(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

function paymentRecordTypeText(type?: string) {
  const map: Record<string, string> = {
    receivable: "应收款",
    received: "实际回款",
    invoice: "发票",
    retention_withheld: "质保金预留",
    retention_returned: "质保金返还"
  };
  return type ? map[type] ?? type : "-";
}

export function TeamSettlementPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [form, setForm] = useState({ projectId: "", teamId: "", settlementMonth: new Date().toISOString().slice(0, 7), compensationAmount: 0, deductionAmount: 0 });
  const { data } = useQuery({ queryKey: ["team-settlements"], queryFn: () => getData<PageResult<Settlement>>("/team-settlements", { page: 1, pageSize: 200 }) });
  const generate = useMutation({ mutationFn: () => postData("/team-settlements/generate", form), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["team-settlements"] }) });
  const action = useMutation({ mutationFn: ({ id, op }: { id: string; op: string }) => postData(`/team-settlements/${id}/${op}`, {}), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["team-settlements"] }) });
  return <Page title="班组结算">
    <form className="panel toolbar" onSubmit={(e: FormEvent) => { e.preventDefault(); generate.mutate(); }}><Field label="项目"><SelectInput value={form.projectId} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="班组"><SelectInput value={form.teamId} onChange={e => setForm({ ...form, teamId: e.target.value })}>{lookups.teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</SelectInput></Field><Field label="月份"><TextInput type="month" value={form.settlementMonth} onChange={e => setForm({ ...form, settlementMonth: e.target.value })} /></Field><Field label="零星补偿"><TextInput type="number" value={form.compensationAmount} onChange={e => setForm({ ...form, compensationAmount: Number(e.target.value) })} /></Field><Field label="扣款"><TextInput type="number" value={form.deductionAmount} onChange={e => setForm({ ...form, deductionAmount: Number(e.target.value) })} /></Field><Button type="submit">生成结算</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>班组</th><th>月份</th><th>确认工程量</th><th>结算金额</th><th>实付</th><th>结算状态</th><th>付款状态</th><th>驳回原因</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{row.projectName ?? nameOf(lookups.projects, row.projectId)}</td><td>{row.teamName ?? nameOf(lookups.teams, row.teamId)}</td><td>{row.settlementMonth}</td><td>{row.confirmedQuantity}</td><td>{money(row.settlementAmount)}</td><td>{money(row.actualPayAmount)}</td><td><Badge status={row.settlementStatus} /></td><td><Badge status={row.paymentStatus} /></td><td>{row.rejectedReason ?? "-"}</td><td><Button variant="secondary" disabled={row.settlementStatus !== "draft"} onClick={() => action.mutate({ id: row.id, op: "confirm" })}>确认</Button>{" "}<Button variant="danger" disabled={row.settlementStatus !== "draft"} onClick={() => action.mutate({ id: row.id, op: "reject" })}>驳回</Button>{" "}<Button disabled={row.settlementStatus !== "confirmed" || row.paymentStatus === "paid"} onClick={() => action.mutate({ id: row.id, op: "mark-paid" })}>标记付款</Button></td></tr>)}</tbody></table></div>
  </Page>;
}

export function VariationOrderPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [form, setForm] = useState<Partial<Variation>>({});
  const [open, setOpen] = useState(false);
  const [evidence, setEvidence] = useState<EvidenceTarget | null>(null);
  const { data } = useQuery({ queryKey: ["variation-orders"], queryFn: () => getData<PageResult<Variation>>("/variation-orders", { page: 1, pageSize: 200 }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/variation-orders/${form.id}`, form) : postData("/variation-orders", form), onSuccess: () => { setOpen(false); setForm({}); queryClient.invalidateQueries({ queryKey: ["variation-orders"] }); } });
  const action = useMutation({ mutationFn: ({ id, op, data }: { id: string; op: string; data?: unknown }) => postData(`/variation-orders/${id}/${op}`, data ?? {}), onSuccess: (_result, variables) => { queryClient.invalidateQueries({ queryKey: ["variation-orders"] }); queryClient.invalidateQueries({ queryKey: ["project-evidence", "variation", variables.id] }); queryClient.invalidateQueries({ queryKey: ["project-dashboard"] }); } });
  async function attach(row: Variation, file?: File) {
    if (!file) return;
    const uploaded = await uploadFile(file);
    await postData(`/variation-orders/${row.id}/files`, { fileId: uploaded.id, fileType: "evidence" });
    await queryClient.invalidateQueries({ queryKey: ["project-evidence", "variation", row.id] });
  }
  function confirmVariation(row: Variation) {
    const value = window.prompt("确认金额", String(row.confirmedAmount ?? row.estimatedAmount ?? 0));
    if (value === null) return;
    const confirmedAmount = Number(value);
    if (!Number.isFinite(confirmedAmount) || confirmedAmount < 0) return;
    action.mutate({ id: row.id, op: "confirm", data: { confirmedAmount } });
  }
  return <Page title="签证变更" actions={<Button onClick={() => { setForm({ occurrenceDate: new Date().toISOString().slice(0, 10), variationType: "工程量增加", projectId: lookups.projects[0]?.id, estimatedAmount: 0, increasedQuantity: 0 }); setOpen(true); }}>新增签证</Button>}>
    <div className="panel table-wrap"><table><thead><tr><th>日期</th><th>项目</th><th>类型</th><th>预计金额</th><th>确认金额</th><th>状态</th><th>证据</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{row.occurrenceDate}</td><td>{nameOf(lookups.projects, row.projectId)}</td><td>{row.variationType}</td><td>{money(row.estimatedAmount)}</td><td>{money(row.confirmedAmount)}</td><td><Badge status={row.status} /></td><td><input type="file" onChange={e => attach(row, e.target.files?.[0])} /></td><td><Button variant="secondary" onClick={() => { setForm(row); setOpen(true); }}>编辑</Button>{" "}<Button variant="secondary" onClick={() => action.mutate({ id: row.id, op: "submit" })}>提交</Button>{" "}<Button onClick={() => confirmVariation(row)}>确认</Button>{" "}<Button variant="danger" onClick={() => action.mutate({ id: row.id, op: "reject" })}>驳回</Button>{" "}<Button variant="secondary" onClick={() => setEvidence({ type: "variation", id: row.id, title: `签证 ${row.variationType}` })}>证据链</Button></td></tr>)}</tbody></table></div>
    <Modal open={open} title="签证变更" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>取消</Button><Button onClick={() => save.mutate()}>保存</Button></>}>
      <div className="grid grid-2"><Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="施工段"><SelectInput value={form.projectSectionId ?? ""} onChange={e => setForm({ ...form, projectSectionId: e.target.value || undefined })}><option value="">无</option>{lookups.sections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</SelectInput></Field><Field label="发生日期"><TextInput type="date" value={form.occurrenceDate ?? ""} onChange={e => setForm({ ...form, occurrenceDate: e.target.value })} /></Field><Field label="签证类型"><SelectInput value={form.variationType ?? "工程量增加"} onChange={e => setForm({ ...form, variationType: e.target.value })}>{["设计变更","工程量增加","障碍物处理","管线迁改配合","返工","停工窝工","夜间施工","临时排水","二次倒运","零星用工","其他"].map(item => <option key={item} value={item}>{item}</option>)}</SelectInput></Field><Field label="增加工程量"><TextInput type="number" value={form.increasedQuantity ?? 0} onChange={e => setForm({ ...form, increasedQuantity: Number(e.target.value) })} /></Field><Field label="预计金额"><TextInput type="number" value={form.estimatedAmount ?? 0} onChange={e => setForm({ ...form, estimatedAmount: Number(e.target.value) })} /></Field><Field label="人工数量"><TextInput type="number" value={form.laborCount ?? 0} onChange={e => setForm({ ...form, laborCount: Number(e.target.value) })} /></Field><Field label="责任方"><TextInput value={form.responsibleParty ?? ""} onChange={e => setForm({ ...form, responsibleParty: e.target.value })} /></Field></div>
      <Field label="事件原因"><TextArea value={form.reason ?? ""} onChange={e => setForm({ ...form, reason: e.target.value })} /></Field>
    </Modal>
    <ProjectEvidenceModal target={evidence} onClose={() => setEvidence(null)} />
  </Page>;
}

export function ProjectPaymentPage() {
  const queryClient = useQueryClient();
  const { projects } = useLookups();
  const [form, setForm] = useState<Partial<PaymentRecord>>({ recordType: "received", paymentDate: new Date().toISOString().slice(0, 10), amount: 0 });
  const [file, setFile] = useState<File | undefined>();
  const { data } = useQuery({ queryKey: ["project-payment-records"], queryFn: () => getData<PageResult<PaymentRecord>>("/project-payment-records", { page: 1, pageSize: 200 }) });
  const save = useMutation({
    mutationFn: async () => {
      const uploaded = file ? await uploadFile(file) : undefined;
      return form.id ? putData(`/project-payment-records/${form.id}`, { ...form, voucherFileId: uploaded?.id ?? form.voucherFileId }) : postData("/project-payment-records", { ...form, voucherFileId: uploaded?.id });
    },
    onSuccess: () => { setForm({ recordType: "received", paymentDate: new Date().toISOString().slice(0, 10), amount: 0 }); setFile(undefined); queryClient.invalidateQueries({ queryKey: ["project-payment-records"] }); queryClient.invalidateQueries({ queryKey: ["project-dashboard"] }); }
  });
  return <Page title="项目回款管理">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}>
      <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      <Field label="类型"><SelectInput value={form.recordType ?? "received"} onChange={e => setForm({ ...form, recordType: e.target.value })}><option value="receivable">应收款</option><option value="received">实际回款</option><option value="invoice">发票</option><option value="retention_withheld">质保金预留</option><option value="retention_returned">质保金返还</option></SelectInput></Field>
      <Field label="日期"><TextInput type="date" value={form.paymentDate ?? ""} onChange={e => setForm({ ...form, paymentDate: e.target.value })} /></Field>
      <Field label="金额"><TextInput type="number" value={form.amount ?? 0} onChange={e => setForm({ ...form, amount: Number(e.target.value) })} /></Field>
      <Field label="付款单位"><TextInput value={form.payerName ?? ""} onChange={e => setForm({ ...form, payerName: e.target.value })} /></Field>
      <Field label="发票号"><TextInput value={form.invoiceNo ?? ""} onChange={e => setForm({ ...form, invoiceNo: e.target.value })} /></Field>
      <Field label="开票日期"><TextInput type="date" value={form.invoiceDate ?? ""} onChange={e => setForm({ ...form, invoiceDate: e.target.value })} /></Field>
      <Field label="质保到期"><TextInput type="date" value={form.retentionDueDate ?? ""} onChange={e => setForm({ ...form, retentionDueDate: e.target.value })} /></Field>
      <Field label="凭证"><input type="file" onChange={e => setFile(e.target.files?.[0])} /></Field>
      <Button type="submit">{form.id ? "保存" : "登记"}</Button>
    </form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>类型</th><th>日期</th><th>金额</th><th>付款单位</th><th>发票号</th><th>质保到期</th><th>凭证</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(projects, row.projectId)}</td><td>{paymentRecordTypeText(row.recordType)}</td><td>{row.paymentDate}</td><td>{money(row.amount)}</td><td>{row.payerName ?? "-"}</td><td>{row.invoiceNo ?? "-"}</td><td>{row.retentionDueDate ?? "-"}</td><td>{row.voucherFileId ?? "-"}</td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button></td></tr>)}</tbody></table></div>
  </Page>;
}

export function ProjectCostPage() {
  const queryClient = useQueryClient();
  const { projects } = useLookups();
  const [form, setForm] = useState<Partial<CostRecord>>({ costDate: new Date().toISOString().slice(0, 10), costType: "其他成本", amount: 0 });
  const { data } = useQuery({ queryKey: ["project-cost-page-records"], queryFn: () => getData<PageResult<CostRecord>>("/project-cost-records", { page: 1, pageSize: 200 }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/project-cost-records/${form.id}`, form) : postData("/project-cost-records", form), onSuccess: () => { setForm({ costDate: new Date().toISOString().slice(0, 10), costType: "其他成本", amount: 0 }); queryClient.invalidateQueries({ queryKey: ["project-cost-page-records"] }); queryClient.invalidateQueries({ queryKey: ["project-dashboard"] }); } });
  return <Page title="项目成本管理">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}>
      <Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field>
      <Field label="日期"><TextInput type="date" value={form.costDate ?? ""} onChange={e => setForm({ ...form, costDate: e.target.value })} /></Field>
      <Field label="类型"><SelectInput value={form.costType ?? "其他成本"} onChange={e => setForm({ ...form, costType: e.target.value })}><option value="员工工资分摊">员工工资分摊</option><option value="班组结算">班组结算</option><option value="材料辅材">材料辅材</option><option value="机械台班">机械台班</option><option value="报销费用">报销费用</option><option value="其他成本">其他成本</option></SelectInput></Field>
      <Field label="金额"><TextInput type="number" value={form.amount ?? 0} onChange={e => setForm({ ...form, amount: Number(e.target.value) })} /></Field>
      <Field label="说明"><TextInput value={form.description ?? ""} onChange={e => setForm({ ...form, description: e.target.value })} /></Field>
      <Button type="submit">{form.id ? "保存" : "登记"}</Button>
    </form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>日期</th><th>类型</th><th>金额</th><th>说明</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(projects, row.projectId)}</td><td>{row.costDate}</td><td>{row.costType}</td><td>{money(row.amount)}</td><td>{row.description ?? "-"}</td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button></td></tr>)}</tbody></table></div>
  </Page>;
}

export function MachineryPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [equipment, setEquipment] = useState<Partial<MachineryEquipment>>({ equipmentType: "挖机", unitPrice: 0, status: "active" });
  const [record, setRecord] = useState<Partial<MachineryWorkRecord>>({ workDate: new Date().toISOString().slice(0, 10), shiftCount: 0, unitPrice: 0 });
  const equipments = useQuery({ queryKey: ["machinery-equipments"], queryFn: () => getData<PageResult<MachineryEquipment>>("/machinery-equipments", { page: 1, pageSize: 200 }) });
  const records = useQuery({ queryKey: ["machinery-work-records"], queryFn: () => getData<PageResult<MachineryWorkRecord>>("/machinery-work-records", { page: 1, pageSize: 200 }) });
  const saveEquipment = useMutation({ mutationFn: () => equipment.id ? putData(`/machinery-equipments/${equipment.id}`, equipment) : postData("/machinery-equipments", equipment), onSuccess: () => { setEquipment({ equipmentType: "挖机", unitPrice: 0, status: "active" }); queryClient.invalidateQueries({ queryKey: ["machinery-equipments"] }); queryClient.invalidateQueries({ queryKey: ["machinery-lookup"] }); } });
  const saveRecord = useMutation({ mutationFn: () => record.id ? putData(`/machinery-work-records/${record.id}`, record) : postData("/machinery-work-records", record), onSuccess: () => { setRecord({ workDate: new Date().toISOString().slice(0, 10), shiftCount: 0, unitPrice: 0 }); queryClient.invalidateQueries({ queryKey: ["machinery-work-records"] }); queryClient.invalidateQueries({ queryKey: ["project-dashboard"] }); } });
  return <Page title="机械台班管理">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); saveEquipment.mutate(); }}><Field label="设备名称"><TextInput value={equipment.name ?? ""} onChange={e => setEquipment({ ...equipment, name: e.target.value })} /></Field><Field label="类型"><SelectInput value={equipment.equipmentType ?? "挖机"} onChange={e => setEquipment({ ...equipment, equipmentType: e.target.value })}>{["挖机","吊车","渣土车","发电机","水泵","空压机","运输车辆","小型机具"].map(item => <option key={item} value={item}>{item}</option>)}</SelectInput></Field><Field label="台班单价"><TextInput type="number" value={equipment.unitPrice ?? 0} onChange={e => setEquipment({ ...equipment, unitPrice: Number(e.target.value) })} /></Field><Button type="submit">保存设备</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>设备</th><th>类型</th><th>台班单价</th><th>状态</th><th>操作</th></tr></thead><tbody>{(equipments.data?.records ?? []).map(row => <tr key={row.id}><td>{row.name}</td><td>{row.equipmentType}</td><td>{money(row.unitPrice)}</td><td><Badge status={row.status} /></td><td><Button variant="secondary" onClick={() => setEquipment(row)}>编辑</Button></td></tr>)}</tbody></table></div>
    <form className="panel toolbar" style={{ marginTop: 16 }} onSubmit={(event: FormEvent) => { event.preventDefault(); saveRecord.mutate(); }}><Field label="项目"><SelectInput value={record.projectId ?? ""} onChange={e => setRecord({ ...record, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="设备"><SelectInput value={record.machineryEquipmentId ?? ""} onChange={e => { const selected = lookups.machinery.find(item => item.id === e.target.value); setRecord({ ...record, machineryEquipmentId: e.target.value, unitPrice: selected?.unitPrice ?? record.unitPrice }); }}>{lookups.machinery.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}</SelectInput></Field><Field label="日期"><TextInput type="date" value={record.workDate ?? ""} onChange={e => setRecord({ ...record, workDate: e.target.value })} /></Field><Field label="台班数"><TextInput type="number" value={record.shiftCount ?? 0} onChange={e => setRecord({ ...record, shiftCount: Number(e.target.value) })} /></Field><Field label="单价"><TextInput type="number" value={record.unitPrice ?? 0} onChange={e => setRecord({ ...record, unitPrice: Number(e.target.value) })} /></Field><Field label="窝工原因"><TextInput value={record.idleReason ?? ""} onChange={e => setRecord({ ...record, idleReason: e.target.value })} /></Field><Button type="submit">登记台班</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>设备</th><th>日期</th><th>台班</th><th>金额</th><th>窝工原因</th></tr></thead><tbody>{(records.data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.machinery, row.machineryEquipmentId)}</td><td>{row.workDate}</td><td>{row.shiftCount}</td><td>{money(row.amount)}</td><td>{row.idleReason ?? "-"}</td></tr>)}</tbody></table></div>
  </Page>;
}

export function MaterialRecordPage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [form, setForm] = useState<Partial<MaterialRecord>>({ recordType: "arrival", recordDate: new Date().toISOString().slice(0, 10), quantity: 0, unitPrice: 0 });
  const { data } = useQuery({ queryKey: ["material-records"], queryFn: () => getData<PageResult<MaterialRecord>>("/material-records", { page: 1, pageSize: 200 }) });
  const save = useMutation({ mutationFn: () => form.id ? putData(`/material-records/${form.id}`, form) : postData("/material-records", form), onSuccess: () => { setForm({ recordType: "arrival", recordDate: new Date().toISOString().slice(0, 10), quantity: 0, unitPrice: 0 }); queryClient.invalidateQueries({ queryKey: ["material-records"] }); queryClient.invalidateQueries({ queryKey: ["project-dashboard"] }); } });
  return <Page title="材料辅材管理">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}><Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="材料"><TextInput value={form.materialName ?? ""} onChange={e => setForm({ ...form, materialName: e.target.value })} /></Field><Field label="类型"><SelectInput value={form.recordType ?? "arrival"} onChange={e => setForm({ ...form, recordType: e.target.value })}><option value="arrival">到货</option><option value="usage">使用</option><option value="impact">影响施工</option></SelectInput></Field><Field label="日期"><TextInput type="date" value={form.recordDate ?? ""} onChange={e => setForm({ ...form, recordDate: e.target.value })} /></Field><Field label="数量"><TextInput type="number" value={form.quantity ?? 0} onChange={e => setForm({ ...form, quantity: Number(e.target.value) })} /></Field><Field label="单位"><TextInput value={form.unit ?? ""} onChange={e => setForm({ ...form, unit: e.target.value })} /></Field><Field label="单价"><TextInput type="number" value={form.unitPrice ?? 0} onChange={e => setForm({ ...form, unitPrice: Number(e.target.value) })} /></Field><Field label="影响说明"><TextInput value={form.impactNote ?? ""} onChange={e => setForm({ ...form, impactNote: e.target.value })} /></Field><Button type="submit">保存</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>材料</th><th>类型</th><th>日期</th><th>数量</th><th>金额</th><th>影响说明</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{row.materialName}</td><td>{row.recordType}</td><td>{row.recordDate}</td><td>{row.quantity} {row.unit ?? ""}</td><td>{money(row.amount)}</td><td>{row.impactNote ?? "-"}</td><td><Button variant="secondary" onClick={() => setForm(row)}>编辑</Button></td></tr>)}</tbody></table></div>
  </Page>;
}

export function DocumentArchivePage() {
  const queryClient = useQueryClient();
  const lookups = useLookups();
  const [form, setForm] = useState<Partial<DocumentArchive>>({ archiveDate: new Date().toISOString().slice(0, 10), documentType: "现场照片" });
  const [file, setFile] = useState<File | undefined>();
  const { data } = useQuery({ queryKey: ["project-document-archives"], queryFn: () => getData<PageResult<DocumentArchive>>("/project-document-archives", { page: 1, pageSize: 200 }) });
  const save = useMutation({
    mutationFn: async () => {
      const uploaded = file ? await uploadFile(file) : undefined;
      return form.id ? putData(`/project-document-archives/${form.id}`, { ...form, fileId: uploaded?.id ?? form.fileId }) : postData("/project-document-archives", { ...form, fileId: uploaded?.id });
    },
    onSuccess: () => { setForm({ archiveDate: new Date().toISOString().slice(0, 10), documentType: "现场照片" }); setFile(undefined); queryClient.invalidateQueries({ queryKey: ["project-document-archives"] }); }
  });
  return <Page title="资料照片归档">
    <form className="panel toolbar" onSubmit={(event: FormEvent) => { event.preventDefault(); save.mutate(); }}><Field label="项目"><SelectInput value={form.projectId ?? ""} onChange={e => setForm({ ...form, projectId: e.target.value })}>{lookups.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="施工段"><SelectInput value={form.projectSectionId ?? ""} onChange={e => setForm({ ...form, projectSectionId: e.target.value || undefined })}><option value="">无</option>{lookups.sections.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</SelectInput></Field><Field label="工序"><SelectInput value={form.workProcessId ?? ""} onChange={e => setForm({ ...form, workProcessId: e.target.value || undefined })}><option value="">无</option>{lookups.processes.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field><Field label="日期"><TextInput type="date" value={form.archiveDate ?? ""} onChange={e => setForm({ ...form, archiveDate: e.target.value })} /></Field><Field label="资料类型"><SelectInput value={form.documentType ?? "现场照片"} onChange={e => setForm({ ...form, documentType: e.target.value })}>{["现场照片","签证资料","隐蔽工程照片","验收资料","安全整改","其他"].map(item => <option key={item} value={item}>{item}</option>)}</SelectInput></Field><Field label="上传人"><TextInput value={form.uploaderName ?? ""} onChange={e => setForm({ ...form, uploaderName: e.target.value })} /></Field><Field label="附件"><input type="file" onChange={e => setFile(e.target.files?.[0])} /></Field><Button type="submit">归档</Button></form>
    <div className="panel table-wrap"><table><thead><tr><th>项目</th><th>施工段</th><th>工序</th><th>日期</th><th>类型</th><th>文件</th><th>上传人</th></tr></thead><tbody>{(data?.records ?? []).map(row => <tr key={row.id}><td>{nameOf(lookups.projects, row.projectId)}</td><td>{nameOf(lookups.sections, row.projectSectionId)}</td><td>{nameOf(lookups.processes, row.workProcessId)}</td><td>{row.archiveDate}</td><td>{row.documentType}</td><td>{row.fileId}</td><td>{row.uploaderName ?? "-"}</td></tr>)}</tbody></table></div>
  </Page>;
}

export function ProjectRiskPage() {
  const { projects } = useLookups();
  const [projectId, setProjectId] = useState("");
  const selectedId = projectId || String(projects[0]?.id ?? "");
  const { data } = useQuery({ queryKey: ["project-risk-page", selectedId], enabled: Boolean(selectedId), queryFn: () => getData<Dashboard>(`/project-dashboard/${selectedId}`) });
  return <Page title="项目风险预警">
    <div className="panel toolbar"><Field label="项目"><SelectInput value={selectedId} onChange={e => setProjectId(e.target.value)}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field></div>
    <div className="panel table-wrap"><table><thead><tr><th>级别</th><th>类型</th><th>风险</th></tr></thead><tbody>{(data?.risks ?? []).map(risk => <tr key={`${risk.type}-${risk.message}`}><td><span className={risk.level === "danger" ? "badge badge-red" : "badge badge-yellow"}>{risk.level}</span></td><td>{risk.type}</td><td>{risk.message}</td></tr>)}</tbody></table></div>
  </Page>;
}

export function ProjectDashboardPage() {
  const queryClient = useQueryClient();
  const { projects } = useLookups();
  const [projectId, setProjectId] = useState("");
  const [cost, setCost] = useState({ costDate: new Date().toISOString().slice(0, 10), costType: "其他成本", amount: 0, description: "" });
  const selectedId = projectId || String(projects[0]?.id ?? "");
  const { data } = useQuery({ queryKey: ["project-dashboard", selectedId], enabled: Boolean(selectedId), queryFn: () => getData<Dashboard>(`/project-dashboard/${selectedId}`) });
  const costs = useQuery({ queryKey: ["project-cost-records", selectedId], enabled: Boolean(selectedId), queryFn: () => getData<PageResult<CostRecord>>("/project-cost-records", { page: 1, pageSize: 100, projectId: selectedId }) });
  const saveCost = useMutation({
    mutationFn: () => postData("/project-cost-records", { ...cost, projectId: selectedId }),
    onSuccess: () => {
      setCost({ costDate: new Date().toISOString().slice(0, 10), costType: "其他成本", amount: 0, description: "" });
      queryClient.invalidateQueries({ queryKey: ["project-dashboard", selectedId] });
      queryClient.invalidateQueries({ queryKey: ["project-cost-records", selectedId] });
    }
  });
  const cards = useMemo(() => [
    ["合同金额", money(data?.contractAmount)],
    ["确认收入", money(data?.revenueAmount)],
    ["应收款", money(data?.receivableAmount)],
    ["已开票", money(data?.invoicedAmount)],
    ["实际回款", money(data?.receivedAmount)],
    ["回款进度", `${Number(data?.paymentProgress ?? 0).toFixed(1)}%`],
    ["质保金余额", money(data?.retentionBalanceAmount)],
    ["已完成产值", money(data?.completedOutputValue)],
    ["已确认签证", money(data?.confirmedVariationAmount)],
    ["已确认工程量", data?.confirmedQuantity ?? 0],
    ["班组应付", money(data?.teamPayableAmount)],
    ["班组已付", money(data?.teamPaidAmount)],
    ["工资分摊", money(data?.salaryAllocatedCostAmount)],
    ["机械成本", money(data?.machineryCostAmount)],
    ["材料成本", money(data?.materialCostAmount)],
    ["其他成本", money(data?.otherCostAmount)],
    ["总成本", money(data?.totalCostAmount)],
    ["成本率", percent(data?.costRate)],
    ["当前毛利", money(data?.grossProfit)],
    ["毛利率", percent(data?.profitRate)],
    ["现金流入", money(data?.cashInAmount)],
    ["现金流出", money(data?.cashOutAmount)],
    ["现金余额", money(data?.cashBalanceAmount)],
    ["待确认签证", money(data?.pendingVariationAmount)]
  ], [data]);
  return <Page title="项目经营看板">
    <div className="panel toolbar"><Field label="项目"><SelectInput value={selectedId} onChange={e => setProjectId(e.target.value)}>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</SelectInput></Field></div>
    <div className="grid grid-3">{cards.map(([label, value]) => <div className="card" key={label}><div className="muted">{label}</div><h3>{value}</h3></div>)}</div>
    <form className="panel toolbar" style={{ marginTop: 16 }} onSubmit={(event: FormEvent) => { event.preventDefault(); saveCost.mutate(); }}>
      <Field label="成本日期"><TextInput type="date" value={cost.costDate} onChange={e => setCost({ ...cost, costDate: e.target.value })} /></Field>
      <Field label="成本类型"><SelectInput value={cost.costType} onChange={e => setCost({ ...cost, costType: e.target.value })}><option value="员工工资分摊">员工工资分摊</option><option value="材料辅材">材料辅材</option><option value="机械台班">机械台班</option><option value="报销费用">报销费用</option><option value="其他成本">其他成本</option></SelectInput></Field>
      <Field label="金额"><TextInput type="number" value={cost.amount} onChange={e => setCost({ ...cost, amount: Number(e.target.value) })} /></Field>
      <Field label="说明"><TextInput value={cost.description} onChange={e => setCost({ ...cost, description: e.target.value })} /></Field>
      <Button type="submit">登记成本</Button>
    </form>
    <div className="panel table-wrap" style={{ marginTop: 16 }}>
      <table><thead><tr><th>收入项</th><th>金额</th><th>占比</th><th>来源</th></tr></thead><tbody>{(data?.revenueBreakdowns ?? []).map(row => <tr key={row.category}><td>{row.category}</td><td>{money(row.amount)}</td><td>{percent(row.ratio)}</td><td>{row.source}</td></tr>)}</tbody></table>
    </div>
    <div className="panel table-wrap" style={{ marginTop: 16 }}>
      <table><thead><tr><th>成本项</th><th>金额</th><th>占比</th><th>来源</th></tr></thead><tbody>{(data?.costBreakdowns ?? []).map(row => <tr key={row.category}><td>{row.category}</td><td>{money(row.amount)}</td><td>{percent(row.ratio)}</td><td>{row.source}</td></tr>)}</tbody></table>
    </div>
    <div className="panel table-wrap" style={{ marginTop: 16 }}>
      <table><thead><tr><th>现金流</th><th>金额</th><th>占比</th><th>来源</th></tr></thead><tbody>{(data?.cashFlowBreakdowns ?? []).map(row => <tr key={row.category}><td>{row.category}</td><td>{money(row.amount)}</td><td>{percent(row.ratio)}</td><td>{row.source}</td></tr>)}</tbody></table>
    </div>
    <div className="panel table-wrap" style={{ marginTop: 16 }}><table><thead><tr><th>日期</th><th>类型</th><th>金额</th><th>说明</th></tr></thead><tbody>{(costs.data?.records ?? []).map(row => <tr key={row.id}><td>{row.costDate}</td><td>{row.costType}</td><td>{money(row.amount)}</td><td>{row.description ?? "-"}</td></tr>)}</tbody></table></div>
    <div className="panel table-wrap" style={{ marginTop: 16 }}><table><thead><tr><th>级别</th><th>风险</th></tr></thead><tbody>{(data?.risks ?? []).map(risk => <tr key={risk.type}><td><span className={risk.level === "danger" ? "badge badge-red" : "badge badge-yellow"}>{risk.level}</span></td><td>{risk.message}</td></tr>)}</tbody></table></div>
  </Page>;
}
