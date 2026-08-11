import { describe, expect, it } from "vitest";
import { cn, employeeStatusText, money, statusText } from "./utils";

describe("display utilities", () => {
  it("merges class names and resolves known status labels", () => {
    expect(cn("p-2", "p-4", "text-sm")).toBe("p-4 text-sm");
    expect(statusText("in_progress")).toBe("施工中");
    expect(employeeStatusText("left")).toBe("离职");
  });

  it("preserves unknown statuses and formats default monetary values", () => {
    expect(statusText("custom_status")).toBe("custom_status");
    expect(statusText()).toBe("-");
    expect(money(1234.5)).toMatch(/1,234\.50/);
    expect(money(null)).toMatch(/0\.00/);
  });
});
