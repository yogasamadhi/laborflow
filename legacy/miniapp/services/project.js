const request = require("../utils/request");

function projects(params) {
  return request.getAllPages("/projects", params || {});
}

function sections(projectId) {
  return request.getAllPages("/projects/" + projectId + "/sections");
}

function workProcesses() {
  return request.getAllPages("/work-processes", { status: "active" });
}

function teams() {
  return request.getAllPages("/teams", { status: "active" });
}

function dailyReports(params) {
  return request.get("/daily-work-reports", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

function myDailyReports(params) {
  return dailyReports(Object.assign({ mine: true }, params || {}));
}

function createDailyReport(data) {
  return request.post("/daily-work-reports", data);
}

function dailyReportDetail(id) {
  return request.get("/daily-work-reports/" + id);
}

function contractItems(params) {
  return request.getAllPages("/project-contract-items", Object.assign({ status: "active" }, params || {}));
}

function previewDailyReportPricing(data) {
  return request.post("/daily-work-reports/pricing-preview", data);
}

function updateDailyReport(id, data) {
  return request.put("/daily-work-reports/" + id, data);
}

function submitDailyReport(id) {
  return request.post("/daily-work-reports/" + id + "/submit");
}

function confirmDailyReport(id, stage, data) {
  const action = stage === "final" ? "final-confirm" : "pm-confirm";
  return request.post("/daily-work-reports/" + id + "/" + action, data || {});
}

function rejectDailyReport(id, data) {
  return request.post("/daily-work-reports/" + id + "/reject", data || {});
}

function attachDailyReportFile(id, fileId) {
  return request.post("/daily-work-reports/" + id + "/files", { fileId, fileType: "photo" });
}

function dailyReportConfirms(id) {
  return request.get("/daily-work-reports/" + id + "/confirms");
}

function dailyReportFiles(id) {
  return request.get("/daily-work-reports/" + id + "/files");
}

function removeDailyReportFile(id, fileId) {
  return request.del("/daily-work-reports/" + id + "/files/" + fileId);
}

function variationOrders(params) {
  return request.get("/variation-orders", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

function myVariationOrders(params) {
  return variationOrders(Object.assign({ mine: true }, params || {}));
}

function createVariationOrder(data) {
  return request.post("/variation-orders", data);
}

function variationOrderDetail(id) {
  return request.get("/variation-orders/" + id);
}

function updateVariationOrder(id, data) {
  return request.put("/variation-orders/" + id, data);
}

function submitVariationOrder(id) {
  return request.post("/variation-orders/" + id + "/submit");
}

function confirmVariationOrder(id, data) {
  return request.post("/variation-orders/" + id + "/confirm", data || {});
}

function rejectVariationOrder(id, data) {
  return request.post("/variation-orders/" + id + "/reject", data || {});
}

function attachVariationFile(id, fileId) {
  return request.post("/variation-orders/" + id + "/files", { fileId, fileType: "photo" });
}

function variationConfirms(id) {
  return request.get("/variation-orders/" + id + "/confirms");
}

function variationFiles(id) {
  return request.get("/variation-orders/" + id + "/files");
}

function removeVariationFile(id, fileId) {
  return request.del("/variation-orders/" + id + "/files/" + fileId);
}

function settlements(params) {
  return request.get("/team-settlements", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

function confirmSettlement(id, data) {
  return request.post("/team-settlements/" + id + "/confirm", data || {});
}

function rejectSettlement(id, data) {
  return request.post("/team-settlements/" + id + "/reject", data || {});
}

function markSettlementPaid(id, data) {
  return request.post("/team-settlements/" + id + "/mark-paid", data || {});
}

module.exports = {
  projects,
  sections,
  workProcesses,
  teams,
  dailyReports,
  myDailyReports,
  createDailyReport,
  dailyReportDetail,
  contractItems,
  previewDailyReportPricing,
  updateDailyReport,
  submitDailyReport,
  confirmDailyReport,
  rejectDailyReport,
  attachDailyReportFile,
  dailyReportConfirms,
  dailyReportFiles,
  removeDailyReportFile,
  variationOrders,
  myVariationOrders,
  createVariationOrder,
  variationOrderDetail,
  updateVariationOrder,
  submitVariationOrder,
  confirmVariationOrder,
  rejectVariationOrder,
  attachVariationFile,
  variationConfirms,
  variationFiles,
  removeVariationFile,
  settlements,
  confirmSettlement,
  rejectSettlement,
  markSettlementPaid
};
