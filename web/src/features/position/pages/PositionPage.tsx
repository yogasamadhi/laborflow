import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useMemo, useState } from "react";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Modal } from "../../../components/common/Modal";
import { Page } from "../../../components/common/Page";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { deleteData, getAllPages, postData, putData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import type { Department } from "../../department/types";
import type { Position } from "../types";

type PositionForm = {
  id?: string;
  name: string;
  departmentId: string;
  sortOrder: string;
  status: string;
};

const emptyForm: PositionForm = {
  name: "",
  departmentId: "",
  sortOrder: "0",
  status: "active"
};

function errorMessage(error: unknown) {
  if (error instanceof Error) return error.message;
  if (error && typeof error === "object" && "message" in error) {
    return String((error as { message?: unknown }).message ?? "保存失败");
  }
  return "保存失败";
}

export function PositionPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({ keyword: "", status: "", departmentId: "" });
  const [form, setForm] = useState<PositionForm>(emptyForm);
  const [modalOpen, setModalOpen] = useState(false);

  const { data: departmentsData } = useQuery({
    queryKey: ["departments", "options"],
    queryFn: () => getAllPages<Department>("/departments")
  });
  const departments = departmentsData ?? [];
  const departmentNameMap = useMemo(() => new Map(departments.map((item) => [item.id, item.name])), [departments]);

  const listing = usePagedQuery<Position>(["positions", filters], "/positions", {
    keyword: filters.keyword,
    status: filters.status,
    departmentId: filters.departmentId || undefined
  });
  const data = listing.data;
  const rows = data?.records ?? [];

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name: form.name,
        departmentId: form.departmentId || undefined,
        sortOrder: Number(form.sortOrder || 0),
        status: form.status
      };
      return form.id ? putData(`/positions/${form.id}`, payload) : postData("/positions", payload);
    },
    onSuccess: () => {
      setForm(emptyForm);
      setModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ["positions"] });
    }
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deleteData(`/positions/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["positions"] })
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    saveMutation.mutate();
  }

  function create() {
    setForm(emptyForm);
    setModalOpen(true);
  }

  function edit(row: Position) {
    setForm({
      id: row.id,
      name: row.name,
      departmentId: row.departmentId ? String(row.departmentId) : "",
      sortOrder: String(row.sortOrder ?? 0),
      status: row.status ?? "active"
    });
    setModalOpen(true);
  }

  return (
    <Page title="岗位管理" actions={<Button onClick={create}>新增岗位</Button>}>
      <div className="panel toolbar">
        <Field label="岗位名称">
          <TextInput value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} />
        </Field>
        <Field label="所属部门">
          <SelectInput value={filters.departmentId} onChange={(event) => setFilters({ ...filters, departmentId: event.target.value })}>
            <option value="">全部</option>
            {departments.map((item) => (
              <option key={item.id} value={item.id}>{item.name}</option>
            ))}
          </SelectInput>
        </Field>
        <Field label="状态">
          <SelectInput value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="">全部</option>
            <option value="active">启用</option>
            <option value="inactive">停用</option>
          </SelectInput>
        </Field>
      </div>
      {listing.pagination}

      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>岗位名称</th>
              <th>所属部门</th>
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
                <td>{row.departmentId ? departmentNameMap.get(row.departmentId) ?? row.departmentId : "-"}</td>
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
        title={form.id ? "编辑岗位" : "新增岗位"}
        onClose={() => setModalOpen(false)}
        footer={
          <>
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>取消</Button>
            <Button type="submit" form="position-form" disabled={saveMutation.isPending}>{form.id ? "保存" : "新增"}</Button>
          </>
        }
      >
        <form id="position-form" className="grid" onSubmit={submit}>
          {saveMutation.isError && (
            <div className="form-error" role="alert">{errorMessage(saveMutation.error)}</div>
          )}
          <div className="grid grid-2">
            <Field label="岗位名称">
              <TextInput value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
            </Field>
            <Field label="所属部门">
              <SelectInput value={form.departmentId} onChange={(event) => setForm({ ...form, departmentId: event.target.value })}>
                <option value="">未指定</option>
                {departments.map((item) => (
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
