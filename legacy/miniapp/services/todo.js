const request = require("../utils/request");
const project = require("./project");
const salary = require("./salary");

function list(params) {
  return request.get("/todos", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

async function detail(item) {
  if (item.type === "salary") {
    const header = await request.get("/salary/monthly/" + item.businessId);
    const details = await salary.monthlyDetails(item.businessId, { page: 1, pageSize: 20 });
    const audits = await salary.monthlyAudits(item.businessId);
    return { header, details, audits };
  }
  if (item.type === "report_pm" || item.type === "report_final") return project.dailyReportDetail(item.businessId);
  if (item.type === "variation") return project.variationOrderDetail(item.businessId);
  if (item.type === "settlement") return request.get("/team-settlements/" + item.businessId);
  throw new Error("当前待办暂无可用详情");
}

async function approve(item, data) {
  const payload = data || {};
  if (item.type === "salary") return salary.approveMonthly(item.businessId, payload.remark);
  if (item.type === "report_pm" || item.type === "report_final") {
    const stage = item.type === "report_final" ? "final" : "pm";
    return project.confirmDailyReport(item.businessId, stage, payload);
  }
  if (item.type === "variation") {
    if (payload.confirmedAmount === "" || payload.confirmedAmount === undefined || Number(payload.confirmedAmount) < 0) {
      throw new Error("请输入非负确认金额");
    }
    return project.confirmVariationOrder(item.businessId, {
      confirmedAmount: Number(payload.confirmedAmount),
      remark: payload.remark
    });
  }
  if (item.type === "settlement") return project.confirmSettlement(item.businessId, payload);
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
  detail,
  approve,
  reject
};
