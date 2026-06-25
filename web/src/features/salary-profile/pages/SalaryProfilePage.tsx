import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { Field, TextArea, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, postData } from "../../../lib/axios";
import { money } from "../../../lib/utils";

type Profile = { id: number; employeeId: number; effectiveDate?: string; status: string; totalAmount?: number; remark?: string };

export function SalaryProfilePage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ employeeId: "", effectiveDate: "", itemsText: "基本工资:earning:0\n请假扣款:deduction:0", remark: "" });
  const { data } = useQuery({ queryKey: ["salary-profiles"], queryFn: () => getData<PageResult<Profile>>("/salary/profiles", { page: 1, pageSize: 100 }) });
  const save = useMutation({
    mutationFn: () => postData("/salary/profiles", {
      employeeId: Number(form.employeeId),
      effectiveDate: form.effectiveDate || undefined,
      remark: form.remark,
      items: form.itemsText.split("\n").filter(Boolean).map((line) => {
        const [salaryItemName, salaryItemType, amount] = line.split(":");
        return { salaryItemName, salaryItemType, amount: Number(amount ?? 0) };
      })
    }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["salary-profiles"] })
  });
  function submit(event: FormEvent) { event.preventDefault(); save.mutate(); }
  return (
    <Page title="员工工资档案">
      <form className="panel grid" onSubmit={submit}>
        <div className="grid grid-2">
          <Field label="员工 ID"><TextInput value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })} required /></Field>
          <Field label="生效日期"><TextInput type="date" value={form.effectiveDate} onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })} /></Field>
        </div>
        <Field label="工资项，每行：名称:earning|deduction:金额"><TextArea value={form.itemsText} onChange={(e) => setForm({ ...form, itemsText: e.target.value })} /></Field>
        <Field label="备注"><TextInput value={form.remark} onChange={(e) => setForm({ ...form, remark: e.target.value })} /></Field>
        <Button type="submit">保存档案</Button>
      </form>
      <div className="panel table-wrap"><table><thead><tr><th>员工 ID</th><th>生效日期</th><th>状态</th><th>档案金额</th></tr></thead><tbody>{(data?.records ?? []).map(profile => <tr key={profile.id}><td>{profile.employeeId}</td><td>{profile.effectiveDate ?? "-"}</td><td>{profile.status}</td><td>{money(profile.totalAmount)}</td></tr>)}</tbody></table></div>
    </Page>
  );
}

