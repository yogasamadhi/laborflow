# AGENTS.md

Context for AI agents working in LaborFlow. Read alongside `README.md` (Chinese,
product/architecture overview) and `docs/INDEX.md` (full doc navigation).

## Layout

Two active clients and one legacy client share one Spring Boot API + JWT; no client has its own backend.

- `server/` — Spring Boot 4, Java 21, MyBatis-Plus, Flyway, PostgreSQL 18. Package root
  `com.company.labor` (note: `labor`, **not** `laborflow`). Entry
  `LaborApplication.java`. Business code under `module/{system,employee,salary,file,project,compensation}`,
  plus `security/`, `common/`, `config/`.
- `clients/desktop/` — Electron 43 + React 19 + Vite + TanStack Query + TypeScript desktop admin console.
- `clients/miniapp/` — Taro 4 + React 19 WeChat miniprogram. The watch build writes to `dist/`.
- `legacy/miniapp/` — **legacy** (pending rework) WeChat native miniprogram (plain CommonJS
  JS, Vant Weapp). No build step; runs in WeChat DevTools.
- `deploy/docker-laborflow/` — local Postgres compose only (not a full deploy stack).
- `docs/` — product (Chinese) + E2E regression specs (English, technical contracts).
  See `docs/INDEX.md` before assuming what a doc covers.
- Quality gates are local commands documented in `docs/CI_*_GATE.md`; no hosted CI workflow is committed.

## Commands

Full local stack (Postgres + server + desktop + miniapp watch):

```bash
bun run.ts            # starts docker, server, Electron desktop, and the Taro miniapp watch build
bun run.ts --no-miniapp                  # skip the miniapp watch build
bun run.ts --no-docker --skip-install    # faster restart when infra already up
bun run.ts --stop
```

The miniapp watch build writes to `clients/miniapp/dist/`; import that directory into
WeChat DevTools once and rely on hot reload. It has no TCP port, so `run.ts` starts it
alongside desktop without waiting. Use `--no-miniapp` to skip it entirely.

`run.ts` generates `.env` with random `POSTGRES_PASSWORD`/`JWT_SECRET` on first run —
never uses the committed placeholder values. Do not hand-edit those two back to the
`.env.example` values.

Backend (run from repo root to mirror the local quality gate, or `cd server`):

```bash
mvn -B -ntp -f server/pom.xml test            # local gate; needs Docker for Testcontainers
mvn -B -ntp -f server/pom.xml jacoco:report
# single test:
mvn -B -ntp -f server/pom.xml test -Dtest=PostgresIntegrationTest
mvn -B -ntp -f server/pom.xml test -Dtest=PostgresIntegrationTest#seedsExpectedRolesSalaryItemsAndWorkProcessesOnAnEmptyDatabase
```

Integration tests use Testcontainers (`postgres:18-alpine`) and need a reachable Docker
daemon. They are not skippable; the gate must run them with Docker available.

Desktop (Bun; run from the client directory):

```bash
cd clients/desktop
bun install --frozen-lockfile            # never commit lockfile drift
bun run lint                             # ESLint --max-warnings 0
bun run typecheck                        # tsc --noEmit, strict
bun run test                             # vitest run (no watch)
bun run test src/path/to/file            # single test file
bun run build:renderer                   # tsc -b && vite build under Bun
bun run build                            # Electron Forge installer; Electron uses its embedded Node
```

The local gate order is `install → lint → typecheck → test → build:renderer`; any failure blocks release.

Active miniapp (Bun only; run from the client directory):

```bash
cd clients/miniapp
bun install --frozen-lockfile
bun run check
bun run build:h5
```

Legacy miniapp (pending rework; no PR CI gate):

```bash
npm --prefix legacy/miniapp run check    # node --check all .js + eslint --max-warnings 0 + node --test
npm --prefix legacy/miniapp run test     # node --test tests/*.test.js
node --test legacy/miniapp/tests/specific.test.js
```

Docs/diff gate (run before pushing):

