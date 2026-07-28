import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { usePagedQuery } from "../../../components/common/usePagedQuery";
import { Button } from "../../../components/ui/Button";
import { getAllPages, putData } from "../../../lib/axios";
import { employeeStatusText } from "../../../lib/utils";
import type { Department } from "../../department/types";
import type { Position } from "../../position/types";
import type { Employee } from "../types";

export function EmployeeListPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({ keyword: "", status: "", departmentId: "", positionId: "" });
  const listing = usePagedQuery<Employee>(["employees", filters], "/employees", filters);
  const data = listing.data;
  const { data: departmentsData } = useQuery({
    queryKey: ["departments", "options"],
    queryFn: () => getAllPages<Department>("/departments", { status: "active" })
  });
  const { data: positionsData } = useQuery({
    queryKey: ["positions", "options", filters.departmentId],
    queryFn: () => getAllPages<Position>("/positions", { status: "active", departmentId: filters.departmentId || undefined })
  });
  const departments = departmentsData ?? [];
  const positions = positionsData ?? [];
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => putData(`/employees/${id}/status`, { status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["employees"] })
  });

  return (
    <Page
      title="员工信息"
      actions={
        <Link to="/employees/create">
          <Button>
            <Plus size={16} />
            新增员工
          </Button>
        </Link>
      }
    >
      <div className="panel toolbar">
        <Field label="姓名/手机号">
          <TextInput value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} />
        </Field>
        <Field label="状态">
          <SelectInput value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="">全部</option>
            <option value="active">在职</option>
            <option value="inactive">停用</option>
            <option value="left">离职</option>
          </SelectInput>
        </Field>
        <Field label="部门">
          <SelectInput value={filters.departmentId} onChange={(event) => setFilters({ ...filters, departmentId: event.target.value, positionId: "" })}>
            <option value="">全部</option>
            {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
          </SelectInput>
        </Field>
        <Field label="岗位">
          <SelectInput value={filters.positionId} onChange={(event) => setFilters({ ...filters, positionId: event.target.value })}>
            <option value="">全部</option>
            {positions.map((position) => <option key={position.id} value={position.id}>{position.name}</option>)}
          </SelectInput>
        </Field>
      </div>
      <div className="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>姓名</th>
              <th>手机号</th>
              <th>部门</th>
              <th>岗位</th>
              <th>员工类型</th>
              <th>状态</th>
              <th>银行卡</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {(data?.records ?? []).map((employee) => (
              <tr key={employee.id}>
                <td><Link to={`/employees/${employee.id}`}>{employee.name}</Link></td>
                <td>{employee.phone ?? "-"}</td>
                <td>{employee.departmentName ?? (employee.departmentId ? "已删除部门" : "-")}</td>
                <td>{employee.positionName ?? (employee.positionId ? "已删除岗位" : "-")}</td>
                <td>{employee.employeeType}</td>
                <td>
                  <span className={`badge ${employee.status === "active" ? "badge-green" : employee.status === "left" ? "badge-red" : "badge-yellow"}`}>
                    {employeeStatusText(employee.status)}
                  </span>
                </td>
                <td>{employee.bankAccount ?? "-"}</td>
                <td>
                  <Link to={`/employees/${employee.id}/edit`}><Button variant="secondary">编辑</Button></Link>{" "}
                  <Button variant="secondary" onClick={() => statusMutation.mutate({ id: employee.id, status: "inactive" })}>停用</Button>{" "}
                  <Button variant="danger" onClick={() => statusMutation.mutate({ id: employee.id, status: "left" })}>离职</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {listing.pagination}
    </Page>
  );
}
