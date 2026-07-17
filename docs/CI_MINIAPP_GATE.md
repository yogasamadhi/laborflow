# Miniapp CI Gate

## Scope

`.workflow/PRMiniappPipeline.yml` defines the Gitee Go pull-request check for
`master`. It runs the repository script:

```bash
npm --prefix miniapp run check
```

The script checks every first-party `.js` file under `miniapp/` with
`node --check`, then runs ESLint (`miniapp/.eslintrc.cjs`, `--max-warnings 0`)
covering real-bug rules (`no-undef`, `no-debugger`, unused vars) while
declaring the WeChat runtime globals (`wx`, `Page`, `App`, …), then runs the
mocked identity and service-contract regression tests. Local formatting is
owned by Prettier (`miniapp/.prettierrc`, `npm --prefix miniapp run format`)
and is not enforced in CI yet. WeChat runtime, Vant component rendering,
device permissions, real uploads, and real network behavior remain manual
smoke responsibilities.

## Enablement

Import the pipeline in Gitee Go, run it once for a pull request targeting
`master`, then configure the successful `PR Miniapp Syntax` status as required
for the protected branch. A failed syntax check must block merge after that
status is required.

## Manual Evidence

Every pull request that changes a user-visible miniapp flow must attach a
completed record based on
[`docs/MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md).
The identity/session flow checklist is maintained in
[`docs/MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md`](MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md).
