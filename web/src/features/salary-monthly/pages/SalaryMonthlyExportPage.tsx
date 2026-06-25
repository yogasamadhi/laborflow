import { Download } from "lucide-react";
import { useParams } from "react-router-dom";
import { Page } from "../../../components/common/Page";
import { Button } from "../../../components/ui/Button";
import { downloadFile } from "../../../lib/axios";

export function SalaryMonthlyExportPage() {
  const { id } = useParams();
  const monthlyId = id ?? "";
  const exports = [
    { label: "月度工资表", url: `/salary/monthly/${monthlyId}/export`, filename: `月度工资表-${monthlyId}.xlsx` },
    { label: "部门工资汇总", url: `/salary/monthly/${monthlyId}/export/department-summary`, filename: `部门工资汇总-${monthlyId}.xlsx` },
    { label: "工资发放记录", url: `/salary/monthly/${monthlyId}/export/payment-records`, filename: `工资发放记录-${monthlyId}.xlsx` }
  ];

  return (
    <Page title="工资表导出">
      <div className="panel">
        <div className="toolbar">
          {exports.map((item) => (
            <Button key={item.url} onClick={() => downloadFile(item.url, item.filename)}>
              <Download size={16} />
              {item.label}
            </Button>
          ))}
        </div>
      </div>
    </Page>
  );
}
