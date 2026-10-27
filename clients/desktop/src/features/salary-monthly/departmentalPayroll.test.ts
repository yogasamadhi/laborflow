import { describe, expect, it } from "vitest";
import { groupMonthliesByMonth, groupProfilesByDepartment, monthlyCreatePayload, summarizeProfiles } from "./departmentalPayroll";

describe("departmental payroll presentation", () => {
  it("groups salary profiles by direct department and calculates subtotals", () => {
    const groups = groupProfilesByDepartment([
      { departmentId: "10", departmentName: "财务部", status: "active", earningAmount: 1200, deductionAmount: 100 },
      { departmentId: "10", departmentName: "财务部", status: "inactive", earningAmount: 800, deductionAmount: 50 },
      { departmentId: "20", departmentName: "项目部", status: "active", earningAmount: 1500, deductionAmount: 200 }
    ]);

    expect(groups.map((group) => group.departmentId)).toEqual(["10", "20"]);
    expect(summarizeProfiles(groups[0].records)).toEqual({
      profileCount: 2,
      activeCount: 1,
      earningAmount: 2000,
      deductionAmount: 150,
      netAmount: 1850
    });
  });

  it("groups department payroll sheets under one month and keeps department in the create payload", () => {
    const groups = groupMonthliesByMonth([
      { salaryMonth: "2033-01", departmentId: "10", totalEmployeeCount: 2, totalActualAmount: 1800 },
      { salaryMonth: "2033-01", departmentId: "20", totalEmployeeCount: 3, totalActualAmount: 2700 },
      { salaryMonth: "2032-12", departmentId: "10", totalEmployeeCount: 1, totalActualAmount: 900 }
    ]);

    expect(groups).toHaveLength(2);
    expect(groups[0]).toMatchObject({ salaryMonth: "2033-01", employeeCount: 5, actualAmount: 4500 });
    expect(monthlyCreatePayload({ salaryMonth: "2033-01", departmentId: "20", title: "", includeFixedSalary: 1 }, "项目部"))
      .toMatchObject({ salaryMonth: "2033-01", departmentId: "20", title: "2033-01 项目部工资表" });
  });
});
