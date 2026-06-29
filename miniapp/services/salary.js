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

function detailItems(detailId) {
  return request.get("/salary/details/" + detailId + "/items");
}

function approveMonthly(id) {
  return request.post("/salary/monthly/" + id + "/approve");
}

function rejectMonthly(id) {
  return request.post("/salary/monthly/" + id + "/reject");
}

module.exports = {
  myHistory,
  monthlyList,
  monthlyDetails,
  detailItems,
  approveMonthly,
  rejectMonthly
};
