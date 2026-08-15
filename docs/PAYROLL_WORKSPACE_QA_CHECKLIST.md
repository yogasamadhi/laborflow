# Payroll Workspace QA Checklist

## Scope

This checklist covers the Web payroll workspace: monthly payroll creation and state
transitions, detail review, payment
registration, exports, payslips, and salary anomaly handling. It does not
change payroll calculation rules, approval permissions, payment APIs, or export
formats.

Use a non-production account and test payroll records only. Do not capture real
employee names, bank accounts, phone numbers, salary amounts, private proof
URLs, tokens, or browser storage in evidence.

Run the automated checks before the manual path:

```bash
pnpm --dir clients/desktop typecheck
pnpm --dir clients/desktop test
pnpm --dir clients/desktop build:renderer
pnpm --dir clients/desktop dev
```

## Payroll Workflow

| Checkpoint | Role and starting state | Action | Pass criteria | Evidence |
| --- | --- | --- | --- | --- |
| Create and generate | Salary manager; no current draft | Create a month with enabled rules, then open its detail | Creation result is clear; draft shows the selected rules and employee/amount totals after generation | |
| Re-generate confirmation | Salary manager; draft with generated details | Select `重新生成`, inspect confirmation, then cancel and confirm with a test record | Cancel makes no request; confirm explains the replacement impact and refreshes counts and totals | |
| Submit confirmation | Salary manager; draft with at least one detail | Select `提交审核` from list or detail | Confirmation identifies the payroll month, explains locking, and success changes status to pending approval | |
| Approve and reject | Administrator or boss; pending approval | Approve one test month and reject another with a reason | Approval opens payment registration; rejection requires a reason, returns to draft, and appears in the audit trail | |
| Detail review | Salary manager; generated month | Filter by employee and payment status; open composition and edit an unpaid draft detail | Empty filter is explicit; item/source snapshot is readable; saved adjustment and rule save show a result message | |
| Batch payment | Finance or salary manager; approved month with unpaid details | Select a subset, attach optional test proof, select `发放选中`, and confirm | Confirmation shows selected count, exact amount, date, method, and proof state; success refreshes row status and payment audit | |
| All-unpaid payment | Finance or salary manager; approved month with unpaid details | Do not select rows, choose `发放全部未发`, then cancel and confirm | Cancel does not change rows; confirm applies only to unpaid details and clears the selection after success | |
| Locked payment | Any permitted user; draft, pending, or paid month | Open payment registration | Form and payment actions are unavailable outside the approved state; explanatory feedback remains readable | |

## Exports And Exceptions

| Checkpoint | Route and state | Action | Pass criteria | Evidence |
| --- | --- | --- | --- | --- |
| Payroll exports | `/salary/monthly/:id/export` | Request each of the four Excel files | Context shows month and status; the selected export reports success or an actionable error; no duplicate request while preparing | |
| Payslip scope | `/salary/monthly/:id/payslip` with generated details | Export all, then one test employee | The employee selector makes the scope explicit and reports the result; empty payroll shows no misleading export action | |
| Salary anomaly | `/statistics/salary/anomalies` with one test anomaly | Mark handled, ignored, and adjustment with a note | Each action opens a confirmation with the anomaly context; result and note remain visible after refresh | |

## Visual And Failure States

| Checkpoint | Viewport | Pass criteria | Evidence |
| --- | --- | --- | --- |
| Payroll list and detail | 1366 x 768 | Workflow stages, totals, actions, audit trail, and wide tables remain readable without overlap | |
| Payment and export | 390 x 844 | Controls stack in a usable order; confirmation modal remains scrollable; no horizontal page overflow outside table scroll areas | |
| Empty and filter states | 1366 x 768 and 390 x 844 | No-details, no-filter-match, empty exception list, and empty payslip states explain the next safe action | |
| Request failure | 1366 x 768 | Simulated or test-environment API failure keeps the page usable, provides retry or action-level error feedback, and does not claim a completed payment/export | |

Record screenshots only after the expected result is visible. Authenticated visual evidence
is a non-production UAT checkpoint because it requires a permitted test account.
