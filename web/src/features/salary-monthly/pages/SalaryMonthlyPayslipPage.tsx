import { useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { useState } from "react";
import { useParams } from "react-router-dom";
import { Field } from "../../../components/common/Field";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile, getData, PageResult } from "../../../lib/axios";

type Detail = { id: string; employeeId: string };

export function SalaryMonthlyPayslipPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const [employeeId, setEmployeeId] = useState("");
  const { data } = useQuery({ queryKey: ["salary-payslip-details", id], queryFn: () => getData<PageResult<Detail>>(`/salary/monthly/${id}/details`, { page: 1, pageSize: 500 }) });

  return (
    <Page title="工资条导出">
      <div className="panel">
        <div className="toolbar">
          <Field label="员工">
            <select className="select" value={employeeId} onChange={(event) => setEmployeeId(event.target.value)}>
              <option value="">全部员工</option>
              {(data?.records ?? []).map((detail) => <option key={detail.id} value={detail.employeeId}>员工 ID：{detail.employeeId}</option>)}
            </select>
          </Field>
          <Button onClick={() => downloadFile(`/salary/monthly/${monthlyId}/payslip`, `工资条-${monthlyId}.xlsx`, employeeId ? { employeeId } : undefined)}>
            <Download size={16} />
            导出工资条
          </Button>
        </div>
      </div>
    </Page>
  );
}
