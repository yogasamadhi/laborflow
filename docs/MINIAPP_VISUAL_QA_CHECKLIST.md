# Miniapp Field Workspace Visual QA Checklist

## Scope

Use this checklist for the field-facing miniapp workspace: daily reports,
variation orders, photo uploads and local drafts, approval to-dos, salary
history, and payslip confirmation. Run against a local or test backend only;
do not record production identities, openids, salary values, or project photos.

Run the automated baseline before the manual pass:

```bash
cd clients/miniapp
pnpm run check
cd ../../
mvn -B -ntp -f server/pom.xml test
```

The miniapp check covers TypeScript and the WeChat production build. The backend suite covers the
existing API, attachment, access-control, and salary/project lifecycle
contracts; it is not a substitute for WeChat rendering or device permissions.

## Capture Setup

| Field | Record |
| --- | --- |
| Commit / pull request | |
| Tester and date | |
| WeChat DevTools version | |
| Simulator or device | Narrow phone and regular phone widths |
| Backend environment | Local or test only |
| Test role / fixture label | Field employee, manager, salary employee |
| Network setting | Normal and weak-network simulation |

Capture a screenshot for each listed state. Record a short screen recording
for a form save/submit, a partial-upload recovery, one approval action, and a
payslip confirmation. Use the evidence fields in
[`MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md).

## Field Forms

| Flow | States to capture | Expected result |
| --- | --- | --- |
| Daily-report list | Initial loading, empty, error/retry, draft, rejected | Text remains readable; retry is visible; rejected reason and edit action are easy to find. |
| Daily-report form | Empty, restored local draft, picker disabled while lookups load, validation error, local save | Form restores selected values and photos; no control overflows at narrow width. |
| Daily-report upload | One photo, multiple photos, upload in progress, partial failure, attached photo | Progress text is specific; successful files remain after a later file fails; an attached file cannot be presented as removable. |
| Daily-report save and submit | Save in progress, submit in progress, server failure, success | Save and submit are mutually disabled; failed work remains recoverable as a local draft; success returns once without duplicate requests. |
| Variation list and form | Same states as daily report, including rejected resubmission | The same recovery and locking behavior is present for variation orders. |

## Approval And Salary

| Flow | States to capture | Expected result |
| --- | --- | --- |
| To-do permission | Non-manager empty permission state; manager loading, empty, error/retry | The permission explanation is clear and no action controls leak to non-managers. |
| To-do approval | Inline approve panel, optional remark, processing, success, service failure | The selected item remains visible during confirmation; buttons lock during request and the result refreshes the list. |
| To-do rejection | Inline reject panel with blank and filled reason | Blank rejection is blocked with clear feedback; submitted reason is associated with the selected item only. |
| Salary history | Loading, no employee binding, empty history, error/retry, paid and unpaid record | Amount, payment state, and payslip state are independently legible. |
| Payslip detail | Pending, confirmed, exception, confirmation modal, confirmation request failure | Confirm action is single-flight; confirmed entries do not offer repeat confirmation; exception entries direct the user to administrator follow-up. |

## Weak Network And Touch Checks

1. Enable weak-network simulation before opening a report and variation form.
2. Enter several fields, temporarily leave the page, reopen it, and verify the
   local draft restores with values and unbound uploaded photos.
3. Upload at least two test images and induce a failure after one completes.
   Verify the completed image remains listed and the remaining image can be
   selected again for a later retry.
4. Trigger a save or submit delay, tap every action button repeatedly, and
   verify only one request is sent and buttons remain disabled until it ends.
5. Repeat an approval and a payslip confirmation under delay; verify the
   selected record, confirmation copy, and processing state remain coherent.
6. Repeat the above at narrow and regular phone widths. Text, tags, photo
   chips, and action buttons must wrap or stack without clipping or overlap.

## Completion Record

| Item | Result / evidence reference |
| --- | --- |
| Automated miniapp check | |
| Backend regression | |
| Daily report screenshots and recording | |
| Variation screenshots and recording | |
| Upload partial-failure evidence | |
| Approval screenshots and recording | |
| Salary/payslip screenshots and recording | |
| Weak-network evidence | |
| Defects, waivers, and retest result | |

This checklist defines the required manual evidence. It does not assert that a
DevTools preview, real device test, upload, approval, salary confirmation, or
weak-network run has already occurred.
