# Payroll End-To-End Regression

## Automated Coverage

`SalaryLifecycleIntegrationTest` runs the payroll lifecycle against an empty
Testcontainers PostgreSQL database through the real HTTP API. It creates an
employee and an active salary profile, then verifies these role-separated
steps:

1. An administrator creates the employee, salary profile, and monthly payroll,
   then generates the detail and submits it for approval.
2. A BOSS user approves the submitted payroll.
3. A FINANCE user records the monthly payment and the payroll becomes `paid`.
4. The employee account reads only its own salary history, single detail, and
   salary items, then confirms the payslip.
5. The test verifies the expected amounts, state transitions, and audit actions.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=SalaryLifecycleIntegrationTest test
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
| Create or select an active employee | ADMIN | Employees | Employee can be selected for a salary profile | |
| Save an active salary profile | ADMIN or FINANCE | Salary profiles | Earning and deduction items are visible in the profile | |
| Create and generate the monthly payroll | ADMIN or FINANCE | Monthly salary | One detail shows expected payable, deduction, and actual amounts | |
| Submit and approve | Salary manager, then BOSS | Monthly salary / approval action | Status changes from `draft` to `pending_approval` to `approved` | |
| Record payment | ADMIN or FINANCE | Monthly salary / payment | Detail and monthly payroll become `paid` with a payment method and date | |
| Check audit trail | Salary manager | Monthly salary / audit trail | Create, generate, submit, approve, payment, and payslip actions are traceable | |
| Confirm payslip | Bound EMPLOYEE account | Miniapp My Salary | The employee sees only their own detail and items, then the payslip becomes confirmed | |

Run the Web build before recording a passing smoke result:

```bash
pnpm --dir web build
```

The smoke record must not contain real salary figures, credentials, personal
data, or production screenshots. See
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md) for the
required pull-request evidence rule.
