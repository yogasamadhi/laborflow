import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { FormEvent, useState } from "react";
import { deleteData, getData, PageResult, postData, putData } from "../../lib/axios";
import { statusText } from "../../lib/utils";
import { Button } from "../ui/Button";
import { Field, TextInput } from "./Field";
import { Page } from "./Page";

type Row = {
  id: string;
  name?: string;
  code?: string;
  status?: string;
  sortOrder?: number;
  createdAt?: string;
};

export type SimpleCrudPageProps = { title: string; resource: string; readonly?: boolean };

export function SimpleCrudPage({ title, resource, readonly = false }: SimpleCrudPageProps) {
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Row | null>(null);
  const [name, setName] = useState("");
  const { data } = useQuery({
    queryKey: [resource],
    queryFn: () => getData<PageResult<Row> | Row[]>(`/${resource}`, { page: 1, pageSize: 100 })
  });
  const rows = Array.isArray(data) ? data : data?.records ?? [];
  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = { ...editing, name, status: editing?.status ?? "active" };
      return editing?.id ? putData(`/${resource}/${editing.id}`, payload) : postData(`/${resource}`, payload);
    },
    onSuccess: () => {
      setEditing(null);
      setName("");
      queryClient.invalidateQueries({ queryKey: [resource] });
    }
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    saveMutation.mutate();
  }

  return (
    <Page title={title}>
      {!readonly && (
        <form className="panel toolbar" onSubmit={submit}>
          <Field label="名称">
            <TextInput value={name} onChange={(event) => setName(event.target.value)} required />
          </Field>
          <Button type="submit">
            <Save size={16} />
            {editing ? "保存" : "新增"}
          </Button>
        </form>
      )}
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>编码</th>
              <th>状态</th>
              <th>创建时间</th>
              {!readonly && <th>操作</th>}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.name ?? "-"}</td>
                <td>{row.code ?? "-"}</td>
                <td>{statusText(row.status)}</td>
                <td>{row.createdAt ?? "-"}</td>
                {!readonly && (
                  <td>
                    <Button variant="secondary" onClick={() => { setEditing(row); setName(row.name ?? ""); }}>编辑</Button>{" "}
                    <Button variant="danger" onClick={() => deleteData(`/${resource}/${row.id}`).then(() => queryClient.invalidateQueries({ queryKey: [resource] }))}>停用</Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Page>
  );
}
