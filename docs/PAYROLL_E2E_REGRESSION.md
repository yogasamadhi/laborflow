# Payroll End-To-End Regression

## Automated Coverage

`SalaryLifecycleIntegrationTest` runs the payroll lifecycle against an empty
Testcontainers PostgreSQL database through the real HTTP API. It creates an
active department, employee, and salary profile, then verifies these role-separated
steps:

1. An administrator creates the employee, salary profile, and monthly payroll,
   then generates the detail and submits it for approval.
2. A BOSS user approves the submitted payroll.
3. A FINANCE user records the monthly payment and the payroll becomes `paid`.
4. The employee account reads only its own salary history, single detail, and
   salary items, then confirms the payslip.
5. The test verifies the expected amounts, state transitions, and audit actions.

`DepartmentalPayrollIntegrationTest` covers the department accounting boundary:

1. Salary profiles can be filtered by employee keyword and direct department,
   with employee, department, amount, and item-count summary fields returned.
2. Two direct departments create and generate payroll sheets for the same month;
   a parent department does not absorb employees from a child department.
3. An employee without a department cannot receive an active salary profile,
   and one department cannot create a duplicate payroll sheet for the same month.
4. After an employee transfer, the existing detail keeps its department snapshot.
   The new department is rejected until the old draft is regenerated to remove
   the employee.
5. Both department sheets complete submit, approval, and payment independently.
   Company overview, monthly trend, department summary, and dashboard totals
   equal the sum of the department sheets.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=SalaryLifecycleIntegrationTest test
```

Run the focused department regression with:

```bash
mvn -B -ntp -f server/pom.xml \
  -Dtest=DepartmentalPayrollIntegrationTest test
```

Run the full backend suite before merging a change that affects payroll:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Web Smoke Record

Use local or test fixtures only. The Web check complements the automated API
regression and is recorded in the pull request using
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Web route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Create or select active employees | ADMIN | Employees | Employees have active direct departments; the no-department fixture is rejected when enabling a profile | |
| Save active salary profiles | ADMIN or FINANCE | Salary profiles | Profiles are grouped/filterable by direct department with counts and amount subtotals | |
| Create and generate department payrolls | ADMIN or FINANCE | Monthly salary | Two departments can create the same month independently; each sheet contains only its direct-department employees | |
| Transfer employee in month | ADMIN or FINANCE | Employees / monthly salary | New-department generation rejects a duplicate until the old draft is regenerated; the old snapshot does not change silently | |
| Submit and approve | Salary manager, then BOSS | Monthly salary / approval action | Status changes from `draft` to `pending_approval` to `approved` | |
| Record payment | ADMIN or FINANCE | Monthly salary / payment | Detail and monthly payroll become `paid` with a payment method and date | |
| Check audit trail | Salary manager | Monthly salary / audit trail | Create, generate, submit, approve, payment, and payslip actions are traceable | |
| Confirm payslip | Bound EMPLOYEE account | Miniapp My Salary | The employee sees only their own detail and items, then the payslip becomes confirmed | |

Run the Web build before recording a passing smoke result:

```bash
cd clients/desktop
pnpm run build:renderer
```

The smoke record must not contain real salary figures, credentials, personal
data, or production screenshots. See
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md) for the
required pull-request evidence rule.
