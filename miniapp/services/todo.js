const salary = require("./salary");
const project = require("./project");
const format = require("../utils/format");

async function list() {
  const [monthlies, submittedReports, pmReports, variations, settlements] = await Promise.all([
    salary.monthlyList({ status: "pending_approval" }).catch(() => ({ records: [] })),
    project.dailyReports({ status: "submitted" }).catch(() => ({ records: [] })),
    project.dailyReports({ status: "pm_confirmed" }).catch(() => ({ records: [] })),
    project.variationOrders({ status: "submitted" }).catch(() => ({ records: [] })),
    project.settlements().catch(() => ({ records: [] }))
  ]);

  const rows = [];
  format.pageRecords(monthlies).forEach((item) => rows.push({
    id: "salary-" + item.id,
    type: "salary",
    businessId: item.id,
    title: item.title || item.salaryMonth + " 工资表",
    desc: "待审核工资表",
    status: item.status
  }));
  format.pageRecords(submittedReports).forEach((item) => rows.push({
    id: "report-pm-" + item.id,
    type: "report_pm",
    businessId: item.id,
    title: "工程量日报 #" + item.id,
    desc: "待项目经理确认",
    status: item.status
  }));
  format.pageRecords(pmReports).forEach((item) => rows.push({
    id: "report-final-" + item.id,
    type: "report_final",
    businessId: item.id,
    title: "工程量日报 #" + item.id,
    desc: "待终审确认",
    status: item.status
  }));
  format.pageRecords(variations).forEach((item) => rows.push({
    id: "variation-" + item.id,
    type: "variation",
    businessId: item.id,
    title: "签证上报 #" + item.id,
    desc: item.variationType || "待确认签证",
    status: item.status
  }));
  format.pageRecords(settlements)
    .filter((item) => item.settlementStatus === "draft")
    .forEach((item) => rows.push({
      id: "settlement-" + item.id,
      type: "settlement",
      businessId: item.id,
      title: "班组结算 #" + item.id,
      desc: item.settlementMonth || "待审核结算",
      status: item.settlementStatus
    }));
  return rows;
}

async function approve(item, remark) {
  if (item.type === "salary") return salary.approveMonthly(item.businessId, remark);
  if (item.type === "report_pm") return project.confirmDailyReport(item.businessId, "pm", { remark });
  if (item.type === "report_final") return project.confirmDailyReport(item.businessId, "final", { remark });
  if (item.type === "variation") return project.confirmVariationOrder(item.businessId, { remark });
  if (item.type === "settlement") return project.confirmSettlement(item.businessId, { remark });
  throw new Error("不支持的待办类型");
}

async function reject(item, remark) {
  if (item.type === "salary") return salary.rejectMonthly(item.businessId, remark);
  if (item.type === "report_pm" || item.type === "report_final") return project.rejectDailyReport(item.businessId, { remark });
  if (item.type === "variation") return project.rejectVariationOrder(item.businessId, { remark });
  if (item.type === "settlement") return project.rejectSettlement(item.businessId, { remark });
  throw new Error("当前待办不支持驳回");
}

module.exports = {
  list,
  approve,
  reject
};
