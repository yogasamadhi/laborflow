# Project Settlement End-To-End Regression

## Automated Coverage

`ProjectSettlementLifecycleIntegrationTest` uses an empty Testcontainers
PostgreSQL database and the real HTTP API to verify the operational project
flow:

1. Create a project, section, work process, team, team unit price, and active
   contract item.
2. Create a daily work report linked to the contract item and verify the output
   value uses the contract-item unit price.
3. Submit the report, complete project-manager and final quantity confirmation,
   and verify all confirmation stages are recorded.
4. Generate the team settlement from confirmed quantity, then confirm and mark
   it paid.
5. Verify the project dashboard reflects confirmed quantity, output value, team
   payable amount, and team paid amount.

Run the focused regression from the repository root:

```bash
JAVA_HOME=/Users/h/code/env/jdk-21/Contents/Home \
  /Users/h/code/env/maven-3.9/bin/mvn -B -ntp -f server/pom.xml \
  -Dtest=ProjectSettlementLifecycleIntegrationTest test
```

## Web And Miniapp Smoke Record

Use local or test fixtures only. Record the result in the pull request using
[`REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`](REGRESSION_EVIDENCE_RECORD_TEMPLATE.md).

| Step | Role | Route or action | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| Configure project and contract item | ADMIN or MANAGER | Project / contract items | Section, work process, unit, quantity, and unit price are consistent | |
| Configure team unit price | ADMIN or MANAGER | Teams / team unit price | The selected project, team, process, and unit have an active price | |
| Create and submit daily report | Project member | Miniapp daily report or Web daily reports | The report is linked to the project, section, team, and contract item, with calculated output value | |
| Confirm quantity | MANAGER | Daily reports | Status changes from `draft` to `submitted` to `pm_confirmed` to `confirmed` and confirmation history is visible | |
| Generate and confirm settlement | ADMIN, MANAGER, or FINANCE | Team settlements | Confirmed report quantity creates one draft settlement with expected amount | |
| Record settlement payment | ADMIN, MANAGER, or FINANCE | Team settlements | Confirmed settlement becomes `paid` and retains the payment remark | |
| Check project dashboard | Project finance role | Project dashboard | Confirmed quantity, output value, team payable, and team paid amounts match the completed flow | |

Run these local quality checks before recording a passing smoke result:

```bash
pnpm --dir web build
npm --prefix miniapp run check
```

Do not attach production project data, location details, phone numbers, or
unredacted payment evidence. The governing evidence and exception rules are in
[`REGRESSION_EVIDENCE_POLICY.md`](REGRESSION_EVIDENCE_POLICY.md).
