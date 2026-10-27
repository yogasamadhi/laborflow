# Project Workspace QA Checklist

## Scope

This checklist covers the Web project workspace actions that change operational or
financial state: daily report submission and confirmation, variation amount
confirmation, team-settlement payment registration, list empty states, and
mobile table behavior. Project pricing, settlement calculation, permissions,
and server-side state transitions remain unchanged.

Use only a non-production account and project fixture data. Do not include real
project names, contracts, employee information, amounts, attachments, private
file URLs, tokens, or browser storage in screenshots.

```bash
cd clients/desktop
pnpm run typecheck
pnpm run test
pnpm run build:renderer
pnpm run dev
```

## Action Checks

| Checkpoint | Starting state | Pass criteria | Evidence |
| --- | --- | --- | --- |
| Daily report submit | Draft report with pricing and optional evidence | Confirmation shows date, project, quantity, and output value; cancel makes no request; success moves it to confirmation | |
| Work confirmation | Submitted report, then project-manager-confirmed report | Only the valid next action is visible; reject requires a reason; evidence chain shows the resulting status | |
| Variation confirmation | Submitted variation with test estimated amount | Confirmation form shows project and estimate, requires a non-negative confirmed amount, and success updates the confirmed amount | |
| Team settlement payment | Confirmed, unpaid settlement | Confirmation shows team, month, and actual payable; cancel does not change payment state; success marks payment and updates the dashboard source | |
| Empty lists | No fixture records for daily reports, variations, or settlements | Each table explains the next safe action instead of rendering an empty grid | |

## Visual Checks

| Checkpoint | Viewport | Pass criteria | Evidence |
| --- | --- | --- | --- |
| Project list workspace | 1366 x 768 | Wide operational tables remain scannable; action buttons do not overlap; confirmation context is complete | |
| Project mobile workspace | 390 x 844 | Page controls stack cleanly; tables use their own horizontal scroll area; confirmation forms remain scrollable and actionable | |
| Failure feedback | 1366 x 768 | A test-environment action failure leaves the table usable and reports a clear error without claiming success | |

Authenticated action screenshots are a non-production UAT checkpoint. Record the
actual state, result, and redacted evidence only after the expected response is visible.
