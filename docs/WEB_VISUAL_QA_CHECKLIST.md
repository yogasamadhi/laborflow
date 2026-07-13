# Web Visual QA Checklist

## Baseline Scope

This checklist is the public Web experience baseline for `L2-24-01`. It covers
the shared login flow, application shell, responsive navigation, route loading,
and global request feedback. Payroll and project workspaces keep their business
action screenshots in their own capability loops.

Run these commands before recording visual evidence:

```bash
pnpm --dir web typecheck
pnpm --dir web test
pnpm --dir web build
pnpm --dir web exec vite --host 127.0.0.1 --port 5175
```

Use only local or test accounts. Do not capture production names, phone
numbers, salary amounts, private file URLs, tokens, or browser storage.

## Screenshot Checkpoints

| Checkpoint | Viewport | Route and state | Pass criteria | Evidence |
| --- | --- | --- | --- | --- |
| Login default | 1366 x 768 | `/login` | Brand area and sign-in form share the viewport without clipping; account and password controls, mode switch, and submit action are readable | |
| Login narrow | 390 x 844 | `/login` | Brand area stacks above the form; no horizontal overflow; all controls remain reachable by scroll | |
| Login register | 390 x 844 | `/login`, select `注册` | Nickname, phone, password, confirmation, validation feedback, and submit state remain vertically ordered without overlap | |
| Protected route feedback | 1366 x 768 | A direct route without a valid session | Session feedback leads to `/login` with a safe return path; no empty application shell is shown | |
| Mobile navigation | 390 x 844 | Any signed-in route | Menu icon opens one focusable drawer, overlay closes it, and selecting a navigation entry closes it before route content is shown | |
| Route loading | 1366 x 768 and 390 x 844 | Navigate between distinct route modules | Skeleton state preserves page rhythm while the next module loads; it does not shift the shell or overflow | |
| Request feedback | 1366 x 768 | Trigger a permitted validation/API failure and a forbidden operation | Readable in-context notice appears; close control works and the underlying page remains usable | |

The login default and narrow checkpoints were rendered locally in this loop with
the application browser at the stated viewports. Capture authenticated-shell,
loading, and request-feedback evidence with a non-production account whenever a
shared-shell or API change is reviewed.

## Build Baseline

| Build item | Before `L2-24-01` | Current baseline | Result |
| --- | --- | --- | --- |
| Initial `index` JavaScript | 1,605.36 kB / 505.39 kB gzip | 396.80 kB / 127.43 kB gzip | Route-level lazy loading removes feature pages from the initial route |
| Project feature chunk | Included in initial bundle | 60.25 kB / 9.83 kB gzip | Loaded only for project routes |
| Compensation feature chunk | Included in initial bundle | 19.26 kB / 3.71 kB gzip | Loaded only for compensation routes |
| Salary statistics chart chunk | Included in initial bundle | 1,036.92 kB / 343.96 kB gzip | ECharts remains isolated to the salary-statistics route |

The chart route is intentionally the remaining large lazy chunk. Treat a future
increase in the initial `index` bundle as a public-shell performance regression;
evaluate further chart-library reduction separately from unrelated workspace
changes.
