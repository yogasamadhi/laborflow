const request = require("../utils/request");

function myHistory() {
  return request.get("/statistics/salary/my-history");
}

function monthlyList(params) {
  return request.get("/salary/monthly", Object.assign({ page: 1, pageSize: 20 }, params || {}));
}

function monthlyDetails(monthlyId, params) {
  return request.get("/salary/monthly/" + monthlyId + "/details", Object.assign({ page: 1, pageSize: 100 }, params || {}));
}

function detail(detailId) {
  return request.get("/salary/details/" + detailId);
}

function detailItems(detailId) {
  return request.get("/salary/details/" + detailId + "/items");
}

function confirmPayslip(detailId) {
  return request.post("/salary/details/" + detailId + "/payslip/confirm");
}

function monthlyAudits(monthlyId) {
  return request.get("/salary/monthly/" + monthlyId + "/audits");
}

function approveMonthly(id, remark) {
  return request.post("/salary/monthly/" + id + "/approve", { remark });
}

function rejectMonthly(id, remark) {
  return request.post("/salary/monthly/" + id + "/reject", { remark });
}

module.exports = {
  myHistory,
  monthlyList,
  monthlyDetails,
  detail,
  detailItems,
  confirmPayslip,
  monthlyAudits,
  approveMonthly,
  rejectMonthly
};
