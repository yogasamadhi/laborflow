import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { getData } from "../../../lib/axios";
import { statusText } from "../../../lib/utils";
import type { Employee } from "../types";

export function EmployeeDetailPage() {
  const { id } = useParams();
  const { data } = useQuery({ queryKey: ["employee", id], queryFn: () => getData<Employee>(`/employees/${id}`) });
  const rows = [
    ["姓名", data?.name],
    ["手机号", data?.phone],
    ["身份证号", data?.idCard],
    ["部门", data?.departmentName ?? data?.departmentId],
    ["岗位", data?.positionName ?? data?.positionId],
    ["员工类型", data?.employeeType],
    ["状态", statusText(data?.status)],
    ["银行卡号", data?.bankAccount],
    ["开户行", data?.bankName],
    ["紧急联系人", data?.emergencyContact],
    ["紧急联系人电话", data?.emergencyPhone],
    ["备注", data?.remark]
  ];

  return (
    <Page title="员工详情" actions={<><Link to={`/employees/${id}/salary-history`}><Button>历史工资</Button></Link>{" "}<Link to={`/employees/${id}/edit`}><Button variant="secondary">编辑</Button></Link></>}>
      <div className="panel">
        <table>
          <tbody>
            {rows.map(([label, value]) => <tr key={label}><th>{label}</th><td>{value ?? "-"}</td></tr>)}
          </tbody>
        </table>
      </div>
    </Page>
  );
}
