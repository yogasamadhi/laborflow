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
    active: "在职",
    inactive: "停用",
    left: "离职",
    draft: "草稿",
    pending_approval: "待审核",
    approved: "已审核",
    paid: "已发放",
    archived: "已归档",
    cancelled: "已作废",
    unpaid: "未发放"
  };
  return status ? map[status] ?? status : "-";
}

