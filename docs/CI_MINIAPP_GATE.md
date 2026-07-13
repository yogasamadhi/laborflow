# Miniapp CI Gate

## Scope

`.workflow/PRMiniappPipeline.yml` defines the Gitee Go pull-request check for
`master`. It runs the repository script:

```bash
npm --prefix miniapp run check
```

The script checks every first-party `.js` file under `miniapp/` with
`node --check` and skips `node_modules`. It validates JavaScript syntax only;
WeChat runtime, Vant component rendering, device permissions, uploads, and
network behavior remain manual smoke responsibilities.

## Enablement

Import the pipeline in Gitee Go, run it once for a pull request targeting
`master`, then configure the successful `PR Miniapp Syntax` status as required
for the protected branch. A failed syntax check must block merge after that
status is required.

## Manual Evidence

Every pull request that changes a user-visible miniapp flow must attach a
completed record based on
[`docs/MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md).
The later detailed flow checklist is maintained separately from this CI gate.
