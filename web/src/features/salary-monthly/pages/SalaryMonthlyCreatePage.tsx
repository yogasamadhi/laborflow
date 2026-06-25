import { useMutation } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Field, TextArea, TextInput } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { postData } from "../../../lib/axios";

export function SalaryMonthlyCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ salaryMonth: "", title: "", remark: "" });
  const save = useMutation({ mutationFn: () => postData<{ id: number }>("/salary/monthly", form), onSuccess: (data) => navigate(`/salary/monthly/${data.id}`) });
  function submit(event: FormEvent) { event.preventDefault(); save.mutate(); }
  return (
    <Page title="创建工资月份">
      <form className="panel grid" onSubmit={submit}>
        <Field label="工资月份"><TextInput placeholder="2026-06" value={form.salaryMonth} onChange={(e) => setForm({ ...form, salaryMonth: e.target.value })} required /></Field>
        <Field label="标题"><TextInput value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></Field>
        <Field label="备注"><TextArea value={form.remark} onChange={(e) => setForm({ ...form, remark: e.target.value })} /></Field>
        <Button type="submit">创建</Button>
      </form>
    </Page>
  );
}

