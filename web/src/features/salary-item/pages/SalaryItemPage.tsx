import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { deleteData, postData, putData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";

type SalaryItem = { id: string; name: string; type: string; isFixed: number; calculationRole: "fixed_component" | "performance_base"; status: string; sortOrder: number };

export function SalaryItemPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<SalaryItem>>({ type: "earning", isFixed: 1, calculationRole: "fixed_component", status: "active", sortOrder: 0 });
  const listing = usePagedQuery<SalaryItem>(["salary-items"], "/salary/items");
  const data = listing.data;
  const save = useMutation({
    mutationFn: () => form.id ? putData(`/salary/items/${form.id}`, form) : postData("/salary/items", form),
    onSuccess: () => { setForm({ type: "earning", isFixed: 1, calculationRole: "fixed_component", status: "active", sortOrder: 0 }); queryClient.invalidateQueries({ queryKey: ["salary-items"] }); }
  });
  function submit(event: FormEvent) { event.preventDefault(); save.mutate(); }
  return (
    <Page title="工资项设置">
      <form className="panel toolbar" onSubmit={submit}>
        <Field label="名称"><TextInput value={form.name ?? ""} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
        <Field label="类型"><SelectInput value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}><option value="earning">应发项</option><option value="deduction">扣减项</option></SelectInput></Field>
        <Field label="计算角色"><SelectInput value={form.calculationRole ?? "fixed_component"} onChange={(e) => setForm({ ...form, calculationRole: e.target.value as SalaryItem["calculationRole"], isFixed: e.target.value === "performance_base" ? 0 : form.isFixed })}><option value="fixed_component">固定工资项</option><option value="performance_base">绩效计算基数</option></SelectInput></Field>
        <Button type="submit">{form.id ? "保存" : "新增"}</Button>
      </form>
      <div className="panel table-wrap"><table><thead><tr><th>名称</th><th>类型</th><th>计算角色</th><th>状态</th><th>操作</th></tr></thead><tbody>{(data?.records ?? []).map(item => <tr key={item.id}><td>{item.name}</td><td>{item.type === "earning" ? "应发项" : "扣减项"}</td><td>{item.calculationRole === "performance_base" ? "绩效计算基数" : "固定工资项"}</td><td>{statusText(item.status)}</td><td><Button variant="secondary" onClick={() => setForm(item)}>编辑</Button>{" "}<Button variant="danger" onClick={() => deleteData(`/salary/items/${item.id}`).then(() => queryClient.invalidateQueries({ queryKey: ["salary-items"] }))}>停用</Button></td></tr>)}</tbody></table></div>
      {listing.pagination}
    </Page>
  );
}
