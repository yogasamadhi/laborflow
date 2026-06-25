import { useMutation, useQuery } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Field, SelectInput, TextArea, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, postData, putData } from "../../../lib/axios";
import type { Employee } from "../types";

const initial: Partial<Employee> = {
  employeeType: "company_employee",
  status: "active"
};

export function EmployeeFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState<Partial<Employee>>(initial);
  const { data } = useQuery({
    queryKey: ["employee", id],
    queryFn: () => getData<Employee>(`/employees/${id}`),
    enabled: Boolean(id)
  });
  useEffect(() => {
    if (data) setForm(data);
  }, [data]);
  const mutation = useMutation({
    mutationFn: () => (id ? putData(`/employees/${id}`, form) : postData("/employees", form)),
    onSuccess: (saved: unknown) => {
      const nextId = (saved as Employee)?.id ?? id;
      navigate(nextId ? `/employees/${nextId}` : "/employees");
    }
  });

  function set<K extends keyof Employee>(key: K, value: Employee[K]) {
    setForm((old) => ({ ...old, [key]: value }));
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  return (
    <Page title={id ? "编辑员工" : "新增员工"}>
      <form className="panel grid" onSubmit={submit}>
        <div className="grid grid-3">
          <Field label="姓名"><TextInput value={form.name ?? ""} onChange={(e) => set("name", e.target.value)} required /></Field>
          <Field label="手机号"><TextInput value={form.phone ?? ""} onChange={(e) => set("phone", e.target.value)} /></Field>
          <Field label="身份证号"><TextInput value={form.idCard ?? ""} onChange={(e) => set("idCard", e.target.value)} /></Field>
          <Field label="性别"><SelectInput value={form.gender ?? ""} onChange={(e) => set("gender", e.target.value)}><option value="">未填</option><option value="male">男</option><option value="female">女</option></SelectInput></Field>
          <Field label="出生日期"><TextInput type="date" value={form.birthDate ?? ""} onChange={(e) => set("birthDate", e.target.value)} /></Field>
          <Field label="部门 ID"><TextInput type="number" value={form.departmentId ?? ""} onChange={(e) => set("departmentId", Number(e.target.value) || undefined)} /></Field>
          <Field label="岗位 ID"><TextInput type="number" value={form.positionId ?? ""} onChange={(e) => set("positionId", Number(e.target.value) || undefined)} /></Field>
          <Field label="员工类型"><SelectInput value={form.employeeType ?? "company_employee"} onChange={(e) => set("employeeType", e.target.value)}><option value="company_employee">公司员工</option><option value="project_staff">项目人员</option><option value="temporary_worker">临时人员</option><option value="team_leader">班组负责人</option></SelectInput></Field>
          <Field label="员工状态"><SelectInput value={form.status ?? "active"} onChange={(e) => set("status", e.target.value)}><option value="active">在职</option><option value="inactive">停用</option><option value="left">离职</option></SelectInput></Field>
          <Field label="入职日期"><TextInput type="date" value={form.entryDate ?? ""} onChange={(e) => set("entryDate", e.target.value)} /></Field>
          <Field label="离职日期"><TextInput type="date" value={form.leaveDate ?? ""} onChange={(e) => set("leaveDate", e.target.value)} /></Field>
          <Field label="银行卡号"><TextInput value={form.bankAccount ?? ""} onChange={(e) => set("bankAccount", e.target.value)} /></Field>
          <Field label="开户行"><TextInput value={form.bankName ?? ""} onChange={(e) => set("bankName", e.target.value)} /></Field>
          <Field label="紧急联系人"><TextInput value={form.emergencyContact ?? ""} onChange={(e) => set("emergencyContact", e.target.value)} /></Field>
          <Field label="紧急联系人电话"><TextInput value={form.emergencyPhone ?? ""} onChange={(e) => set("emergencyPhone", e.target.value)} /></Field>
        </div>
        <Field label="备注"><TextArea value={form.remark ?? ""} onChange={(e) => set("remark", e.target.value)} /></Field>
        <Button type="submit" disabled={mutation.isPending}><Save size={16} />保存</Button>
      </form>
    </Page>
  );
}

