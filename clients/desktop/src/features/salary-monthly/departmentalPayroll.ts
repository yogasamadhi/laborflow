export type DepartmentProfileRow = {
  departmentId?: string;
  departmentName: string;
  status: string;
  earningAmount: number;
  deductionAmount: number;
};

export type DepartmentMonthlyRow = {
  salaryMonth: string;
  totalEmployeeCount: number;
  totalActualAmount: number;
};

export function groupProfilesByDepartment<T extends DepartmentProfileRow>(rows: T[]) {
  const groups = new Map<string, T[]>();
  for (const row of rows) {
    const key = row.departmentId ?? "unassigned";
    groups.set(key, [...(groups.get(key) ?? []), row]);
  }
  return [...groups.entries()].map(([departmentId, records]) => ({
    departmentId,
    departmentName: records[0]?.departmentName ?? "未分配部门",
    records
  }));
}

export function summarizeProfiles(rows: DepartmentProfileRow[]) {
  const earningAmount = rows.reduce((sum, row) => sum + Number(row.earningAmount ?? 0), 0);
  const deductionAmount = rows.reduce((sum, row) => sum + Number(row.deductionAmount ?? 0), 0);
  return {
    profileCount: rows.length,
    activeCount: rows.filter((row) => row.status === "active").length,
    earningAmount,
    deductionAmount,
    netAmount: earningAmount - deductionAmount
  };
}

export function groupMonthliesByMonth<T extends DepartmentMonthlyRow>(rows: T[]) {
  const groups = new Map<string, T[]>();
  for (const row of rows) {
    groups.set(row.salaryMonth, [...(groups.get(row.salaryMonth) ?? []), row]);
  }
  return [...groups.entries()].map(([salaryMonth, records]) => ({
    salaryMonth,
    records,
    employeeCount: records.reduce((sum, row) => sum + Number(row.totalEmployeeCount ?? 0), 0),
    actualAmount: records.reduce((sum, row) => sum + Number(row.totalActualAmount ?? 0), 0)
  }));
}

export function defaultDepartmentalPayrollTitle(salaryMonth: string, departmentName: string) {
  return `${salaryMonth} ${departmentName}工资表`;
}

export function monthlyCreatePayload<T extends { salaryMonth: string; departmentId: string; title: string }>(form: T, departmentName: string) {
  return {
    ...form,
    title: form.title.trim() || defaultDepartmentalPayrollTitle(form.salaryMonth, departmentName)
  };
}
