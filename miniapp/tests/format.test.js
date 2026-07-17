const test = require("node:test");
const assert = require("node:assert/strict");

const format = require("../utils/format");

test("statusTagType maps rejected to danger", () => {
  assert.equal(format.statusTagType("rejected"), "danger");
});

test("statusTagType maps pending/draft states to warning", () => {
  for (const status of ["draft", "submitted", "pending_approval", "pm_confirmed"]) {
    assert.equal(format.statusTagType(status), "warning");
  }
});

test("payslipStatusText returns the same label on list and detail pages", () => {
  // Regression: the list page previously said "工资条已确认" while the detail
  // page said "已确认" for the same status. Both now read from format.js.
  assert.equal(format.payslipStatusText("confirmed"), "已确认");
  assert.equal(format.payslipStatusText("exception"), "有异常");
  assert.equal(format.payslipStatusText(undefined), "待确认");
  assert.equal(format.payslipStatusText("anything-else"), "待确认");
});

test("payslipStatusTagType is success for confirmed", () => {
  assert.equal(format.payslipStatusTagType("confirmed"), "success");
  assert.equal(format.payslipStatusTagType("exception"), "danger");
  assert.equal(format.payslipStatusTagType(undefined), "warning");
});

test("MAX_PHOTO_COUNT is a positive integer photo cap", () => {
  assert.equal(typeof format.MAX_PHOTO_COUNT, "number");
  assert.ok(format.MAX_PHOTO_COUNT > 0);
  assert.equal(Number.isInteger(format.MAX_PHOTO_COUNT), true);
});

test("money formats numbers as currency with two decimals", () => {
  assert.equal(format.money(1234.5), "¥1234.50");
  assert.equal(format.money(0), "¥0.00");
  assert.equal(format.money(null), "¥0.00");
});
