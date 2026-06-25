import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Field, SelectInput, TextInput } from "../../../components/common/Field";
import { Button } from "../../../components/ui/Button";
import { getData, PageResult, putData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import type { Employee } from "../types";

export function EmployeeListPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({ keyword: "", status: "", departmentId: "", positionId: "" });
  const { data } = useQuery({
    queryKey: ["employees", filters],
    queryFn: () => getData<PageResult<Employee>>("/employees", { ...filters, page: 1, pageSize: 50 })
  });
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => putData(`/employees/${id}/status`, { status }),
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
                <td>{employee.departmentName ?? employee.departmentId ?? "-"}</td>
                <td>{employee.positionName ?? employee.positionId ?? "-"}</td>
                <td>{employee.employeeType}</td>
                <td><span className="badge badge-green">{statusText(employee.status)}</span></td>
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
    </Page>
  );
}

