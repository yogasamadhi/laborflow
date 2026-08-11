const statusMap = {
  draft: "草稿",
  pending_approval: "待审核",
  approved: "已审核",
  paid: "已发放",
  unpaid: "未发放",
  submitted: "待确认",
  pm_confirmed: "项目经理已确认",
  confirmed: "已确认",
  rejected: "已驳回",
  not_started: "未开工",
  in_progress: "施工中",
  paused: "暂停",
  completed: "已完工"
};

function money(value) {
  const num = Number(value || 0);
  return "¥" + num.toFixed(2);
}

function status(value) {
  return statusMap[value] || value || "-";
}

function today() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return now.getFullYear() + "-" + month + "-" + day;
}

function pageRecords(pageResult) {
  if (Array.isArray(pageResult)) return pageResult;
  return pageResult && pageResult.records ? pageResult.records : [];
}

function statusTagType(value) {
  if (value === "rejected") return "danger";
  if (value === "draft" || value === "submitted" || value === "pending_approval" || value === "pm_confirmed") return "warning";
  return "primary";
}

// Maximum number of photos a field form (report/variation) accepts.
// Used by both create pages and the file service; keep it in one place so a
// change does not require hunting three call sites.
const MAX_PHOTO_COUNT = 6;

// Payslip confirmation state shared by the salary list and detail pages.
// Previously each page declared its own copy — and the copies disagreed:
// the list page said "工资条已确认" while the detail page said "已确认" for
// the same status. This is the single source of truth now.
function payslipStatusText(status) {
  if (status === "confirmed") return "已确认";
  if (status === "exception") return "有异常";
  return "待确认";
}

function payslipStatusTagType(status) {
  if (status === "confirmed") return "success";
  if (status === "exception") return "danger";
  return "warning";
}

module.exports = {
  money,
  status,
  today,
  pageRecords,
  statusTagType,
  payslipStatusText,
  payslipStatusTagType,
  MAX_PHOTO_COUNT
};
