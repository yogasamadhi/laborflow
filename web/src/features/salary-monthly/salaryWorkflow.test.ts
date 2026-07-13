import { describe, expect, it } from "vitest";
import { canRunMonthlyAction, getMonthlyActionCopy, monthlyWorkflowStage } from "./salaryWorkflow";

describe("monthly salary workflow", () => {
  it("only exposes actions valid for the current payroll state", () => {
    expect(canRunMonthlyAction({ status: "draft", totalEmployeeCount: 3 }, "generate")).toBe(true);
    expect(canRunMonthlyAction({ status: "draft", totalEmployeeCount: 0 }, "submit")).toBe(false);
    expect(canRunMonthlyAction({ status: "pending_approval" }, "approve")).toBe(true);
    expect(canRunMonthlyAction({ status: "approved" }, "approve")).toBe(false);
    expect(canRunMonthlyAction({ status: "draft", deleted: 1 }, "restore")).toBe(true);
    expect(canRunMonthlyAction({ status: "draft", deleted: 1 }, "generate")).toBe(false);
  });

  it("provides the right workflow stage and safeguard copy", () => {
    expect(monthlyWorkflowStage("draft")).toBe(0);
    expect(monthlyWorkflowStage("pending_approval")).toBe(1);
    expect(monthlyWorkflowStage("approved")).toBe(2);
    expect(getMonthlyActionCopy("reject").needsRemark).toBe(true);
    expect(getMonthlyActionCopy("approve").success).toContain("审核通过");
  });
});
