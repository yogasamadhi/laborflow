import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useMemo, useState } from "react";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { deleteData, getData, PageResult, postData, putData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import type { Department } from "../types";

type DepartmentForm = {
  id?: string;
  name: string;
  parentId: string;
  sortOrder: string;
  status: string;
};

const emptyForm: DepartmentForm = {
  name: "",
  parentId: "",
  sortOrder: "0",
  status: "active"
};

export function DepartmentPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({ keyword: "", status: "" });
  const [form, setForm] = useState<DepartmentForm>(emptyForm);
  const [modalOpen, setModalOpen] = useState(false);

  const { data } = useQuery({
    queryKey: ["departments", filters],
    queryFn: () => getData<PageResult<Department>>("/departments", { ...filters, page: 1, pageSize: 200 })
  });
  const rows = data?.records ?? [];
  const departmentNameMap = useMemo(() => new Map(rows.map((item) => [item.id, item.name])), [rows]);

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name,
        parentId: form.parentId || undefined,
        sortOrder: Number(form.sortOrder || 0),
        status: form.status
      };
      return form.id ? putData(`/departments/${form.id}`, payload) : postData("/departments", payload);
    },
    onSuccess: () => {
      setForm(emptyForm);
      setModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ["departments"] });
    }
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deleteData(`/departments/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["departments"] })
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    saveMutation.mutate();
  }

  function create() {
    setForm(emptyForm);
    setModalOpen(true);
  }

  function edit(row: Department) {
    setForm({
      id: row.id,
      name: row.name,
      parentId: row.parentId ? String(row.parentId) : "",
      sortOrder: String(row.sortOrder ?? 0),
      status: row.status ?? "active"
    });
    setModalOpen(true);
  }

  return (
    <Page title="部门管理" actions={<Button onClick={create}>新增部门</Button>}>
      <div className="panel toolbar">
        <Field label="部门名称">
          <TextInput value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} />
        </Field>
        <Field label="状态">
          <SelectInput value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="">全部</option>
            <option value="active">启用</option>
            <option value="inactive">停用</option>
          </SelectInput>
        </Field>
      </div>

      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>部门名称</th>
              <th>上级部门</th>
              <th>排序</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.name}</td>
                <td>{row.parentId ? departmentNameMap.get(row.parentId) ?? row.parentId : "-"}</td>
                <td>{row.sortOrder ?? 0}</td>
                <td><span className={`badge ${row.status === "active" ? "badge-green" : "badge-yellow"}`}>{statusText(row.status)}</span></td>
                <td>{row.createdAt ?? "-"}</td>
                <td>
                  <Button variant="secondary" onClick={() => edit(row)}>编辑</Button>{" "}
                  {row.status !== "inactive" && <Button variant="danger" onClick={() => deactivateMutation.mutate(row.id)}>停用</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal
        open={modalOpen}
        title={form.id ? "编辑部门" : "新增部门"}
        onClose={() => setModalOpen(false)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>取消</Button>
            <Button type="submit" form="department-form" disabled={saveMutation.isPending}>{form.id ? "保存" : "新增"}</Button>
          </>
        }
      >
        <form id="department-form" className="grid" onSubmit={submit}>
          <div className="grid grid-2">
            <Field label="部门名称">
              <TextInput value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
            </Field>
            <Field label="上级部门">
              <SelectInput value={form.parentId} onChange={(event) => setForm({ ...form, parentId: event.target.value })}>
                <option value="">无</option>
                {rows.filter((item) => item.id !== form.id).map((item) => (
                  <option key={item.id} value={item.id}>{item.name}</option>
                ))}
              </SelectInput>
            </Field>
            <Field label="排序">
              <TextInput type="number" value={form.sortOrder} onChange={(event) => setForm({ ...form, sortOrder: event.target.value })} />
            </Field>
            <Field label="状态">
              <SelectInput value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
                <option value="active">启用</option>
                <option value="inactive">停用</option>
              </SelectInput>
            </Field>
          </div>
        </form>
      </Modal>
    </Page>
  );
}
