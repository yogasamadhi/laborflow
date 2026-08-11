export type MonthlyAction = "generate" | "submit" | "approve" | "reject" | "delete" | "restore";

export type MonthlyWorkflowState = {
  status: string;
  deleted?: number;
  totalEmployeeCount?: number;
};

type MonthlyActionCopy = {
  label: string;
  title: string;
  description: string;
  success: string;
  tone?: "primary" | "danger";
  needsRemark?: boolean;
};

const actionCopy: Record<MonthlyAction, MonthlyActionCopy> = {
  generate: {
    label: "生成",
    title: "确认重新生成工资明细",
    description: "系统会按当前工资档案和已启用的规则重建这张草稿工资表的明细。请先确认人工调整已经保存。",
    success: "工资明细已生成并刷新。"
  },
  submit: {
    label: "提交",
    title: "确认提交工资表审核",
    description: "提交后工资表将进入审核流程，明细和规则将被锁定，直到审核驳回。",
    success: "工资表已提交审核。"
  },
  approve: {
    label: "通过",
    title: "确认审核通过",
    description: "通过后工资表会锁定并开放发放登记。请确认人数、应发、扣减和实发金额。",
    success: "工资表已审核通过，可以登记发放。"
  },
  reject: {
    label: "驳回",
    title: "确认驳回工资表",
    description: "驳回后工资表会回到草稿状态，需重新核对并再次提交审核。请填写驳回原因。",
    success: "工资表已驳回并退回草稿。",
    tone: "danger",
    needsRemark: true
  },
  delete: {
    label: "删除",
    title: "确认移入回收站",
    description: "只有草稿工资表可以删除。删除后不会立即清除数据，可在回收站恢复。",
    success: "工资表已移入回收站。",
    tone: "danger"
  },
  restore: {
    label: "恢复",
    title: "确认恢复工资表",
    description: "恢复后工资表会重新回到正常列表，可继续查看和处理。",
    success: "工资表已恢复。"
  }
};

export function getMonthlyActionCopy(action: MonthlyAction) {
  return actionCopy[action];
}

export function canRunMonthlyAction(monthly: MonthlyWorkflowState, action: MonthlyAction) {
  if (action === "restore") return monthly.deleted === 1;
  if (monthly.deleted === 1) return false;
  if (action === "generate" || action === "delete") return monthly.status === "draft";
  if (action === "submit") return monthly.status === "draft" && Number(monthly.totalEmployeeCount ?? 0) > 0;
  if (action === "approve" || action === "reject") return monthly.status === "pending_approval";
  return false;
}

export function monthlyWorkflowStage(status?: string) {
  if (status === "draft") return 0;
  if (status === "pending_approval") return 1;
  return 2;
}
