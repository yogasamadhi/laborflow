import { describe, expect, it } from "vitest";
import { canAccessRoute } from "./access";

describe("route access rules", () => {
  it("allows each role only on its configured management routes", () => {
    expect(canAccessRoute("/dashboard", ["EMPLOYEE"])).toBe(true);
    expect(canAccessRoute("/employees/42", ["EMPLOYEE"])).toBe(false);
    expect(canAccessRoute("/salary/monthly/42/payment", ["FINANCE"])).toBe(true);
    expect(canAccessRoute("/projects/team-settlements", ["FINANCE"])).toBe(true);
    expect(canAccessRoute("/projects/daily-reports", ["FINANCE"])).toBe(false);
    expect(canAccessRoute("/system/users", ["MANAGER"])).toBe(false);
    expect(canAccessRoute("/system/users", ["ADMIN"])).toBe(true);
  });

  it("leaves unknown paths to the router's not-found route", () => {
    expect(canAccessRoute("/missing-route", ["EMPLOYEE"])).toBe(true);
  });
});
