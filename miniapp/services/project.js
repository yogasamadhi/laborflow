const request = require("../utils/request");

function projects(params) {
  return request.get("/projects", Object.assign({ page: 1, pageSize: 100 }, params || {}));
}

function sections(projectId) {
  return request.get("/projects/" + projectId + "/sections", { page: 1, pageSize: 100 });
}

function workProcesses() {
  return request.get("/work-processes", { page: 1, pageSize: 100, status: "active" });
}

function teams() {
  return request.get("/teams", { page: 1, pageSize: 100, status: "active" });
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
  return request.post("/daily-work-reports/" + id + "/files", { fileId, fileType: "site_photo" });
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
  return request.post("/variation-orders/" + id + "/files", { fileId, fileType: "site_photo" });
}

function settlements(params) {
  return request.get("/team-settlements", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

function confirmSettlement(id) {
  return request.post("/team-settlements/" + id + "/confirm");
}

module.exports = {
  projects,
  sections,
  workProcesses,
  teams,
  dailyReports,
  myDailyReports,
  createDailyReport,
  updateDailyReport,
  submitDailyReport,
  confirmDailyReport,
  rejectDailyReport,
  attachDailyReportFile,
  variationOrders,
  myVariationOrders,
  createVariationOrder,
  updateVariationOrder,
  submitVariationOrder,
  confirmVariationOrder,
  rejectVariationOrder,
  attachVariationFile,
  settlements,
  confirmSettlement
};