```bash
git diff --check
bun scripts/ci/check-docs.mjs
```

## Integration branch

The remote default and real integration branch is **`main`**; no `master` branch exists.
For branch-range verification use `git diff --check origin/main...HEAD`.

## Non-obvious build-breaking guards

These fail the local gate or build silently if ignored:

- **`.env.example` ↔ `application.yml` sync** (`scripts/ci/check-docs.mjs`): every
  `${NEW_VAR}` referenced in `application.yml` must also appear as `NEW_VAR=` in
  `.env.example`, or the gate fails. Add the var in both places in the same change.
- **Hardcoded latest-migration assertion**: `PostgresIntegrationTest` asserts the Flyway
  version is `"24"`. When you add migration `V25`+, update that string and check
  `FlywayLegacyUpgradeIntegrationTest` (V12 upgrade fixture) for compatibility.
- **Migration filename rule** (also enforced by `check-docs.mjs`): `V<number>__lowercase_snake.sql`,
  no duplicate versions, lowercase only.
- **Explicit `@MapperScan`** in `LaborApplication.java`: a new module's mapper package
  must be added to the `@MapperScan({...})` list or MyBatis-Plus won't wire it.
- **Controller boundary test** (`boundary/RequestDtoBoundaryTest`): controller write
  bodies must be `@Valid @RequestBody SomeDto` — never a `DO` persistence object, and
  never missing `@Valid`. Walking the source tree, so it applies to every controller.
- **Soft delete**: MyBatis-Plus logic-delete field is `deleted` (1=deleted, 0=active). Do
  not issue hard `DELETE`s on these tables.

## Conventions

- **Timezone**: business TZ is pinned to `Asia/Shanghai` via `LABORFLOW_TIMEZONE` /
  `TZ` (set by `run.ts`) / `spring.jackson.time-zone`. Historical columns are
  `TIMESTAMP WITHOUT TIME ZONE` — **new** timestamp columns must be
  `TIMESTAMP WITH TIME ZONE` with Java `Instant`/`OffsetDateTime` (no automated guard;
  review-enforced). See `docs/TIMEZONE_POLICY.md`.
- **Bootstrap admin**: there is no default login. First admin is created only when the
  runtime env sets both `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` and no
  active `ADMIN` exists; the server refuses to start otherwise. Remove both after first
  login and restart. Do not bypass via self-registration or DB edits.
- **Ports**: Postgres host port `5434` (container 5432), server `8089`, desktop renderer `5175`
  (Vite `strictPort` — will not pick another port).
- **Language**: product/release docs in Chinese; E2E regression specs in English
  (per `docs/INDEX.md`).
- **No comments in generated/edited code** unless explicitly requested.

## Data safety & PR evidence

Never commit or paste into PRs/screenshots: passwords, tokens, cookies, `openid`, phone
numbers, ID numbers, bank accounts, unredacted salary values, or production data. Use
test/redacted fixtures only.

PRs into `master`/`main` start from `PULL_REQUEST_TEMPLATE.md`. Any change touching
**payroll, project settlement, permissions/data-scope, or Flyway migrations** must attach
a record based on `docs/REGRESSION_EVIDENCE_RECORD_TEMPLATE.md`
(see `docs/REGRESSION_EVIDENCE_POLICY.md`). "Not tested"/"small change" is not an accepted
exception — state why the boundary is unaffected instead.

## Docs to consult before non-trivial work

- `docs/ROLE_PERMISSION_MATRIX.md` — roles (`ADMIN/BOSS/FINANCE/MANAGER/EMPLOYEE`) & scope
- `docs/TIMEZONE_POLICY.md` — timestamp conventions
- `docs/CI_*_GATE.md` — exact per-area local quality contracts
- `docs/FLYWAY_UPGRADE_GUIDE.md` — note its example command has machine-specific paths;
  use `mvn -B -ntp -f server/pom.xml test` instead.
- `docs/TECH_DEBT_GOD_CLASS.md` — `ProjectManagementServiceImpl` split plan (in progress)
