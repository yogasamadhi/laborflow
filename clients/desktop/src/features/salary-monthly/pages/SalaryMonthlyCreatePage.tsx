import { useMutation, useQuery } from "@tanstack/react-query";
import { CalendarPlus, RefreshCw } from "lucide-react";
import { FormEvent, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getAllPages, postData } from "../../../lib/axios";
import type { Department } from "../../department/types";
import { defaultDepartmentalPayrollTitle, monthlyCreatePayload } from "../departmentalPayroll";

function currentMonth() {
  return new Date().toISOString().slice(0, 7);
}

type RuleKey = "includeFixedSalary" | "includeAttendanceDeduction" | "includePerformanceSalary" | "includeProjectBonus";

const generationRules: Array<{ key: RuleKey; label: string }> = [
  { key: "includeFixedSalary", label: "固定工资" },
  { key: "includeAttendanceDeduction", label: "考勤扣款" },
  { key: "includePerformanceSalary", label: "绩效工资" },
  { key: "includeProjectBonus", label: "项目奖金" }
];

export function SalaryMonthlyCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    salaryMonth: currentMonth(),
    departmentId: "",
    title: "",
    remark: "",
    includeFixedSalary: 1,
    includeAttendanceDeduction: 1,
    includePerformanceSalary: 1,
    includeProjectBonus: 1
  });
  const [autoGenerate, setAutoGenerate] = useState(true);
  const [error, setError] = useState("");
  const [createdMonthlyId, setCreatedMonthlyId] = useState<string | null>(null);
  const { data: departmentsData } = useQuery({
    queryKey: ["departments", "salary-monthly-create"],
    queryFn: () => getAllPages<Department>("/reference/departments", { status: "active" })
  });
  const departments = departmentsData ?? [];
  const departmentName = departments.find((department) => department.id === form.departmentId)?.name ?? "";
  const defaultTitle = useMemo(() => defaultDepartmentalPayrollTitle(form.salaryMonth || currentMonth(), departmentName || "部门"), [departmentName, form.salaryMonth]);
  const save = useMutation({
    mutationFn: async () => {
      const monthly = await postData<{ id: string }>("/salary/monthly", monthlyCreatePayload(form, departmentName));
      setCreatedMonthlyId(monthly.id);
      if (autoGenerate) {
        try {
          await postData(`/salary/monthly/${monthly.id}/generate`);
        } catch (reason) {
          const message = reason instanceof Error ? reason.message : "生成工资明细失败";
          throw new Error(`工资表已创建，但自动生成失败：${message}`);
        }
      }
      return monthly;
    },
    onSuccess: (data) => navigate(`/salary/monthly/${data.id}`),
    onError: (reason) => setError(reason instanceof Error ? reason.message : "创建失败")
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (!generationRules.some((rule) => form[rule.key] === 1)) {
      setError("至少启用一项工资生成规则");
      return;
    }
    if (!form.departmentId) {
      setError("请选择部门");
      return;
    }
    save.mutate();
  }

  function toggleRule(key: RuleKey) {
    setForm((current) => ({ ...current, [key]: current[key] === 1 ? 0 : 1 }));
  }

  const enabledRules = generationRules.filter((rule) => form[rule.key] === 1).map((rule) => rule.label).join("、") || "未启用";

  return (
    <Page title="创建部门工资表">
      <form className="panel grid create-monthly-form" onSubmit={submit}>
        {error && <div className="form-error">{error}</div>}
        {createdMonthlyId && error && (
          <div>
            <Link to={`/salary/monthly/${createdMonthlyId}`}>
              <Button type="button" variant="secondary">进入已创建的工资表</Button>
            </Link>
          </div>
        )}
        <div className="grid grid-2">
          <Field label="工资月份">
            <TextInput type="month" value={form.salaryMonth} onChange={(event) => setForm({ ...form, salaryMonth: event.target.value })} required />
          </Field>
          <Field label="标题">
            <TextInput placeholder={defaultTitle} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          </Field>
          <Field label="直属部门">
            <SelectInput value={form.departmentId} onChange={(event) => setForm({ ...form, departmentId: event.target.value })} required>
              <option value="">请选择部门</option>
              {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
            </SelectInput>
          </Field>
        </div>
        <Field label="生成方式">
          <SelectInput value={autoGenerate ? "auto" : "manual"} onChange={(event) => setAutoGenerate(event.target.value === "auto")}>
            <option value="auto">创建后立即按工资档案生成明细</option>
            <option value="manual">只创建部门工资表，稍后手动生成</option>
          </SelectInput>
        </Field>
        <Field label="生成规则">
          <div className="checkbox-chips">
            {generationRules.map((rule) => (
              <label className="checkbox-chip" key={rule.key}>
                <input type="checkbox" checked={form[rule.key] === 1} onChange={() => toggleRule(rule.key)} />
                {rule.label}
              </label>
            ))}
          </div>
        </Field>
        <Field label="备注">
          <TextArea value={form.remark} onChange={(event) => setForm({ ...form, remark: event.target.value })} />
        </Field>
        <div className="summary-strip">
          <span>月份：{form.salaryMonth || "-"}</span>
          <span>部门：{departmentName || "-"}</span>
          <span>标题：{form.title || defaultTitle}</span>
          <span>规则：{enabledRules}</span>
          <strong>{autoGenerate ? "会立即生成工资明细" : "只保存草稿工资表"}</strong>
        </div>
        <div className="toolbar">
          <Button type="submit" disabled={save.isPending}>
            {autoGenerate ? <RefreshCw size={16} /> : <CalendarPlus size={16} />}
            {save.isPending ? "处理中" : "创建工资表"}
          </Button>
        </div>
      </form>
    </Page>
  );
}
