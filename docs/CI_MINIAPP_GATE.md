# Miniapp Local Quality Gate

## Scope

The active Taro client in `clients/miniapp` uses pnpm 11.22.0 for installation,
script execution, TypeScript, Vite, and both WeChat and H5 builds. Its
`pnpm-workspace.yaml` pins the hoisted node linker and the approved
build-script allowlist, so the gate does not require a system Node.js
installation.

Run from the repository root:

```bash
cd clients/miniapp
pnpm install --frozen-lockfile
pnpm ignored-builds
pnpm run check
pnpm run build:h5
```

`check` runs the TypeScript project build and the WeChat production build. The
H5 command validates the second supported Vite target. `pnpm ignored-builds` must
report no blocked lifecycle scripts.

## Manual Evidence

WeChat DevTools rendering, device permissions, real uploads, account binding,
and real network behavior remain manual smoke responsibilities. Every
user-visible miniapp change must attach a completed record based on
[`MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md`](MINIAPP_SMOKE_EVIDENCE_TEMPLATE.md), with
the identity/session flow covered by
[`MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md`](MINIAPP_IDENTITY_CONTRACT_QA_CHECKLIST.md).

The native client in `legacy/miniapp` is retained only as a migration reference
and keeps its pnpm/Node checks until that directory is reworked.
