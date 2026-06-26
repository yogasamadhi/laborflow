import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function money(value?: number | string | null) {
  const n = Number(value ?? 0);
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(n);
}

export function statusText(status?: string) {
  const map: Record<string, string> = {
    active: "启用",
    inactive: "停用",
    draft: "草稿",
    pending_approval: "待审核",
    approved: "已审核",
    paid: "已发放",
    archived: "已归档",
    cancelled: "已作废",
    unpaid: "未发放",
    not_started: "未开工",
    in_progress: "施工中",
    paused: "暂停",
    completed: "已完工",
    settled: "已结算",
    submitted: "已提交",
    pm_confirmed: "项目经理已确认",
    confirmed: "已确认",
    rejected: "已驳回",
    released: "已发布",
    allocated: "已分配"
  };
  return status ? map[status] ?? status : "-";
}

export function employeeStatusText(status?: string) {
  const map: Record<string, string> = {
    active: "在职",
    inactive: "停用",
    left: "离职"
  };
  return status ? map[status] ?? statusText(status) : "-";
}
