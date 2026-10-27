# Salary Sources End-To-End Regression

## Automated Coverage

`SalarySourcesLifecycleIntegrationTest` uses an empty Testcontainers PostgreSQL
database and the real HTTP API to verify that four operational compensation
sources reach one employee's monthly salary and remain traceable:

1. Create an active department, employee, project, and salary profile. The department payroll disables
   fixed salary so the expected amounts come only from variable sources.
2. Create an attendance record with an automatically calculated leave deduction
   and a separate active social-security deduction record.
3. Create and confirm a performance score, then create, release, allocate, and
   confirm a project bonus allocation.
4. Generate the department monthly salary and verify salary-detail amounts, four detail
   items, source IDs and signed source amounts, and the compensation dashboard.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=SalarySourcesLifecycleIntegrationTest test
```

Run the full backend suite before merging a change that affects compensation
sources, payroll generation, salary detail items, or source snapshots:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml test
```

## Web Smoke Record

Use local or test fixtures only. Record the result in the pull request using
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Record attendance | ADMIN, MANAGER, or FINANCE | Compensation / attendance | Leave or absence produces the expected attendance deduction | |
| Record statutory deduction | ADMIN, MANAGER, or FINANCE | Compensation / deductions | The selected social-security, housing-fund, tax, advance, or other deduction is active for the payroll month | |
| Confirm performance | ADMIN, MANAGER, or FINANCE | Compensation / performance scores | A confirmed score has the expected performance amount | |
| Release and confirm bonus | ADMIN, MANAGER, or FINANCE | Compensation / bonus pools and allocations | The project bonus allocation is confirmed for the payroll month | |
| Generate monthly salary | Salary manager | Monthly salary | Variable-source rules create the expected payable, deduction, and actual amounts | |
| Check salary sources | Salary manager | Monthly salary / source snapshot | Attendance, statutory deduction, performance, and project-bonus source IDs and amounts are traceable | |

Run the Web build before recording a passing smoke result:

```bash
cd clients/desktop
pnpm run build:renderer
```

Do not attach production employee, project, bonus, payroll, or payment data.
The governing evidence and exception rules are in
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
